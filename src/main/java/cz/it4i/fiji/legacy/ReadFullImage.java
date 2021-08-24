package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Read Full Image")
public class ReadFullImage implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:")
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

	@Parameter(type = ItemIO.OUTPUT)
	public Dataset outDatasetImg;

	@Parameter
	public CommandService cs;

	@Override
	public void run() {
		cs.run(ReadIntoImagePlus.class,true,
				"URL",URL,
				"datasetID",datasetID,
				"accessRegime","read",
				"minX",0,
				"maxX",999999,
				"minY",0,
				"maxY",999999,
				"minZ",0,
				"maxZ",999999,
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
