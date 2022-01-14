/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.ImageJ;

import org.scijava.command.CommandModule;

import bdv.ij.BigDataBrowserPlugIn;

public class RunBigDataBrowser {

	public static void main(String[] args) throws InterruptedException,
		ExecutionException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		Future<CommandModule> result = ij.command().run(BigDataBrowserPlugIn.class,
			true);
		result.get();
	}

}
