package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.log.LogLevel;
import org.scijava.log.Logger;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

import cz.it4i.fiji.rest.util.DatasetInfo;
import cz.it4i.fiji.legacy.util.GuiResolutionLevelParams;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Create new dataset")
public class CreateNewDataset implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String url = "someHostname:9080";

	@Parameter(label = "Voxel type:", choices = {"uint8", "uint16", "uint32", "uint64", "float"})
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
	public double res_x = 1;
	@Parameter(label = "Physical dimension of one voxel in y:", min = "0")
	public double res_y = 1;
	@Parameter(label = "Physical dimension of one voxel in z:", min = "0")
	public double res_z = 1;
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

	@Parameter(type = ItemIO.OUTPUT)
	public String newDatasetUUID;

	@Override
	public void run() {
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

		final CommandInfo rldlg_ci = new CommandInfo(GuiResolutionLevelParams.class);
		final Map<String,Object> rldlg_presets = new HashMap<>(4);
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
			//finishup the resolutions...
			for (DatasetInfo.ResolutionLevel l : di.resolutionLevels)
				l.setDimensions(di.dimensions);

		} catch (ExecutionException e) {
			e.printStackTrace();
			return; //stops the whole thing!
		} catch (InterruptedException e) {
			return; //stops the whole thing!
		}

		//logging facility
		myLogger = mainLogger.subLogger("HPC CreateDataset", LogLevel.INFO);
		myLogger.info("============ collected this params: ===============");
		myLogger.info(di);

		try {
			final ObjectMapper om = new ObjectMapper();
			myLogger.info("SUBMITTED JSON:\n" + om.writeValueAsString(di));

			final HttpURLConnection connection = (HttpURLConnection)new URL("http://"+this.url+"/datasets").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","application/json");
			connection.setDoOutput(true);
			connection.connect();
			om.writeValue(connection.getOutputStream(), di);

			newDatasetUUID = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
		} catch (JsonProcessingException e) {
			myLogger.error("Error converting dataset description into JSON:"+e.getMessage());
		} catch (MalformedURLException e) {
			myLogger.error("Malformed URL, probably because of \"http://\" prefix. Please use only hostname:port without any spaces.");
			myLogger.error(e.getMessage());
		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}