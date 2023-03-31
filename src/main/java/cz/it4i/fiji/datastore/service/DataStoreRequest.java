/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.service;

/**
 * Collects all data that together form exactly one request to start up a dedicated DatasetServer.
 */
public class DataStoreRequest {
	//cannot create without initialization, there's no other c'tor
	public DataStoreRequest(final String URLwithPort, final String datasetUUID,
	                        final int rx, final int ry, final int rz,
	                        final String version, final String accessRegime, final int timeout)
	{
		reUseFor(URLwithPort,datasetUUID, rx,ry,rz, version,accessRegime,timeout);
	}

	//completely renew
	public void reUseFor(final String URLwithPort, final String datasetUUID,
	                     final int rx, final int ry, final int rz,
	                     final String version, final String accessRegime, final int timeout)
	{
		this.URLwithPort = URLwithPort;
		this.datasetUUID = datasetUUID;
		this.rx = rx;
		this.ry = ry;
		this.rz = rz;
		this.version = version;
		this.accessRegime = accessRegime;
		this.timeout = timeout;
	}

	//partially renew
	public void reUseFor(final int rx, final int ry, final int rz,
	                     final String version, final String accessRegime, final int timeout)
	{
		reUseFor(URLwithPort,datasetUUID, rx,ry,rz, version,accessRegime,timeout);
	}

	private String URLwithPort;
	private String datasetUUID;
	private int rx,ry,rz;
	private String version;
	private String accessRegime;
	private int timeout;

	public long getTimeout()
	{
		return timeout;
	}

	public String createRequestURL()
	{
		return createRequestURL(URLwithPort,datasetUUID, rx,ry,rz, version,accessRegime,timeout);
	}

	static
	public String createRequestURL(final String URLwithPort, final String datasetUUID,
	                     final int rx, final int ry, final int rz,
	                     final String version, final String accessRegime, final int timeout)
	{
		return "http://"+URLwithPort+"/datasets/"+datasetUUID+"/"
				+rx+"/"+ry+"/"+rz+"/"+version+"/"+accessRegime+"?timeout="+timeout;
	}


	@Override
	public String toString() {
		return createRequestURL();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || o.getClass() != this.getClass()) return false;

		final DataStoreRequest dsr = (DataStoreRequest)o;
		if (! dsr.URLwithPort.equals(URLwithPort)) return false;
		if (! dsr.datasetUUID.equals(datasetUUID)) return false;
		if (dsr.rx != rx || dsr.ry != ry || dsr.rz != rz) return false;
		if (! dsr.version.equals(version)) return false;
		if (! dsr.accessRegime.equals(accessRegime)) return false;
		//NB: timeouts are irrelevant here, they ain't deciding what content will be served
		return true;
	}

	@Override
	public int hashCode()
	{
		return URLwithPort.hashCode() + datasetUUID.hashCode() + 7*rx + 5*ry + 3*rz
				+ version.hashCode() + accessRegime.hashCode();
	}
}