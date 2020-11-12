/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasetserver")
public class DatasetServerEndpoint {

	@GET
	@Path("{blocks}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response readBlocks(@PathParam(value = "blocks") String blocks)
	{
		JsonParser parser = Json.createParser(new StringReader(blocks));
		log.info(blocks);
		parser.next();
		JsonArray object = parser.getArray();
		log.info("" + object);
		return Response.ok(new byte[1024]).build();
	}
}
