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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.scijava.command.CommandModule;

import bdv.ij.BigDataBrowserPlugIn;
import cz.it4i.fiji.datastore.ij.ExportSPIMAsN5PlugIn;
import cz.it4i.fiji.legacy.CreateNewDataset;

@Disabled
public class Fiji {


	@Test
	public void runExportSPIMAsN5PlugIn() throws InterruptedException,
		ExecutionException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		Future<CommandModule> result = ij.command().run(ExportSPIMAsN5PlugIn.class,
			true);
		result.get();
	}

	@Test
	public void runCreateNewDataset() throws InterruptedException,
		ExecutionException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		Future<CommandModule> result = ij.command().run(CreateNewDataset.class,
			true);
		result.get();
	}

	@Test
	public void runBigDataBrowser() throws InterruptedException,
		ExecutionException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		Future<CommandModule> result = ij.command().run(BigDataBrowserPlugIn.class,
			true);
		result.get();
	}

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

	}
}
