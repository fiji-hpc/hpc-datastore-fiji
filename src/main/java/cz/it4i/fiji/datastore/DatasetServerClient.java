/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetServerEndpoint.ANGLE_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.CHANNEL_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.TIME_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Z_PARAM;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public interface DatasetServerClient {


//@formatter:off
	@Path("/{" + X_PARAM + "}"
			+"/{" + Y_PARAM + "}"
			+"/{" +	Z_PARAM + "}"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}")
	// @formatter:on
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response writeBlock(@PathParam(X_PARAM) long x,
		@PathParam(Y_PARAM) long y, @PathParam(Z_PARAM) long z,
		@PathParam(TIME_PARAM) int time, @PathParam(CHANNEL_PARAM) int channel,
		@PathParam(ANGLE_PARAM) int angle, byte[] data);

	//@formatter:off
	@Path("/{" + X_PARAM + "}"
			+"/{" + Y_PARAM + "}"
			+"/{" +	Z_PARAM + "}"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}")
	//@formatter:on
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response readBlock(@PathParam(X_PARAM) long x,
		@PathParam(Y_PARAM) long y, @PathParam(Z_PARAM) long z,
		@PathParam(TIME_PARAM) int time, @PathParam(CHANNEL_PARAM) int channel,
		@PathParam(ANGLE_PARAM) int angle);

	//@formatter:off
	@Path("/datatype"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}")
	// @formatter:on
	@GET
	public Response getType(@PathParam(TIME_PARAM) int time,
		@PathParam(CHANNEL_PARAM) int channel, @PathParam(ANGLE_PARAM) int angle);
}
