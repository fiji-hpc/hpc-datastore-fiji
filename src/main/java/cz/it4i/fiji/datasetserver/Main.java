/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datasetserver;

import io.quarkus.runtime.annotations.QuarkusMain;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;

@QuarkusMain
public class Main {

	public static void main(String... args) throws IOException {

		N5Reader reader = new N5FSReader("/home/koz01/aaa/export.n5");
		System.out.println("" + reader.getDatasetAttributes("setup0"));

	}

}
