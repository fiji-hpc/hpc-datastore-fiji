/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationConfiguration implements Serializable{

	private static final long serialVersionUID = -5159325588360781467L;

	public Path getDatasetPath(UUID uuid) {
		return getDatastorePath().resolve(uuid.toString());
	}


	public Path getDatastorePath() {
		return Paths.get(System.getProperty("datastore.path", "output"));
	}
}
