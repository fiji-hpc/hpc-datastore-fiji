/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationConfiguration {



	public String getDatasetPath() {
		return System.getProperty("dataset.path");
	}

	public String getDatasetUUID() {
		return System.getProperty("dataset.uuid");
	}
}
