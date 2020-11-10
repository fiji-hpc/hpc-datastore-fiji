/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datasetserver;


import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;


public class DatasetRegisterServiceEndpoint {

	public static final String UUID = "uuid";
	public static final String X_PARAM = "xParam";
	public static final String Y_PARAM = "yParam";
	public static final String Z_PARAM = "zParam";

	@Route(path = "/dataset/:" + UUID, methods = HttpMethod.GET)
	public String get(@Param(UUID) String uuid) {
		return "get uuid: " + uuid;
	}

	@Route(path = "/dataset/:" + UUID, methods = HttpMethod.DELETE)
	public String delete(@Param(UUID) String uuid) {
		return "delete uuid: " + uuid;
	}

	@Route(path = "/dataset/:" + UUID + "/:" + X_PARAM + "/:" + Y_PARAM + "/:" +
		Z_PARAM, methods = HttpMethod.GET)
	public String getXYZ(@Param(UUID) String uuid, @Param(X_PARAM) String x,
		@Param(Y_PARAM) String y, @Param(Z_PARAM) String z)
	{
		return "get uuid: " + uuid + " x y z: " + x + " " + y + " " + z + " ";
	}

	@Route(path = "/dataset/:" + UUID + "/metadata", methods = HttpMethod.GET)
	public String getMetadata(@Param(UUID) String uuid)
	{
		return "getMetadata uuid: " + uuid;
	}

	@Route(path = "/dataset/:" + UUID + "/metadata", methods = { HttpMethod.PUT,
		HttpMethod.POST })
	public String setMetadata(@Param(UUID) String uuid) {
		return "setMetadata uuid: " + uuid;
	}
}