/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetServerEndpoint.ANGLE_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.BLOCKS_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.CHANNEL_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.TIME_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.MODE_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_Z_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Z_PARAM;

import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public interface DatasetServerClient {

	// @formatter:off
	@Path("/{" + MODE_PARAM + "}")
	// @formatter:on
	@GET
	public Response confirm(@PathParam(MODE_PARAM) String mode);

//@formatter:off
	@Path("/{" + X_PARAM + "}"
			+"/{" + Y_PARAM + "}"
			+"/{" +	Z_PARAM + "}"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}"
			+ "{" + BLOCKS_PARAM + ":/?.*}")
	// @formatter:on
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response writeBlock(@PathParam(R_X_PARAM) int rX,
		@PathParam(R_Y_PARAM) int rY, @PathParam(R_Z_PARAM) int rZ,
		@PathParam(X_PARAM) long x, @PathParam(Y_PARAM) long y,
		@PathParam(Z_PARAM) long z, @PathParam(TIME_PARAM) int time,
		@PathParam(CHANNEL_PARAM) int channel, @PathParam(ANGLE_PARAM) int angle,
		@PathParam(BLOCKS_PARAM) String blocks, OutputStream outputStream);
}
