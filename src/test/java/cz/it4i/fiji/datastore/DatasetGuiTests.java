/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import cz.it4i.fiji.legacy.ReadIntoImagePlus;
import cz.it4i.fiji.legacy.RequestDatasetServing;
import net.imagej.ImageJ;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DatasetGuiTests {
/* -- everything is killed after the @test is over...
even stuff living in a separate thread is killed...
	static ImageJ ij;
	static Runnable ijThread;

	@BeforeAll
	public static void startFiji() {
		System.out.println("thread starting");
		ijThread = new Thread(() -> { ij = new ImageJ(); ij.ui().showUI();
			System.out.println("Fiji started in own thread"); });
		ijThread.run();
		System.out.println("thread started");

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("waiting finished");
	}

	@Test
	public void OpenEntryMenuItem() {
		System.out.println(ij.getLocation());
		ij.command().run(RequestDatasetServing.class,true);
		try {
			ijThread.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
*/

	public static void main(String[] args) {
		final ImageJ myIJ = new ImageJ();
		myIJ.ui().showUI();
		myIJ.command().run(ReadIntoImagePlus.class,true,
				"URL","localhost:8080",
				"datasetID","c2684432-c589-4898-9727-c0601655d8d4",
				"accessRegime","read");
	}
}
