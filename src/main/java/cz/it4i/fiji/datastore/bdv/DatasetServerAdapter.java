/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import cz.it4i.fiji.datastore.rest_client.DatasetServerClient;
import cz.it4i.fiji.datastore.rest_client.Routines;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatasetServerAdapter<T> implements DatasetServer<T> {

	private final DatasetServerClient client;

	@Override
	public DataBlock<T> readBlock(long[] coords, int time,
		ViewSetupValues setupValues) throws IOException
	{
		return Routines.readBlock(DataType.INT16, client, coords, time, setupValues
			.getChannelId(), setupValues.getAngleId());
				
	}


}
