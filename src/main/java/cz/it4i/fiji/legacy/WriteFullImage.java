package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static cz.it4i.fiji.legacy.common.ImagePlusTransferrer.createResStr;
import cz.it4i.fiji.datastore.service.DataStoreService;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;
import cz.it4i.fiji.rest.util.DatasetInfo;
import net.imglib2.img.Img;
import net.imglib2.view.Views;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import java.io.IOException;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Write full image")
public class WriteFullImage implements Command {
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

	@Parameter(label = "Write also lower resolutions:", required = false)
	public boolean uploadResPyramids = true;

	@Parameter(label = "Selected version:",
			description = "provide number, or keyword: latest, new",
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

	@Parameter
	public Dataset inDatasetImg;

	@Parameter
	public LogService log;

	@Override
	public void run() {
		try {
			new LocalWriter(log.getContext()).writeNow((Img)inDatasetImg.getImgPlus().getImg(),
					URL,datasetID,
					timepoint,channel,angle,
					resolutionLevelsAsStr,uploadResPyramids,versionAsStr,
					timeout,verboseLog);
			log.info("transfer is finished");
		} catch (IOException | IllegalArgumentException e) {
			log.error("Problem writing full image: "+e.getMessage());
		}
	}


	public static
	void to(final Img<? extends RealType<?>> image, final String url, final String datasetID,
	        final int timepoint, final int channel, final int angle,
	        final int downscaleX, final int downscaleY, final int downscaleZ,
	        final String versionAsStr)
	throws IOException,IllegalArgumentException {
		to(image, url,datasetID,timepoint,channel,angle,
				createResStr(downscaleX,downscaleY,downscaleZ),
				versionAsStr,120000,false);
	}

	public static
	void to(final Img<? extends RealType<?>> image, final String url, final String datasetID,
	        final int timepoint, final int channel, final int angle,
	        final String resolutionLevelsAsStr, final String versionAsStr)
	throws IOException,IllegalArgumentException {
		to(image, url,datasetID,timepoint,channel,angle,
				resolutionLevelsAsStr,
				versionAsStr,120000,false);
	}

	public static
	void to(final Img<? extends RealType<?>> image, final String url, final String datasetID,
	        final int timepoint, final int channel, final int angle,
	        final String resolutionLevelsAsStr, final String versionAsStr,
	        final int serverTimeout, final boolean verboseLog)
	throws IOException,IllegalArgumentException {
		new LocalWriter().writeNow((Img)image, url, datasetID, timepoint, channel, angle,
				resolutionLevelsAsStr, versionAsStr, serverTimeout, verboseLog);
	}


	public static
	void toWithoutPyramids(final Img<? extends RealType<?>> image, final String url, final String datasetID,
	        final int timepoint, final int channel, final int angle,
	        final int downscaleX, final int downscaleY, final int downscaleZ,
	        final String versionAsStr)
	throws IOException,IllegalArgumentException {
		toWithoutPyramids(image, url,datasetID,timepoint,channel,angle,
				createResStr(downscaleX,downscaleY,downscaleZ),
				versionAsStr,120000,false);
	}

	public static
	void toWithoutPyramids(final Img<? extends RealType<?>> image, final String url, final String datasetID,
	        final int timepoint, final int channel, final int angle,
	        final String resolutionLevelsAsStr, final String versionAsStr)
	throws IOException,IllegalArgumentException {
		toWithoutPyramids(image, url,datasetID,timepoint,channel,angle,
				resolutionLevelsAsStr,
				versionAsStr,120000,false);
	}

	public static
	void toWithoutPyramids(final Img<? extends RealType<?>> image, final String url, final String datasetID,
	        final int timepoint, final int channel, final int angle,
	        final String resolutionLevelsAsStr, final String versionAsStr,
	        final int serverTimeout, final boolean verboseLog)
	throws IOException,IllegalArgumentException {
		new LocalWriter().writeNowWithoutPyramids((Img)image, url, datasetID, timepoint, channel, angle,
				resolutionLevelsAsStr, versionAsStr, serverTimeout, verboseLog);
	}


	static class LocalWriter extends ImagePlusTransferrer {
		/** intended for use in solo (without a valid scijava context) application */
		LocalWriter() {
			final Context ctx = new Context(LogService.class,DataStoreService.class);
			mainLogger = ctx.getService(LogService.class);
		}

		/** PREFERRED whenever appropriate context is available */
		LocalWriter(final Context useThisCtx) {
			this.setContext(useThisCtx);
		}

		<T extends RealType<T>>
		void writeNow(final Img<T> img, final String url, final String datasetID,
		              final int timepoint, final int channel, final int angle,
		              final String resolutionLevelsAsStr, final String versionAsStr,
		              final int serverTimeout, final boolean verboseLog)
		throws IOException,IllegalArgumentException
		{
			writeNow(img,url,datasetID,timepoint,channel,angle,
					resolutionLevelsAsStr,true,versionAsStr,serverTimeout,verboseLog);
		}

		<T extends RealType<T>>
		void writeNowWithoutPyramids(final Img<T> img, final String url, final String datasetID,
		              final int timepoint, final int channel, final int angle,
		              final String resolutionLevelsAsStr, final String versionAsStr,
		              final int serverTimeout, final boolean verboseLog)
		throws IOException,IllegalArgumentException
		{
			writeNow(img,url,datasetID,timepoint,channel,angle,
					resolutionLevelsAsStr,false,versionAsStr,serverTimeout,verboseLog);
		}


		<TR extends RealType<TR>, TNR extends NativeType<TNR> & RealType<TNR>>
		void writeNow(final Img<TR> img, final String url, final String datasetID,
		              final int timepoint, final int channel, final int angle,
		              final String resolutionLevelsAsStr, final boolean uploadResPyramids,
		              final String versionAsStr,
		              final int serverTimeout, final boolean verboseLog)
		throws IOException,IllegalArgumentException
		{
			final Img<TNR> image = checkForAndExtendWithNativeType(img);

			this.URL = url;
			this.datasetID = datasetID;
			this.timepoint = timepoint;
			this.channel = channel;
			this.angle = angle;
			this.resolutionLevelsAsStr = resolutionLevelsAsStr;
			this.versionAsStr = versionAsStr;
			this.timeout = serverTimeout;
			this.verboseLog = verboseLog;
			this.accessRegime = "write";
			this.minX=0;
			this.maxX=Integer.MAX_VALUE;
			this.minY=0;
			this.maxY=Integer.MAX_VALUE;
			this.minZ=0;
			this.maxZ=Integer.MAX_VALUE;
			this.dataStoreService = getContext().getService(DataStoreService.class);
			if (this.dataStoreService == null)
				throw new RuntimeException("Missing DataStoreService (is null) when writing full image.");

			myLogger = mainLogger.subLogger("HPC LegacyImage Write", verboseLog ? LogLevel.INFO : LogLevel.ERROR);
			myLogger.info("entered init with this state: "+reportCurrentSettings());

			myLogger.info("Reading "+datasetID+" from "+URL);
			di = DatasetInfo.createFrom(URL, datasetID);
			myLogger.info(di.toString());

			matchResLevel();
			rangeSpatialX();
			rangeSpatialY();
			rangeSpatialZ();

			if (currentResLevel == null)
				throw new IOException("Cannot write to res level "+resolutionLevelsAsStr
						+" because the dataset is not having this one.");

			this.writeWithAType(image);

			if (uploadResPyramids)
			{
				//prevent from creating new version with every next resolution
				if (versionAsStr.startsWith("new")) this.versionAsStr = "latest";

				//plan: find upper res levels, for each create downscaled image and upload it
				int resLevelIdx = 0;
				while (resLevelIdx < di.resolutionLevels.size()
						&& di.resolutionLevels.get(resLevelIdx) != currentResLevel) ++resLevelIdx;
				if (resLevelIdx == di.resolutionLevels.size())
					throw new RuntimeException("Failed re-matching the resolution level, that's odd...");
				myLogger.info("Starting to upload down-scaled versions from level "+ resLevelIdx +".");

				//base downscale factors:
				final int rx = currentResLevel.resolutions.get(0);
				final int ry = currentResLevel.resolutions.get(1);
				final int rz = currentResLevel.resolutions.get(2);

				//these remaining res levels we're gonna write too
				++resLevelIdx;
				while (resLevelIdx < di.resolutionLevels.size())
				{
					currentResLevel = di.resolutionLevels.get(resLevelIdx);
					this.resolutionLevelsAsStr = currentResLevel.resolutions.toString();
					myLogger.info("==> Writing also: "+this.resolutionLevelsAsStr);

					rangeSpatialX();
					rangeSpatialY();
					rangeSpatialZ();

					//downscale factor w.r.t to the base factor
					final int dx = currentResLevel.resolutions.get(0) / rx;
					final int dy = currentResLevel.resolutions.get(1) / ry;
					final int dz = currentResLevel.resolutions.get(2) / rz;

					//check the scaling is integer, or skip this scaling
					if (dx*rx != currentResLevel.resolutions.get(0)
						|| dy*ry != currentResLevel.resolutions.get(1)
						|| dz*rz != currentResLevel.resolutions.get(2))
					{
						myLogger.info("Cannot reach res level "+resLevelIdx+" from "+ resLevelIdx +" with integer-scaling.");
					} else {
						writeWithAType( Views.subsample(image, dx,dy,dz), image.firstElement() );
					}
					++resLevelIdx;
				}
			}
		}
	}
}
