package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import net.imglib2.img.Img;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;

@Plugin(type = Command.class, headless = true)
public class WriteFromImagePlus extends ImagePlusTransferrer {
	@Parameter
	public Dataset inDatasetImg;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();
		adjustReportingVerbosity();

		writeWithAType((Img)inDatasetImg);
		if (showRunCmd)
			mainLogger.info("Corresponding IJM command: "+reportAsMacroCommand("Write From Image"));
	}
}
