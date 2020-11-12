/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;


import java.awt.print.Book;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class BlockIdentificationReader implements
	MessageBodyReader<BlockIdentification>
{

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
		Annotation[] annotations, MediaType mediaType)
	{

		return type.equals(BlockIdentification.class);
	}


	@Override
	public BlockIdentification readFrom(Class<BlockIdentification> type,
		Type genericType, Annotation[] annotations, MediaType mediaType,
		MultivaluedMap<String, String> httpHeaders, InputStream is)
		throws IOException, WebApplicationException
	{
		try (JsonReader jsonReader = Json.createReader(is)) {
			JsonObject jsonObject = jsonReader.readObject();

			return null;
		}
	}
}
