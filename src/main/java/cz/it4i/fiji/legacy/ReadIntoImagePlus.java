package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Read Into Image")
public class ReadIntoImagePlus extends ImagePlusTransferrer implements Command {
	@Parameter(type = ItemIO.OUTPUT)
	public Dataset outDatasetImg;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();

		outDatasetImg = readWithAType();
		myLogger.info("Corresponding IJM command: "+reportAsMacroCommand("Read Into Image"));
	}
}
