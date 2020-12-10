/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import io.helidon.microprofile.server.Server;

public class App {

	public static void main(String[] args) {
		Server server = Server.builder().addApplication(DatastoreApplication.class)
			.build();
		server.start();

	}

}
