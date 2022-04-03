package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import cz.it4i.fiji.legacy.util.GuiSelectVersion;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Delete>Delete dataset or its version")
public class DeleteDataset implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be deleted:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	@Parameter(label = "What everything should be deleted:",
			choices = {"whole dataset","select version"})
	public String range = "whole dataset";

	@Parameter(label = "Are you sure?", persist = false)
	public boolean areYouSure = false;

	@Parameter
	public CommandService cs;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC DeleteDataset", LogLevel.INFO);
		if (!areYouSure) {
			myLogger.info("Doing nothing, user is not sure...");
			return;
		}

		try {
			if (range.startsWith("whole")) {
				myLogger.info("Deleting dataset "+datasetID+" at "+url);
				final HttpURLConnection connection = (HttpURLConnection)new URL("http://"+url+"/datasets/"+datasetID+"/delete").openConnection();
				connection.getInputStream(); //the communication happens only after this command
				myLogger.info("Server responded: "+connection.getResponseMessage());
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
