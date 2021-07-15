package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * This class only collects initial data (URL,UUID) from the user
 * for the next plugin that starts from that and that fetches info
 * from here-selected dataset.
 */
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Request Dataset Serving")
public class RequestDatasetServing implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:")
	public String datasetID;

	@Parameter
	public CommandService cs;

	@Override
	public void run() {
		cs.run(ReadIntoImagePlus.class,true,"URL",URL,"datasetID",datasetID,"accessRegime","read");
	}
}
