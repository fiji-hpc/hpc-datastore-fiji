/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;


public class DataStoreException extends Exception {

	private static final long serialVersionUID = -5159648762082042916L;

	public DataStoreException() {
		super();
	}

	public DataStoreException(String message, Throwable cause,
		boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DataStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataStoreException(String message) {
		super(message);
	}

	public DataStoreException(Throwable cause) {
		super(cause);
	}

}
