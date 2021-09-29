package cz.it4i.fiji.legacy;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Create>Create new dataset from JSON")
public class CreateNewDatasetFromJSON implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String url = "someHostname:9080";

	@Parameter(label = "Specification in JSON:", persist = false)
	public String json;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Parameter(label = "Report corresponding macro command:", required = false)
	public boolean showRunCmd = false;

	@Parameter(type = ItemIO.OUTPUT)
	public String newDatasetUUID;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC CreateDataset", LogLevel.INFO);

		try {
			final HttpURLConnection connection = (HttpURLConnection)new URL("http://"+this.url+"/datasets").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","application/json");
			connection.setDoOutput(true);
			connection.connect();
			connection.getOutputStream().write(json.getBytes());

			newDatasetUUID = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();

			if (showRunCmd) {
				myLogger.info("run(\"Create new dataset from JSON\", 'url="+this.url
						+" json="+this.json+" showruncmd=False');");
			}
		} catch (MalformedURLException e) {
			myLogger.error("Malformed URL, probably because of \"http://\" prefix. Please use only hostname:port without any spaces.");
			myLogger.error(e.getMessage());
		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}