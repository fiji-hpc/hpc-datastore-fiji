/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import com.esotericsoftware.minlog.Log;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

//TODO locking datasets for read/write - with defined timeout
//TODO Support for timeout
//TODO Starting  remote dataservers - use registerservice for start
//TODO - set proper working directory for tests - erase it after test running
public class App implements QuarkusApplication {

	public static void main(String[] args) {
		Quarkus.run(App.class, App::handleExit, args);
	}

	@Override
	public int run(String... args) throws Exception {
		Quarkus.waitForExit();
		return 0;
	}

	public static void handleExit(Integer status, Throwable t) {
		if (t != null) {
			Log.error("Unhandled exception", t);
		}
		System.exit(status);
	}
}
