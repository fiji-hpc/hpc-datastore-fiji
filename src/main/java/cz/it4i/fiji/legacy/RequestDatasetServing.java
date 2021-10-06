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
	@Parameter(label = "URL of a DatasetsRegisterService:",
			description = "IPaddress:portNumber or hostname:portNumber, always without http://")
	public String URL;

	@Parameter(label = "UUID of a dataset on that service:",
			description = "A dataset name that one just needs to know, the Service is not listing available dataset names.")
	public String datasetID;

	@Parameter(label = "Access mode:", choices = {"read","write","read-write"})
	public String accessRegime;

	@Parameter
	public CommandService cs;

	@Override
	public void run() {
		if (accessRegime.equals("read-write")) {
			System.out.println("Not implemented yet. Sorry.");
			return;
		}

		Class<? extends Command> clazz = accessRegime.equals("read") ? ReadIntoImagePlus.class : WriteFromImagePlus.class;
		cs.run(clazz,true,"URL",URL,"datasetID",datasetID,"accessRegime",accessRegime);
	}
}
