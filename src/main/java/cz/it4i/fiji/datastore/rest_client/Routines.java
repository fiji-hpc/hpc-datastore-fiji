/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class Routines {

	private Routines() {
	}

	public static DatasetServerClient startDatasetServer(
		DatasetRegisterServiceClient registerService, String uuid,
		int[] resolutions, String version, String mode, Long timeout)
		throws IOException
	{
		Response response = registerService.start(uuid.toString(), resolutions[0],
			resolutions[1], resolutions[2], version, mode, timeout);
		if (response.getStatus() == HttpStatus.SC_TEMPORARY_REDIRECT) {
			String uri = response.getLocation().toString();
			return RESTClientFactory.create(uri, DatasetServerClient.class);

		}
		throw new IOException(String.format(
			"Response for url /%s/%s/%s/%s/%s/%s was: %s - %s", uuid, resolutions[0],
			resolutions[1], resolutions[2], version, mode, response.getStatusInfo()
				.getStatusCode(), response.getStatusInfo().getReasonPhrase()));
	}

	public static <T> DataBlock<T> readBlock(DataType dataType,
		DatasetServerClient datasetServer, long[] gridPosition, int timepoint,
		int channel, int angle)
		throws IOException
	{
		Response response = datasetServer.readBlock(gridPosition[0],
			gridPosition[1], gridPosition[2], timepoint, channel, angle);
		if (response.getStatus() != Status.OK.getStatusCode()) {
			log.warn("readBlock(%s) - status = %s, msg = %s", "" + response
				.getStatusInfo()
						.getStatusCode(), getText((InputStream) response.getEntity()));
			return null;
		}
		@SuppressWarnings("unchecked")
		DataBlock<T> result = (DataBlock<T>) DataBlockRoutines.constructDataBlock(
			gridPosition, response,
			dataType);
		
		return result;
	}

	static String getText(InputStream entity) {
		return new BufferedReader(new InputStreamReader(entity)).lines().collect(
			Collectors.joining("\n"));
	}

}
