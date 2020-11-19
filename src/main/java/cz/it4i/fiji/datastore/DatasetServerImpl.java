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
import java.nio.ByteBuffer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import mpicbg.spim.data.SpimDataException;

@Default
@RequestScoped
public class DatasetServerImpl implements Closeable {

	private N5Access n5Access;

	@Inject
	private ApplicationConfiguration configuration;


	@PostConstruct
	private void init() throws SpimDataException, IOException {
		n5Access = N5Access.loadExisting(configuration.getDatasetPath());
	}

	@Override
	public void close() {

	}

	public ByteBuffer read(long[] gridPosition, int time, int channel, int angle,
		int[] resolutionLevel) throws IOException
	{
		return n5Access.read(gridPosition, time, channel, angle, resolutionLevel);
	}

}
