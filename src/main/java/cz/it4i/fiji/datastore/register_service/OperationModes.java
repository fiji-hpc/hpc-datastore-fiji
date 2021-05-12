/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore.register_service;

public enum OperationModes {

		READ("for-reading-only"), WRITE("for-writing-only");


	private String urlPath;

	private OperationModes(String urlPath) {
		this.urlPath = urlPath;
	}

}
