/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.management;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
@ApplicationScoped
public class DataServerManagerEndpoint {

	@Inject
	DataServerManager dataServerManager;

	@POST
	@Path("/stop")
	public Response stopDataServer() {
		dataServerManager.stopCurrentDataServer();
		return Response.ok().build();
	}
}
