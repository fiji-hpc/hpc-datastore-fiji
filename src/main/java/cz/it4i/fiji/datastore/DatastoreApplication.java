/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import io.helidon.common.CollectionsHelper;

import java.util.Set;

import javax.ws.rs.core.Application;

public class DatastoreApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		return CollectionsHelper.setOf(DatasetServerEndpoint.class);
	}
}
