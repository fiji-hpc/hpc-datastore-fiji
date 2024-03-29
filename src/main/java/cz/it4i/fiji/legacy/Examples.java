/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import cz.it4i.fiji.legacy.util.OpenOsWebBrowser;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ItemIO;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Examples web page")
public class Examples implements Command {
	@Parameter(type = ItemIO.OUTPUT, label = "Just in case, here's again the URL with examples:")
	public String openThisUrl = "https://github.com/fiji-hpc/hpc-datastore-fiji/tree/master/src/main/ijm";

	@Override
	public void run() {
		OpenOsWebBrowser.openUrl(openThisUrl);
	}
}
