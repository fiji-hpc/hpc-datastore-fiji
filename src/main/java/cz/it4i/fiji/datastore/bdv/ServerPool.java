/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import java.util.HashMap;
import java.util.Map;

import bdv.img.hdf5.Util;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoaderMetaData;
import cz.it4i.fiji.datastore.rest_client.DatasetRegisterServiceClient;
import cz.it4i.fiji.datastore.rest_client.RESTClientFactory;

class ServerPool<T> {

	private DatasetRegisterServiceClient datasetRegisterService;

	private Map<DatasetServerId, DatasetServer<T>> servers =
		new HashMap<>();

	private HPCDatastoreImageLoaderMetaData metadata;

	
	ServerPool(String baseUrl, HPCDatastoreImageLoaderMetaData metadata)
	{
		datasetRegisterService = RESTClientFactory.create(baseUrl, DatasetRegisterServiceClient.class);
		this.metadata = metadata;
	}

	DatasetServer<T> getServerClient(ViewSetupValues setupValues,
		int level)
	{
		DatasetServerId datasetServerId = constructDatasetServerId(setupValues,
			level);

		return servers.computeIfAbsent(datasetServerId,
			$1 -> new DatasetServerProxy<>(datasetRegisterService, datasetServerId));
	}

	private DatasetServerId constructDatasetServerId(ViewSetupValues setupValues, int level) {
		int[] levelResolution = Util.castToInts(metadata.getPerSetupMipmapInfo()
			.get(setupValues.getSetupId()).getResolutions()[level]);
		return new DatasetServerId(metadata.getUuid().toString(), levelResolution,
			level,
			setupValues.getVersion());
	}
}
