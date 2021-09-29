package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;
import cz.it4i.fiji.legacy.util.WaitingCaller;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Read>Read Into Image")
public class ReadIntoImagePlus extends ImagePlusTransferrer implements Command {
	@Parameter(type = ItemIO.OUTPUT)
	public Dataset outDatasetImg;

	@Parameter(required = false)
	public WaitingCaller notifyThisCaller = null;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();
		adjustReportingVerbosity();

		outDatasetImg = readWithAType();
		if (showRunCmd)
			mainLogger.info("Corresponding IJM command: "+reportAsMacroCommand("Read Into Image"));

		if (notifyThisCaller != null)
			notifyThisCaller.innerSaysItsDone();
	}
}
