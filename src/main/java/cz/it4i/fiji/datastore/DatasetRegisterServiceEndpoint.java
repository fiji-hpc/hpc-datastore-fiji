/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimDataException;

@Slf4j
@Path("/")
public class DatasetRegisterServiceEndpoint {


	public static final String UUID = "uuid";
	public static final String X_PARAM = "x";
	public static final String Y_PARAM = "y";
	public static final String Z_PARAM = "z";
	public static final String R_X_PARAM = "RxParam";
	public static final String R_Y_PARAM = "RyParam";
	public static final String R_Z_PARAM = "RzParam";
	public static final String VERSION_PARAM = "versionParam";
	public static final String MODE_PARAM = "mode";
	public static final String TIMEOUT_PARAM = "timeout";

	@Inject
	private ApplicationConfiguration configuration;

	@Inject
	private CheckUUIDVersionTS checkversionUUIDTS;

	@Inject
	private DatasetRegisterServiceImpl datasetRegisterServiceImpl;

//@formatter:off
	@Path("{" + UUID + "}"
			+"/{" + R_X_PARAM + "}"
			+"/{" + R_Y_PARAM + "}"
			+"/{" + R_Z_PARAM +	"}"
			+"/{" + VERSION_PARAM + "}"
			+"/{" + MODE_PARAM + "}")
// @formatter:on
			
	@GET
	public Response start(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(MODE_PARAM) String mode, @QueryParam(TIMEOUT_PARAM) Long timeout)
	{
		log.debug("timeout = {}", timeout);
		Response resp = checkversionUUIDTS.run(uuid, version);
		if (resp != null) {
			return resp;
		}
		return Response.temporaryRedirect(URI.create("/" + uuid + "/" + rX + "/" +
			rY + "/" + rZ + "/" + version + "?mode=" + mode)).build();
	}

	@POST
	@Path("datasets/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEmptyDataset(DatasetDTO dataset)
	{
		log.info("dataset=" + dataset);
		try {
			datasetRegisterServiceImpl.createEmptyDataset(dataset);
			return Response.ok().entity(configuration.getDatasetUUID()).type(
				MediaType.TEXT_PLAIN).build();
		}
		catch (Exception exc) {
			log.warn("read", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
	}

}