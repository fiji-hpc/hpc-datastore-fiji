/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@ApplicationScoped
public class CheckUUIDVersionTS {

	@Inject
	private ApplicationConfiguration configuration;

	public Response run(String uuid, String version) {
		if (!Objects.equals(uuid, configuration.getDatasetUUID())) {
			return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_HTML).entity(
				"Dataset with UUID: " + uuid + " not found").build();
		}

		if (!Objects.equals(version, configuration.getDatasetVersion())) {
			return Response.status(Status.NOT_IMPLEMENTED).type(MediaType.TEXT_HTML)
				.entity("Different version then " + configuration.getDatasetVersion() +
					" is not supported.").build();
		}
		return null;
	}
}
