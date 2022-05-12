package cz.it4i.fiji.legacy.common;

import org.scijava.ItemVisibility;
import org.scijava.command.DynamicCommand;
import org.scijava.prefs.PrefService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import cz.it4i.fiji.rest.util.DatasetInfo;
import cz.it4i.fiji.legacy.ReadIntoImagePlus;
import cz.it4i.fiji.legacy.WriteFromImagePlus;

abstract class ImagePlusDialogHandler extends DynamicCommand {
	// ========= internal parameters that must be set when using this command =========
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:", persistKey = "datasetdatasetid")
	public String datasetID;

	@Parameter(label = "Access regime:")
	public String accessRegime;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Parameter
	public PrefService prefService;

	// ========= user-visible parameters to define subset of the original dataset =========
	@Parameter(label="Params of UUID:", visibility = ItemVisibility.MESSAGE,
			required = false, persist = false)
	public String datasetUUID = "";
	@Parameter(label="Dataset label:", visibility = ItemVisibility.MESSAGE,
			required = false, persist = false)
	public String datasetLabel = "";

	@Parameter(label="min X [px]:", min="0", callback = "rangeSpatialX")
	public int minX = -1;
	@Parameter(label="max X [px]:", min="0", callback = "rangeSpatialX")
	public int maxX = -1;

	@Parameter(label="min Y [px]:", min="0", callback = "rangeSpatialY")
	public int minY = -1;
	@Parameter(label="max Y [px]:", min="0", callback = "rangeSpatialY")
	public int maxY = -1;

	@Parameter(label="min Z [px]:", min="0", callback = "rangeSpatialZ")
	public int minZ = -1;
	@Parameter(label="max Z [px]:", min="0", callback = "rangeSpatialZ")
	public int maxZ = -1;

	@Parameter(label="time point:", min="0", callback = "rangeTPs",
			description="In units of the respective dataset.",
			persistKey="datasettimepoint")
	public int timepoint = 0;

	@Parameter(label="channel:", min="0", callback = "rangeChannels",
			description="In units of the respective dataset.",
			persistKey="datasetchannel")
	public int channel = 0;

	@Parameter(label="angle:", min="0", callback = "rangeAngles",
			description="In units of the respective dataset.",
			persistKey="datasetangle")
	public int angle = 0;

	@Parameter(label = "Available down-resolutions:", choices = {""},
			initializer = "readInfo", callback = "updateSpatialRanges",
			persistKey="datasetreslevel")
	public String resolutionLevelsAsStr = null;
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

	/** updates currentResLevel given the current choice in resolutionLevelsAsStr,
	    sets currentResLevel to null when resolutionLevelsAsStr is not valid */
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

	@Parameter(label = "Available versions:", choices = {""},
			persistKey="datasetversion")
	public String versionAsStr = null;

	@Parameter(label = "Server alive timeout [miliseconds]:", min = "1", stepSize = "1000",
			description = "How long inactivity period has to pass before the connection gets closed...",
			persistKey="datasettimeout",
			required = false)
	public int timeout = 30000;

	@Parameter(label = "Verbose reporting:", required = false,
			description = "The change takes effect always during transfers and for future use of this dialog, not for the current use.",
			persistKey="datasetverboselog")
	public boolean verboseLog = false;

	@Parameter(label = "Report corresponding macro command:", required = false)
	public boolean showRunCmd = false;

	protected DatasetInfo di;

	/** this acts as a c'tor: it is called when the dialog is initialized to retrieve dataset
	    parameters to know how to populate/setup this dialog's choice boxes and bounds...
	    which is why it requires 'URL' and 'datasetID' to be for sure set in advance */
	protected void readInfo() {
		try {
			//logging flags
			if (minX == -1) {
				//if no CLI at all, use prefs, or class default (which was the verboseLog holds now)
				verboseLog = prefService.getBoolean(this.getClass(), "verboseLog", verboseLog);
				showRunCmd = prefService.getBoolean(this.getClass(), "showRunCmd", showRunCmd);
			}

			//logging facility
			myLogger = mainLogger.subLogger("HPC LegacyImage", verboseLog ? LogLevel.INFO : LogLevel.ERROR);
			myLogger.info("entered init with this state: "+reportCurrentSettings());

			//sanity check
			if (accessRegime == null) {
				//if we are executed from a script, in which case users are not requested
				//to provide the 'accessregime', we have to set it now for them...
				if (this.getClass().isAssignableFrom(ReadIntoImagePlus.class)) {
					accessRegime="read";
					resolveInput("accessRegime");
				}
				if (this.getClass().isAssignableFrom(WriteFromImagePlus.class)) {
					accessRegime="write";
					resolveInput("accessRegime");
				}
			}
			if (URL == null || datasetID == null || accessRegime == null) {
				myLogger.warn("Intended to be called from macros or scripts, in which case");
				myLogger.warn("the parameters 'URL', 'datasetID' and 'accessRegime' must be supplied.");
				throw new IllegalArgumentException("URL, datasetID or accessRegime was not provided.");
			}

			//retrieve data
			myLogger.info("Reading "+datasetID+" from "+URL);
			di = DatasetInfo.createFrom(URL, datasetID);
			myLogger.info(di.toString());

			//set choices lists:
			getInfo().getMutableInput("resolutionLevelsAsStr",String.class).setChoices(
					di.resolutionLevels.stream().map(v -> v.resolutions.toString()).collect(Collectors.toList()) );

			final List<String> versions = di.versions.stream().map(Object::toString).collect(Collectors.toList());
			if (accessRegime.equals("write")) {
				versions.add(0,"new");
				versions.add("latest");
			}
			if (accessRegime.equals("read")) {
				versions.add("latest");
				versions.add("mixedLatest");
			}
			getInfo().getMutableInput("versionAsStr",String.class).setChoices(versions);

			//we want to leave this init() method with all params set to sensible values,
			//therefore, we read their given/last/default values first and adjust them later

			//nevertheless, default values of some parameters depend already on the
			//current value of res. level -> we have to fix&set res. level prominently
			if (resolutionLevelsAsStr == null) {
				//class default value -> no CLI -> try prefs
				resolutionLevelsAsStr = prefService.get(this.getClass(),"resolutionLevelsAsStr");
			}
			//
			//got some res. level anyhow? and is it a valid one?
			//NB: matchResLevel() indicates failure also when res. level is null
			matchResLevel();
			if (resolutionLevelsAsStr == null || currentResLevel == null) {
				//no previous valid value -> but need to set some...
				resolutionLevelsAsStr = getInfo()
						.getMutableInput("resolutionLevelsAsStr",String.class)
						.getChoices().get(0);
				matchResLevel();
				//NB: should work well now...
			}

			//only now we can retrieve values to the remaining params (and fix them later)
			setParamsToCliOrPrefsOrDefaultValues();
			myLogger.info("updated init with this state: "+reportCurrentSettings());

			//fix the remaining params
			rangeSpatialX();
			rangeSpatialY();
			rangeSpatialZ();
			rangeTPs();
			rangeChannels();
			rangeAngles();
			datasetLabel = di.getLabel();
			datasetUUID = di.uuid;

			//make also sure versionAsStr holds already some intelligent input, there's
			//no callback nor validator used as it seems for it impossible to hold value
			//not available in the choices list... except now when it may possibly
			//take (some wrong) value from CLI command or from the Fiji internal prefs
			if (!isVersionAmongChoices()) {
				versionAsStr = accessRegime.startsWith("read") ? "latest" : "new";
			}
			if (!isVersionAmongChoices())
				throw new IllegalStateException("Failed finding symbolic version: "+versionAsStr);

			//now all inputs should be okay w.r.t. to the current dataset and res. level,
			//however, if this command is not called from CLI then the input will be
			//overwritten from the prefs store (or their class defaults will be used if
			//the prefs store is empty), so we save the input into the prefs store...
			saveInputs();

			myLogger.info("leaving init with this state: "+reportCurrentSettings());
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
		timepoint = Math.max(0, Math.min(timepoint, di.timepoints-1));
	}
	protected void rangeChannels() {
		channel = Math.max(0, Math.min(channel, di.channels-1));
	}
	protected void rangeAngles() {
		angle = Math.max(0, Math.min(angle, di.angles-1));
	}

	// ========= remaining params checking and adjusting =========
	protected void checkAccessRegimeVsDatasetVersionOrThrow() {
		if (versionAsStr.equals("new") && accessRegime.startsWith("read")) {
			myLogger.warn("Cannot _create and write new_ version when intending to _read_ from a dataset.");
			throw new IllegalArgumentException("Wrong combination, cannot read new version of a dataset.");
		}
		if (versionAsStr.contains("mixed") && accessRegime.contains("write")) {
			myLogger.warn("Cannot _read "+versionAsStr+"_ version when intending to _write_ into a dataset.");
			throw new IllegalArgumentException("Wrong combination, cannot read new version of a dataset.");
		}
	}

	protected boolean isVersionAmongChoices() {
		if (versionAsStr == null) return false;
		for (String v : getInfo().getMutableInput("versionAsStr",String.class).getChoices())
			if (versionAsStr.equals(v)) return true;
		return false;
	}

	protected void setParamsToCliOrPrefsOrDefaultValues() {
		//we set here all user "external" plugin parameters except
		//for URL,datasetID,accessRegime that were already provided and
		//for timeout that is a general param and scijava's care is good enough for it

		//NB: if an attribute still holds its class default value, which is
		//    given to it at this object construction, it means no CLI value
		//    for it has been provided yet, we then try to fetch one from
		//    the Fiji internal prefs service or use some sensible default
		//    if nothing is found in the prefs store
		String prefVal;
		try {
			if (minX == -1) { //-1 is the class default value for this attrib
				prefVal = prefService.get(this.getClass(),"minX");
				minX = prefVal != null ? Integer.parseInt(prefVal) : 0;
			}
			if (minY == -1) {
				prefVal = prefService.get(this.getClass(),"minY");
				minY = prefVal != null ? Integer.parseInt(prefVal) : 0;
			}
			if (minZ == -1) {
				prefVal = prefService.get(this.getClass(),"minZ");
				minZ = prefVal != null ? Integer.parseInt(prefVal) : 0;
			}
			if (maxX == -1) {
				prefVal = prefService.get(this.getClass(),"maxX");
				maxX = prefVal != null ? Integer.parseInt(prefVal) : currentResLevel.dimensions[0];
				//NB: rangeSpatial will adjust potentially
			}
			if (maxY == -1) {
				prefVal = prefService.get(this.getClass(),"maxY");
				maxY = prefVal != null ? Integer.parseInt(prefVal) : currentResLevel.dimensions[1];
			}
			if (maxZ == -1) {
				prefVal = prefService.get(this.getClass(),"maxZ");
				maxZ = prefVal != null ? Integer.parseInt(prefVal) : currentResLevel.dimensions[2];
			}

			if (timepoint == -1) {
				prefVal = prefService.get(this.getClass(),"timepoint");
				timepoint = prefVal != null ? Integer.parseInt(prefVal) : 0;
			}
			if (channel == -1) {
				prefVal = prefService.get(this.getClass(),"channel");
				channel = prefVal != null ? Integer.parseInt(prefVal) : 0;
			}
			if (angle == -1) {
				prefVal = prefService.get(this.getClass(),"angle");
				angle = prefVal != null ? Integer.parseInt(prefVal) : 0;
			}

			if (versionAsStr == null) {
				versionAsStr = prefService.get(this.getClass(),"versionAsStr");
				//NB: null default value forces the readInfo() to choose some existing version
			}
			if (timeout == -99) {
				prefVal = prefService.get(this.getClass(),"timeout");
				timeout = prefVal != null ? Integer.parseInt(prefVal) : 30000;
			}
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Fiji preferences contain non-integer value for either minX,minY,minZ,maxX,maxY or maxZ",e);
		}
	}


	// ========= command reporting =========
	public String reportCurrentSettings() {
		return  "url="+URL
				+" datasetid="+datasetID
				+" accessregime="+accessRegime
				+" versionasstr="+versionAsStr
				+" resolutionlevelsasstr=["+resolutionLevelsAsStr+"]"
				+" minx="+minX
				+" maxx="+maxX
				+" miny="+minY
				+" maxy="+maxY
				+" minz="+minZ
				+" maxz="+maxZ
				+" timepoint="+timepoint
				+" channel="+channel
				+" angle="+angle
				+" timeout="+timeout;
	}

	public String reportAsMacroCommand(final String forThisCommand) {
		return "run(\""+forThisCommand+"\", \""
				+ "url="+URL
				+" datasetid="+datasetID
				+" versionasstr="+versionAsStr
				+" resolutionlevelsasstr=["+resolutionLevelsAsStr+"]"
				//+" minx="+minX
				//+" maxx="+maxX
				//+" miny="+minY
				//+" maxy="+maxY
				//+" minz="+minZ
				//+" maxz="+maxZ
				+" timepoint="+timepoint
				+" channel="+channel
				+" angle="+angle
				+" timeout="+timeout
				+" verboselog="+verboseLog+"\");";
				//+" showruncmd="+showRunCmd+"\");";
	}

	protected void adjustReportingVerbosity() {
		myLogger = mainLogger.subLogger("HPC LegacyImage", verboseLog ? LogLevel.INFO : LogLevel.ERROR);
	}
}
