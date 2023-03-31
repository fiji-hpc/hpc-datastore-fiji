/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.service;

import java.util.Date;

/**
 * Represents a recently opened connection (with its endpoint) and its validity time window.
 */
public class DataStoreConnection {

	public DataStoreConnection(final String datasetServerURL, final long withThisTimeout)
	{
		this.datasetServerURL = datasetServerURL;
		this.createdWithThisTimeout = withThisTimeout;
	}

	final String datasetServerURL;
	final long createdWithThisTimeout;

	private long estEndOfLifeTimepoint;
	final private Date datePrinter = new Date();

	public long timeWhenServerCloses() {
		return estEndOfLifeTimepoint;
	}

	public boolean willServerCloseAfter(final long periodOfTime) {
		return System.currentTimeMillis()+periodOfTime > estEndOfLifeTimepoint;
	}

	public void serverIsUsedNow() {
		estEndOfLifeTimepoint = System.currentTimeMillis() + createdWithThisTimeout;
	}


	@Override
	public String toString() {
		datePrinter.setTime(estEndOfLifeTimepoint);
		return datasetServerURL+" (with timeouts of "
				+(createdWithThisTimeout/1000)+" seconds; closes likely at "
				+datePrinter+")";
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || o.getClass() != this.getClass()) return false;

		return datasetServerURL.equals( ((DataStoreConnection)o).datasetServerURL );
	}

	@Override
	public int hashCode()
	{
		return datasetServerURL.hashCode();
	}
}
