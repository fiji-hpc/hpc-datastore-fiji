package cz.it4i.fiji.legacy;

import static java.nio.file.Paths.get;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;

import cz.it4i.fiji.datastore.rest_client.DatasetIndex;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>HPC DataStore>Delete>Delete info about uploaded datasets")
public class DeleteDatasetIndex extends DynamicCommand {

	@Parameter(label = "Directory with indices:",
			visibility = ItemVisibility.MESSAGE,
			persist = false, required = false)
	public String pathToDatasetIndexDirectory = DatasetIndex.getPath()
		.toAbsolutePath().toString();

	@Parameter(label = "Discovered index files: ",
			choices = {}, initializer = "initFileLists",
			description = "This is just a listing, choosing any item has no effect on anything.",
			persist = false, required = false)
	public String listFiles = "no files found";

	@Parameter(label = "Info: ", visibility = ItemVisibility.MESSAGE)
	public String info = "All the above listed files will be deleted.";

	private List<File> removeFiles = null;
	void initFileLists() {
		try {
			//pathToDatasetIndexDirectory = DatasetIndex.getPath().toAbsolutePath().toString();
			removeFiles = Files.walk(get(pathToDatasetIndexDirectory))
					.skip(1)    //skip over the folder itself
					.sorted()   //make it a bit more comfortable for the users
					.map(Path::toFile)
					.collect(Collectors.toList());
			getInfo()
					.getMutableInput("listFiles",String.class)
					.setChoices( removeFiles.stream().map(File::getName).collect(Collectors.toList()) );
					//NB: converts List<File> -> List<String>
		}
		catch (IOException exc) {
			myLogger.error("Error listing files", exc);
		}
	}

	@Parameter(label = "Are you sure?", persist = false)
	public boolean areYouSure = false;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC DeleteIndex", LogLevel.INFO);
		if (!areYouSure) {
			myLogger.info("Doing nothing, user is not sure...");
			return;
		}

		removeFiles.forEach(f ->
				myLogger.info("deleting " + f.getAbsolutePath()
						+ (f.delete() ? " succeeded":" failed") )
		);
	}
}
