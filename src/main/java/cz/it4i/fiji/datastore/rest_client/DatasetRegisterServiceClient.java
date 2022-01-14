/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import cz.it4i.fiji.datastore.core.DatasetDTO;

@Path("/")
public interface DatasetRegisterServiceClient {

	static final String UUID = "uuid";
	static final String X_PARAM = "x";
	static final String Y_PARAM = "y";
	static final String Z_PARAM = "z";
	static final String R_X_PARAM = "RxParam";
	static final String R_Y_PARAM = "RyParam";
	static final String R_Z_PARAM = "RzParam";
	static final String VERSION_PARAM = "versionParam";
	static final String VERSION_PARAMS = "versionParams";
	static final String MODE_PARAM = "mode";
	static final String TIMEOUT_PARAM = "timeout";

	@POST
	@Path("datasets/")
	@Consumes(MediaType.APPLICATION_JSON)
	Response createEmptyDataset(DatasetDTO dataset);

//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}" 
			+ "/{" + MODE_PARAM +"}")
// @formatter:on
	@GET
	Response start(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(MODE_PARAM) String mode,
		@QueryParam(TIMEOUT_PARAM) Long timeout);

	@GET
	@Path("datasets/{" + UUID + "}")
	Response queryDataset(@PathParam(UUID) String uuid);
}
