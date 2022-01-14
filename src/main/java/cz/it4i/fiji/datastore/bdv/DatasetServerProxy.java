/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import static cz.it4i.fiji.datastore.rest_client.Routines.startDatasetServer;

import java.io.IOException;

import javax.ws.rs.ProcessingException;

import org.janelia.saalfeldlab.n5.DataBlock;

import cz.it4i.fiji.datastore.rest_client.DatasetRegisterServiceClient;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DatasetServerProxy<T> implements DatasetServer<T> {

	private static final int MAX_ATTEMPTS = 10;

	private final DatasetRegisterServiceClient datasetRegisterService;

	private final DatasetServerId datasetServerId;

	private DatasetServer<T> innerServer;

	@Override
	public DataBlock<T> readBlock(long[] coords, int time,
		ViewSetupValues setupValues) throws IOException
	{
		RuntimeException resultException = null;
		int attempts = 0;
		do {
			try {
				return getInnerServer().readBlock(coords, time, setupValues);
			}
			catch (ProcessingException exc) {
				resultException = exc;
				attempts++;
				innerServer = null;
				continue;
			}
		}
		while (attempts < MAX_ATTEMPTS);
		throw resultException;
	}

	private DatasetServer<T> getInnerServer() throws IOException {
		if (innerServer == null) {
			innerServer = new DatasetServerAdapter<>(startDatasetServer(
				datasetRegisterService, datasetServerId.getUuid(), datasetServerId
					.getResolution(), datasetServerId.getVersion(), "read", 10000l));

		}
		return innerServer;
	}

}
