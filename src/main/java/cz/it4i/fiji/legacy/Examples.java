package cz.it4i.fiji.legacy;

import cz.it4i.fiji.legacy.util.OpenOsWebBrowser;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ItemIO;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Open examples web page")
public class Examples implements Command {
	@Parameter(label = "Web page with examples:", persist = false, required = false)
	public String url = "https://github.com/fiji-hpc/hpc-datastore-fiji/tree/master/src/main/ijm";

	@Parameter(type = ItemIO.OUTPUT, label = "Just in case, here's again the URL with examples:")
	public String openThisUrl = url;

	@Parameter(visibility = ItemVisibility.MESSAGE)
	public String info = "When you click OK, I'll try to open a browser for you, okay?";

	@Override
	public void run() {
		OpenOsWebBrowser.openUrl(url);
	}
}
