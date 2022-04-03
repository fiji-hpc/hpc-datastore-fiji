/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import net.imagej.ImageJ;
import cz.it4i.fiji.legacy.RequestDatasetServing;

public class DatasetGuiTests {
	public static void main(String[] args) {
		final ImageJ myIJ = new ImageJ();
		myIJ.ui().showUI();
		myIJ.command().run(RequestDatasetServing.class,true);
	}
}
