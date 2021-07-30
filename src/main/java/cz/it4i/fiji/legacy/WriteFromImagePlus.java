package cz.it4i.fiji.legacy;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Write From Image")
public class WriteFromImagePlus extends ImagePlusTransferrer implements Command {
	@Parameter
	public Dataset inDatasetImg;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();

		writeWithAType(inDatasetImg);
	}
}
