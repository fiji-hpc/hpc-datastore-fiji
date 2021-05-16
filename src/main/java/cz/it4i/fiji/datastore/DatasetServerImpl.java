/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import cz.it4i.fiji.datastore.register_service.OperationMode;
import mpicbg.spim.data.SpimDataException;

@Default
@SessionScoped
public class DatasetServerImpl implements Closeable, Serializable {


	private static final long serialVersionUID = -2060288635563742563L;

	N5Access n5Access;

	@Inject
	ApplicationConfiguration configuration;

	UUID uuid;

	private String version;

	private OperationMode mode;

	public synchronized void init(UUID aUuid, String aVersion,
		OperationMode aMode) throws SpimDataException,
		IOException
	{
		uuid = aUuid;
		version = aVersion;
		mode = aMode;
		initN5Access();
	}

	@Override
	public synchronized void close() {
		uuid = null;
		n5Access = null;
	}

	public DataBlock<?> read(long[] gridPosition, int time, int channel,
		int angle,
		int[] resolutionLevel) throws IOException
	{
		return n5Access.read(gridPosition, time, channel, angle, resolutionLevel);
	}

	public void write(long[] gridPosition, int time, int channel, int angle,
		int[] resolutionLevel, InputStream inputStream) throws IOException
	{
		n5Access.write(gridPosition, time, channel, angle, resolutionLevel,
			inputStream);
	}


	public DataType getType(int time, int channel, int angle, int[] level) {
		return n5Access.getType(time, channel, angle, level);
	}

	private void initN5Access() throws IOException, SpimDataException {
		n5Access = N5Access.loadExisting(configuration.getDatasetPath(uuid));
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
		ClassNotFoundException
	{
		in.defaultReadObject();
		try {
			initN5Access();
		}
		catch (SpimDataException exc) {
			throw new IOException(exc);
		}
	}
}
