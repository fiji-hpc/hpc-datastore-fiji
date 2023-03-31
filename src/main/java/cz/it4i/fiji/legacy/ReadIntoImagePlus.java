/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import ij.plugin.frame.Recorder;
import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import cz.it4i.fiji.legacy.common.ImagePlusTransferrer;

@Plugin(type = Command.class, headless = true, name = "ReadIntoImagePlus - can't be used directly")
public class ReadIntoImagePlus extends ImagePlusTransferrer {
	@Parameter(type = ItemIO.OUTPUT)
	public Dataset outDatasetImg;

	@Override
	public void run() {
		checkAccessRegimeVsDatasetVersionOrThrow();
		adjustReportingVerbosity();

		outDatasetImg = readWithAType();
		mainLogger.info("transfer is finished");
		if (showRunCmd) {
			final String howToRun = reportAsMacroCommand("Read full image");
			mainLogger.info("Corresponding IJM command: "+howToRun);
			Recorder.recordString(howToRun);
		}
	}
}
