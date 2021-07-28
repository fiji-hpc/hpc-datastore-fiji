package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.prefs.PrefService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.rest.util.DatasetInfo;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, headless = true)
abstract class ImagePlusDialogHandler extends DynamicCommand {
	// ========= internal parameters that needs to be supplied =========
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:")
	public String datasetID;

	@Parameter(label = "Access regime:")
	public String accessRegime;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Parameter
	public PrefService prefService;

	// ========= user-visible parameters to define subset of the original dataset =========
	@Parameter(label="min X [px]:", min="0", callback = "rangeSpatialX")
	public int minX = 0;
	@Parameter(label="max X [px]:", min="0", callback = "rangeSpatialX")
	public int maxX;

	@Parameter(label="min Y [px]:", min="0", callback = "rangeSpatialY")
	public int minY = 0;
	@Parameter(label="max Y [px]:", min="0", callback = "rangeSpatialY")
	public int maxY;

	@Parameter(label="min Z [px]:", min="0", callback = "rangeSpatialZ")
	public int minZ = 0;
	@Parameter(label="max Z [px]:", min="0", callback = "rangeSpatialZ")
	public int maxZ;

	@Parameter(label="time point:", min="0", callback = "rangeTPs",
			description="In units of the respective dataset.")
	public int timepoints = 0;

	@Parameter(label="channel:", min="0", callback = "rangeChannels",
			description="In units of the respective dataset.")
	public int channels = 0;

	@Parameter(label="angle:", min="0", callback = "rangeAngles",
			description="In units of the respective dataset.")
	public int angles = 0;

	@Parameter(label = "Available down-resolutions:", choices = {""},
			initializer = "readInfo", callback = "updateSpatialRanges")
	public String resolutionLevelsAsStr;
	protected DatasetInfo.ResolutionLevel currentResLevel; //caches the current level info

	protected void updateSpatialRanges() {
		//backup bounds when at full-res
		int nX = minX * currentResLevel.resolutions.get(0);
		int xX = maxX * currentResLevel.resolutions.get(0);
		int nY = minY * currentResLevel.resolutions.get(1);
		int xY = maxY * currentResLevel.resolutions.get(1);
		int nZ = minZ * currentResLevel.resolutions.get(2);
		int xZ = maxZ * currentResLevel.resolutions.get(2);

		//update to the new res level
		matchResLevel();
		if (currentResLevel == null) return;

		//udpate the bounds to the current res level
		minX = nX / currentResLevel.resolutions.get(0);
		maxX = xX / currentResLevel.resolutions.get(0);
		minY = nY / currentResLevel.resolutions.get(1);
		maxY = xY / currentResLevel.resolutions.get(1);
		minZ = nZ / currentResLevel.resolutions.get(2);
		maxZ = xZ / currentResLevel.resolutions.get(2);

		//align the bounds
		rangeSpatialX();
		rangeSpatialY();
		rangeSpatialZ();
	}

	/** updates currentResLevel given the current choice in resolutionLevelsAsStr */
	protected void matchResLevel() {
		myLogger.info("going to match against: "+resolutionLevelsAsStr);
		int i = 0;
		while (i < di.resolutionLevels.size()) {
			DatasetInfo.ResolutionLevel l = di.resolutionLevels.get(i);
			if (l.resolutions.toString().equals(resolutionLevelsAsStr)) {
				currentResLevel = l;
				myLogger.info("matched res level to i="+i);
				return;
			}
			++i;
		}
		myLogger.error("no matching res level!");
		currentResLevel = null;
	}

	@Parameter(label = "Available versions:", choices = {""})
	public String versionAsStr = "0";

	@Parameter(label = "Server alive timeout [miliseconds]:", min = "-1", stepSize = "1000",
			description = "Value of -1 sets timeout to infinity.")
	public int timeout = 30000;

	protected DatasetInfo di;

	/** this acts as a c'tor: it is called when the dialog is initialized to retrieve dataset
	    parameters to know how to populate/setup this dialog's choice boxes and bounds...
	    which is why it requires 'URL' and 'datasetID' to be for sure set in advance */
	protected void readInfo() {
		try {
			//logging facility
			myLogger = mainLogger.subLogger("HPC LegacyImage", LogLevel.INFO);

			//sanity check
			if (URL == null || datasetID == null) {
				myLogger.warn("Intended to be called from macros or scripts, in which case");
				myLogger.warn("the parameters 'URL' and 'datasetID' must be supplied.");
				throw new IllegalArgumentException("URL or datasetID was not provided.");
			}

			//retrieve data
			myLogger.info("Reading "+datasetID+" from "+URL);
			di = DatasetInfo.createFrom(URL, datasetID);
			myLogger.info(di.toString());

			//populate some variables
			getInfo().getMutableInput("resolutionLevelsAsStr",String.class).setChoices(
					di.resolutionLevels.stream().map(v -> v.resolutions.toString()).collect(Collectors.toList()) );
			resolutionLevelsAsStr = prefService.get(this.getClass(),"resolutionLevelsAsStr");
			if (resolutionLevelsAsStr == null) {
				//no previous value is stored -> need to set some (to enable matchResLevel() to function)
				resolutionLevelsAsStr = getInfo()
						.getMutableInput("resolutionLevelsAsStr",String.class)
						.getChoices().get(0);
			}
			matchResLevel();

			final List<String> versions = di.versions.stream().map(Object::toString).collect(Collectors.toList());
			if (accessRegime.equals("write")) versions.add(0,"new");
			if (accessRegime.equals("read")) {
				versions.add("latest");
				versions.add("mixedLatest");
			}
			getInfo().getMutableInput("versionAsStr",String.class).setChoices(versions);

			maxX = currentResLevel.dimensions[0]; //rangeSpatial will adjust potentially
			maxY = currentResLevel.dimensions[1];
			maxZ = currentResLevel.dimensions[2];
			rangeSpatialX();
			rangeSpatialY();
			rangeSpatialZ();
		} catch (Exception e) {
			myLogger.error("Problem accessing the dataset: "+e.getMessage());
			this.cancel("Problem accessing the dataset: "+e.getMessage());
		}
	}


	// ========= spatial bounds checking and adjusting =========
	private final int[] spatialDimBackup = {minX,maxX,minY,maxY,minZ,maxZ};

	private void rangeSpatial(int cn,int cx, int nI,int xI, int blockSize, int maxx) {
		//shortcuts, also note cX = currentX
		int min = spatialDimBackup[nI];
		int max = spatialDimBackup[xI];

		//incremental steps for miN?
		if (cn == min-1) min -= blockSize;
		else if (cn == min+1) min += blockSize;
		else min = cn;

		//incremental steps for maX?
		if (cx == max-1) max -= blockSize;
		else if (cx == max+1) max += blockSize;
		else max = cx;

		//upper bound: align to block size
		if (max < 1) max = 1;
		max = (int)Math.ceil((double)max / (double)blockSize) * blockSize -1;
		if (max > maxx) max = maxx-1;

		//lower bound: align to block size
		if (min < 0) min = 0;
		if (min > max) min = max;
		min = Math.floorDiv(min, blockSize) * blockSize;

		spatialDimBackup[nI] = min;
		spatialDimBackup[xI] = max;
	}

	protected void rangeSpatialX() {
		if (currentResLevel == null) return;

		rangeSpatial(minX,maxX,0,1,
				currentResLevel.blockDimensions.get(0), currentResLevel.dimensions[0]);
		minX = spatialDimBackup[0];
		maxX = spatialDimBackup[1];
	}

	protected void rangeSpatialY() {
		if (currentResLevel == null) return;

		rangeSpatial(minY,maxY,2,3,
				currentResLevel.blockDimensions.get(1), currentResLevel.dimensions[1]);
		minY = spatialDimBackup[2];
		maxY = spatialDimBackup[3];
	}

	protected void rangeSpatialZ() {
		if (currentResLevel == null) return;
		rangeSpatial(minZ,maxZ,4,5,
				currentResLevel.blockDimensions.get(2), currentResLevel.dimensions[2]);
		minZ = spatialDimBackup[4];
		maxZ = spatialDimBackup[5];
	}


	// ========= other bounds checking and adjusting =========
	protected void rangeTPs() {
		timepoints = Math.max(0, Math.min(timepoints, di.timepoints-1));
	}
	protected void rangeChannels() {
		channels = Math.max(0, Math.min(channels, di.channels-1));
	}
	protected void rangeAngles() {
		angles = Math.max(0, Math.min(angles, di.angles-1));
	}

	protected void checkAccessRegimeVsDatasetVersionOrThrow() {
		if (versionAsStr.equals("new") && accessRegime.startsWith("read")) {
			myLogger.warn("Cannot _create and write new_ version when intending to _read_ from a dataset.");
			throw new IllegalArgumentException("Wrong combination, cannot read new version of a dataset.");
		}
		if (versionAsStr.contains("atest") && accessRegime.contains("write")) {
			myLogger.warn("Cannot _read "+versionAsStr+"_ version when intending to _write_ into a dataset.");
			throw new IllegalArgumentException("Wrong combination, cannot read new version of a dataset.");
		}
	}
}
