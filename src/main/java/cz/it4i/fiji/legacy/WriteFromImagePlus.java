package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;
import cz.it4i.fiji.legacy.util.WaitingCaller;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Write>Write From Image")
public class WriteFromImagePlus extends ImagePlusTransferrer implements Command {
	@Parameter
	public Dataset inDatasetImg;

	@Parameter(required = false)
	public WaitingCaller notifyThisCaller = null;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();
		adjustReportingVerbosity();

		writeWithAType(inDatasetImg);
		if (showRunCmd)
			mainLogger.info("Corresponding IJM command: "+reportAsMacroCommand("Write From Image"));

		if (notifyThisCaller != null)
			notifyThisCaller.innerSaysItsDone();
	}
}
