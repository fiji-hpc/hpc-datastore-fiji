package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Write>Write From Image")
public class WriteFromImagePlus extends ImagePlusTransferrer implements Command {
	@Parameter
	public Dataset inDatasetImg;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();
		adjustReportingVerbosity();

		writeWithAType(inDatasetImg);
		if (showRunCmd)
			mainLogger.info("Corresponding IJM command: "+reportAsMacroCommand("Write From Image"));
	}
}
