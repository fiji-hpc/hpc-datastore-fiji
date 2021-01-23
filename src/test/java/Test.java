
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceClient;

/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

public class Test {


	public static void main(String[] args) {
		DatasetRegisterServiceClient client = JAXRSClientFactory.create(
			"http://localhost:9080/", DatasetRegisterServiceClient.class);
		client.createEmptyDataset(null);
	}

}
