/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore.rest_client;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.message.Message;

public final class RESTClientFactory {

	private RESTClientFactory() {}

	public static <T> T create(String url, Class<T> type) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(Message.MAINTAIN_SESSION, true);
		return JAXRSClientFactory.create(url, type, Collections.singletonList(
			new JacksonJaxbJsonProvider().disable(
				SerializationFeature.WRAP_ROOT_VALUE).disable(
					SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)), properties,
			true);
	}

}
