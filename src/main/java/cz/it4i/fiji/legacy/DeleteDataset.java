package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.URL;
import java.io.IOException;
import cz.it4i.fiji.legacy.util.GuiSelectVersion;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Delete dataset or its version")
public class DeleteDataset implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be deleted:")
	public String datasetID = "someDatasetUUID";

	@Parameter(label = "What everything should be deleted:",
			choices = {"whole dataset","select version"})
	public String range = "whole dataset";

	@Parameter
	public CommandService cs;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC DeleteDataset", LogLevel.INFO);

		try {
			if (range.startsWith("whole")) {
				myLogger.info("Deleting dataset "+datasetID+" at "+url);
				new URL("http://"+url+"/datasets/"+datasetID+"/delete").openStream();
			} else {
				cs.run(GuiSelectVersion.class,true,
						"url",url, "datasetID",datasetID);
			}
		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}