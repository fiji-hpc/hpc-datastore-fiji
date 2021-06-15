/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore.register_service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public enum OperationMode {

		READ_WRITE("read-write"), READ("read"), WRITE("write"), NOT_SUPPORTED("");


	private static Map<String, OperationMode> url2Mode = new HashMap<>();

	public static OperationMode getByUrlPath(String requrestedURLPath) {
		return url2Mode.getOrDefault(requrestedURLPath, NOT_SUPPORTED);
	}

	static {
		for (OperationMode i : EnumSet.allOf(OperationMode.class)) {
			url2Mode.put(i.urlPath, i);
		}
	}

	@Getter
	private String urlPath;

	private OperationMode(String urlPath) {
		this.urlPath = urlPath;
	}

}
