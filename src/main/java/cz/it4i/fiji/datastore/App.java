/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import io.quarkus.runtime.Quarkus;

//TODO correct stop
//TODO locking datasets for read/write - with defined timeout
//TODO Support for timeout
//TODO Starting  remote dataservers - use registerservice for start
//TODO - set proper working directory for tests - erase it after test running
public class App {

	public static void main(String[] args) {
		Quarkus.run(args);

	}

}
