package cz.it4i.fiji.legacy;

import static java.nio.file.Paths.get;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cz.it4i.fiji.datastore.rest_client.DatasetIndex;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>HPC DataStore>Delete>Delete info about uploaded datasets")
public class DeleteDatasetIndex implements Command {

	@Parameter(label = "Directory to delete:", persist = false)
	public String pathToDatasetIndexDirectory = DatasetIndex.getPath()
		.toAbsolutePath().toString();

	@Parameter(label = "Are you sure?", persist = false)
	public boolean areYouSure = false;

	@Parameter
	public CommandService cs;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		if (!areYouSure) {
			myLogger.info("Doing nothing, user is not sure...");
			return;
		}
		try {
			Files.walk(get(pathToDatasetIndexDirectory)).sorted(Comparator
				.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
		catch (IOException exc) {
			mainLogger.error("delete", exc);
		}

	}
}
