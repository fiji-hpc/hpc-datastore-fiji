/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DatasetPathRoutines {

	private DatasetPathRoutines() {}

	static Path getBasePath(Path baseDirectory, int version) {
		return baseDirectory.resolve("" + version);
	}

	static Path getXMLPath(Path baseDirectory, int version) {
		return getBasePath(baseDirectory, version).resolve("export.xml");
	}

	static Path getDataPath(Path baseDirectory, int version) {
		return getDataPath(getXMLPath(baseDirectory, version));
	}

	static Path getDataPath(Path pathToXML) {
		return Paths.get(pathToXML.toString().replaceAll("\\.xml$", ".n5"));
	}
}
