/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datasetserver;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {

	public static void main(String... args) {

		Quarkus.run(MyApp.class, args);


	}

	public static class MyApp implements QuarkusApplication {

		@Override
		public int run(String... args) throws Exception {
			System.out.println("Do startup logic here");
			Quarkus.waitForExit();

			return 0;
		}
	}
}
