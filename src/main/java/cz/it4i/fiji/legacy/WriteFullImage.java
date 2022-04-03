package cz.it4i.fiji.legacy;

import net.imagej.Dataset;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Write>Write full image")
public class WriteFullImage implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:", persistKey = "datasetdatasetid")
	public String datasetID;

	@Parameter(label="time point:", min="0",
			description="In units of the respective dataset.")
	public int timepoint = 0;

	@Parameter(label="channel:", min="0",
			description="In units of the respective dataset.")
	public int channel = 0;

	@Parameter(label="angle:", min="0",
			description="In units of the respective dataset.")
	public int angle = 0;

	@Parameter(label = "Selected down-resolution:")
	public String resolutionLevelsAsStr = "[1, 1, 1]";

	@Parameter(label = "Selected version:")
	public String versionAsStr = "0";

	@Parameter(label = "Server alive timeout [miliseconds]:", min = "-1", stepSize = "1000",
			description = "Value of -1 sets timeout to infinity, but that's not a good idea...",
			required = false)
	public int timeout = 30000;

	@Parameter(label = "Verbose reporting:", required = false,
			description = "The change takes effect always during transfers and for future use of this dialog, not for the current use.")
	public boolean verboseLog = false;

	@Parameter
	public Dataset inDatasetImg;

	@Parameter
	public CommandService cs;

	@Override
	public void run() {
		cs.run(WriteFromImagePlus.class,true,
				"URL",URL,
				"datasetID",datasetID,
				"accessRegime","write",
				"datasetUUID",null, //just two informative fields, content is ignored
				"datasetLabel",null,
				"minX",0,
				"maxX",Integer.MAX_VALUE,
				"minY",0,
				"maxY",Integer.MAX_VALUE,
				"minZ",0,
				"maxZ",Integer.MAX_VALUE,
				"timepoint",timepoint,
				"channel",channel,
				"angle",angle,
				"resolutionLevelsAsStr",resolutionLevelsAsStr,
				"versionAsStr",versionAsStr,
				"timeout",timeout,
				"verboseLog",verboseLog,
				"showRunCmd",false
				);
	}
}
