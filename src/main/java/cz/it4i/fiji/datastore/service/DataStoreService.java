package cz.it4i.fiji.datastore.service;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Plugin(type = Service.class)
public class DataStoreService extends AbstractService implements SciJavaService
{
	/**
	 * if the estimated connection ends within this period from the
	 * current moment, the system will rather request opening a new
	 * connection than pointing the user onto the existing one (which
	 * may not be there by the time user actually conducts the request)
	 */
	public int uncertaintyWindowMiliSeconds = 5000;

	private static final int PREFERRED_MAX_STORED_SERVICES = 50;
	private final Map<DataStoreRequest,DataStoreConnection> knownServices = new HashMap<>(PREFERRED_MAX_STORED_SERVICES);

	public String getActiveServingUrl(final DataStoreRequest request)
	throws IOException
	{
		DataStoreConnection connection = knownServices.getOrDefault(request, null);

		//a "dying" connection?  (an existing connection that is about to timeout soon)
		if (connection != null && connection.willServerCloseAfter(uncertaintyWindowMiliSeconds))
		{
			knownServices.remove(request);
			connection = null;
		}

		//shall we open a new connection?
		if (connection == null) {
			connection = new DataStoreConnection(
					requestService( request.createRequestURL() ), request.getTimeout() );
			knownServices.put(request, connection);

			//try to clean up...
			if (knownServices.size() > PREFERRED_MAX_STORED_SERVICES)
				proneInactiveServices();
		}

		//hypothetically "reset" the timeout of the service
		connection.serverIsUsedNow();

		return connection.datasetServerURL;
	}

	public void proneInactiveServices()
	{
		final long criticalTime = System.currentTimeMillis() + uncertaintyWindowMiliSeconds;
		knownServices.entrySet().removeIf(e -> e.getValue().timeWhenServerCloses() < criticalTime);
	}

	public void serverIsUsedNow(final DataStoreRequest request)
	{
		final DataStoreConnection conn = knownServices.getOrDefault(request, null);
		if (conn != null) conn.serverIsUsedNow();
	}

	final Date datePrinter = new Date();

	@Override
	public String toString()
	{
		final long now = System.currentTimeMillis();
		datePrinter.setTime(now);

		final StringBuilder sb = new StringBuilder("Known connections at ");
		sb.append(datePrinter).append(":\n");
		for (DataStoreRequest req : knownServices.keySet())
		{
			final DataStoreConnection conn = knownServices.get(req);
			sb.append("  ").append(req).append("\n  -> ").append(conn);

			final long closingTime = conn.timeWhenServerCloses() - uncertaintyWindowMiliSeconds;
			if (now > closingTime)
				sb.append(" is likely already closed by now\n");
			else
				sb.append(" will likely be opened for at least ")
						.append((closingTime-now)/1000).append(" seconds\n");
		}
		return sb.toString();
	}


	static
	public String requestService(final String requestFormattedUrl)
	throws IOException
	{
		//connect to get the new URL for the blocks-server itself
		final URLConnection connection = new URL(requestFormattedUrl).openConnection();
		connection.getInputStream(); //this enables access to the redirected URL
		return connection.getURL().toString();
	}
}
