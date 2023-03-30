package cz.it4i.fiji.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cz.it4i.fiji.legacy.util.GuiResolutionLevelParams;
import cz.it4i.fiji.legacy.util.Imglib2Types;
import cz.it4i.fiji.rest.util.DatasetInfo;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Create>Create new dataset")
public abstract class CreateNewDataset implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label="Dataset label:")
	public String label = "provide some nickname of this dataset";

	@Parameter(label = "Voxel type:", choices = {"uint8", "uint16", "uint32", "uint64","int8", "int16", "int32", "int64", "float32", "float64"})
	public String voxelType;

	@Parameter(label = "Full-resolution image size x:", min = "1")
	public int fullResSizeX;
	@Parameter(label = "Full-resolution image size y:", min = "1")
	public int fullResSizeY;
	@Parameter(label = "Full-resolution image size z:", min = "1")
	public int fullResSizeZ;

	@Parameter(label = "Total number of time points:", min = "1")
	public int timepoints = 1;
	@Parameter(label = "Total number of channels:", min = "1")
	public int channels = 1;
	@Parameter(label = "Total number of view angles:", min = "1")
	public int angles = 1;

	@Parameter(label = "Physical dimension of one voxel in x:", min = "0")
	public double res_x = 1.0;
	@Parameter(label = "Physical dimension of one voxel in y:", min = "0")
	public double res_y = 1.0;
	@Parameter(label = "Physical dimension of one voxel in z:", min = "0")
	public double res_z = 1.0;
	@Parameter(label = "Physical unit of the voxel dimension:", description = "microns")
	public String res_unit = "microns";

	@Parameter(label = "Time period between successive time points:", min = "0")
	public double time_res = 1.0;
	@Parameter(label = "Unit of the time period:", description = "seconds")
	public String time_unit = "seconds";

	@Parameter(label = "Resolution among the channels:")
	public double channel_res = 1.0;
	@Parameter(label = "Unit of channels' resolution:", description = "channel")
	public String channel_unit = "channel";

	@Parameter(label = "Resolution among the angles:")
	public double angle_res = 1.0;
	@Parameter(label = "Unit of angles' resolution:", description = "deg")
	public String angle_unit = "deg";

	@Parameter(label = "Number of resolution levels:", min = "1",
			description = "There will always be the full-resolution level [1,1,1] plus additional lower-resolution ones.")
	public int numberOfAllResLevels = 1;

	@Parameter(label = "Compression of the stored data:", choices = { "none", "gzip" })
	public String compression = "gzip";

	@Parameter
	public CommandService cs;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Parameter(label = "Report corresponding macro command:", required = false)
	public boolean showRunCmd = false;

	@Parameter(type = ItemIO.OUTPUT, label="UUID of the created dataset:")
	public String newDatasetUUID;
	@Parameter(type = ItemIO.OUTPUT, label="Label of the created dataset:")
	public String newDatasetLabel;

	protected  String getDatasetType()
	{
		return "N5";
	}

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC CreateDataset", LogLevel.INFO);

		DatasetInfo di = new DatasetInfo();
		di.voxelType = this.voxelType;
		di.dimensions = Arrays.asList(fullResSizeX,fullResSizeY,fullResSizeZ);
		di.timepoints = this.timepoints;
		di.channels = this.channels;
		di.angles = this.angles;

		di.voxelUnit = res_unit;
		di.voxelResolution = Arrays.asList(res_x,res_y,res_z);

		di.timepointResolution = new DatasetInfo.ResolutionWithOwnUnit(time_res, time_unit);
		di.channelResolution = new DatasetInfo.ResolutionWithOwnUnit(channel_res, channel_unit);
		di.angleResolution = new DatasetInfo.ResolutionWithOwnUnit(angle_res, angle_unit);

		di.compression = this.compression.equals("none") ? "raw" : this.compression;

		di.versions = Collections.emptyList();
		di.resolutionLevels = new ArrayList<>(numberOfAllResLevels);
		di.label = label;

		final CommandInfo rldlg_ci = new CommandInfo(GuiResolutionLevelParams.class);
		final Map<String,Object> rldlg_presets = new HashMap<>(5);
		rldlg_presets.put("pxSizeInBytes", ((Imglib2Types.TypeHandler<?>)Imglib2Types
				.getTypeHandler(voxelType)).nativeAndRealType.getBitsPerPixel()/8);

		try {
			for (int levelCnt = 1; levelCnt <= numberOfAllResLevels; ++levelCnt) {
				rldlg_presets.put("resLevelNumber", levelCnt);
				if (levelCnt == 1) {
					rldlg_presets.put("down_x", 1);
					rldlg_presets.put("down_y", 1);
					rldlg_presets.put("down_z", 1);
				}

				final Future<CommandModule> rldlg_result = cs.run(rldlg_ci, true, rldlg_presets);
				final GuiResolutionLevelParams rldlg_obj = (GuiResolutionLevelParams)rldlg_result.get().getCommand();
				if (!rldlg_obj.userClickedOK) return; //stops the whole thing!

				di.resolutionLevels.add(new DatasetInfo.ResolutionLevel(
						rldlg_obj.down_x,rldlg_obj.down_y,rldlg_obj.down_z,
						rldlg_obj.block_x,rldlg_obj.block_y,rldlg_obj.block_z ));

				if (levelCnt == 1) {
					rldlg_presets.remove("down_x");
					rldlg_presets.remove("down_y");
					rldlg_presets.remove("down_z");
				}
			}

			//finish it up with the resolutions...
			for (DatasetInfo.ResolutionLevel l : di.resolutionLevels)
				l.setDimensions(di.dimensions);

			myLogger.info("============ collected this params: ===============");
			myLogger.info(di);

			final String json = new ObjectMapper().writeValueAsString(di);
			myLogger.info("CREATED JSON:\n"+json);

			final Future<CommandModule> subcall = cs.run(CreateNewDatasetFromJSON.class, true,
					"url",url, "json",json, "showRunCmd",showRunCmd,"datasetType",getDatasetType());
			newDatasetUUID = (String)subcall.get().getOutput("newDatasetUUID");
			newDatasetLabel = di.getLabel();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			//dialog interrupted, do nothing special...
		} catch (JsonProcessingException e) {
			myLogger.error("Error converting dataset description into JSON:"+e.getMessage());
		}
	}


}