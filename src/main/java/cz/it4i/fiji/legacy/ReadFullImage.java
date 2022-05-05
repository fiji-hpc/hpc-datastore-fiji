package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static cz.it4i.fiji.legacy.common.ImagePlusTransferrer.createResStr;
import cz.it4i.fiji.datastore.service.DataStoreService;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;
import cz.it4i.fiji.rest.util.DatasetInfo;
import org.scijava.Context;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import java.io.IOException;


@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Read full image")
public class ReadFullImage implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:", persistKey = "datasetdatasetid")
	public String datasetID;

	@Parameter(label="time point:", min="0",
			description="In units of the respective dataset.",
			persistKey="datasettimepoint")
	public int timepoint = 0;

	@Parameter(label="channel:", min="0",
			description="In units of the respective dataset.",
			persistKey="datasetchannel")
	public int channel = 0;

	@Parameter(label="angle:", min="0",
			description="In units of the respective dataset.",
			persistKey="datasetangle")
	public int angle = 0;

	@Parameter(label = "Selected down-resolution:",
			persistKey="datasetreslevel")
	public String resolutionLevelsAsStr = "[1, 1, 1]";

	@Parameter(label = "Selected version:",
			description = "provide number, or keyword: latest, mixedLatest",
			persistKey="datasetversion")
	public String versionAsStr = "latest";

	@Parameter(label = "Server alive timeout [miliseconds]:", min = "1", stepSize = "1000",
			description = "How long inactivity period has to pass before the connection gets closed...",
			persistKey="datasettimeout",
			required = false)
	public int timeout = 30000;

	@Parameter(label = "Verbose reporting:", required = false,
			description = "The change takes effect always during transfers and for future use of this dialog, not for the current use.",
			persistKey="datasetverboselog")
	public boolean verboseLog = false;

	@Parameter(type = ItemIO.OUTPUT)
	public Dataset outDatasetImg;

	@Parameter
	public LogService log;

	@Override
	public void run() {
		try {
			outDatasetImg = new LocalReader(log.getContext()).readNow(URL,datasetID,
					timepoint,channel,angle,
					resolutionLevelsAsStr,versionAsStr,
					timeout,verboseLog);
		} catch (IOException e) {
			log.error("Problem reading full image: "+e.getMessage());
			//e.printStackTrace();
		}
	}


	public static
	Dataset from(final String url, final String datasetID,
	             final int timepoint, final int channel, final int angle,
	             final int downscaleX, final int downscaleY, final int downscaleZ,
	             final String versionAsStr)
	throws IOException {
		return from(url,datasetID,timepoint,channel,angle,
				createResStr(downscaleX,downscaleY,downscaleZ),
				versionAsStr,120000,false);
	}

	public static
	Dataset from(final String url, final String datasetID,
	             final int timepoint, final int channel, final int angle,
	             final String resolutionLevelsAsStr, final String versionAsStr)
	throws IOException {
		return from(url,datasetID,timepoint,channel,angle,
				resolutionLevelsAsStr,
				versionAsStr,120000,false);
	}

	public static
	Dataset from(final String url, final String datasetID,
	             final int timepoint, final int channel, final int angle,
	             final String resolutionLevelsAsStr, final String versionAsStr,
	             final int serverTimeout, final boolean verboseLog)
	throws IOException {
		return new LocalReader().readNow(url, datasetID, timepoint, channel, angle,
				resolutionLevelsAsStr, versionAsStr, serverTimeout, verboseLog);
	}


	static class LocalReader extends ImagePlusTransferrer {
		/** intended for use in solo (without a valid scijava context) application */
		LocalReader() {
			final Context ctx = new Context(LogService.class,DataStoreService.class);
			mainLogger = ctx.getService(LogService.class);
		}

		/** PREFERRED whenever appropriate context is available */
		LocalReader(final Context useThisCtx) {
			this.setContext(useThisCtx);
		}

		Dataset readNow(final String url, final String datasetID,
		                final int timepoint, final int channel, final int angle,
		                final String resolutionLevelsAsStr, final String versionAsStr,
		                final int serverTimeout, final boolean verboseLog)
		throws IOException {
			this.URL = url;
			this.datasetID = datasetID;
			this.timepoint = timepoint;
			this.channel = channel;
			this.angle = angle;
			this.resolutionLevelsAsStr = resolutionLevelsAsStr;
			this.versionAsStr = versionAsStr;
			this.timeout = serverTimeout;
			this.verboseLog = verboseLog;
			this.accessRegime = "read";
			this.minX=0;
			this.maxX=Integer.MAX_VALUE;
			this.minY=0;
			this.maxY=Integer.MAX_VALUE;
			this.minZ=0;
			this.maxZ=Integer.MAX_VALUE;
			this.dataStoreService = getContext().getService(DataStoreService.class);
			if (this.dataStoreService == null)
				throw new RuntimeException("Missing DataStoreService (is null) when reading full image.");

			myLogger = mainLogger.subLogger("HPC LegacyImage Read", verboseLog ? LogLevel.INFO : LogLevel.ERROR);
			myLogger.info("entered init with this state: "+reportCurrentSettings());

			myLogger.info("Reading "+datasetID+" from "+URL);
			di = DatasetInfo.createFrom(URL, datasetID);
			myLogger.info(di.toString());

			matchResLevel();
			rangeSpatialX();
			rangeSpatialY();
			rangeSpatialZ();

			return this.readWithAType();
		}
	}
}
