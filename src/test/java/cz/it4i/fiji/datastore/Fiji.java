/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.ImageJ;

import org.junit.jupiter.api.Test;
import org.scijava.command.CommandModule;

import cz.it4i.fiji.datastore.legacy.ExportSPIMAsN5PlugIn;

public class Fiji {

	@Test()
	public void runCommand() throws InterruptedException, ExecutionException {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		Future<CommandModule> result = ij.command().run(ExportSPIMAsN5PlugIn.class,
			true);
		result.get();
	}

}
