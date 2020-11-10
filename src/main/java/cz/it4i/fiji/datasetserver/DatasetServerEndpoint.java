/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datasetserver;

import static cz.it4i.fiji.datasetserver.DatasetRegisterServiceEndpoint.UUID;
import static cz.it4i.fiji.datasetserver.DatasetRegisterServiceEndpoint.X_PARAM;
import static cz.it4i.fiji.datasetserver.DatasetRegisterServiceEndpoint.Y_PARAM;
import static cz.it4i.fiji.datasetserver.DatasetRegisterServiceEndpoint.Z_PARAM;


import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;

public class DatasetServerEndpoint {

	private static final String TIME_PARAM = "TIME";

	private static final String CHANNEL_PARAM = "CHANNEL";

	private static final String ANGLE_PARAM = "ANGLE";

	@Route(path = "/datasetserver/:" + UUID + "/:" + X_PARAM + "/:" + Y_PARAM + "/:" +
		Z_PARAM + "/:" + TIME_PARAM + "/:" + CHANNEL_PARAM + "/:" + ANGLE_PARAM,
		methods = {
			HttpMethod.POST, HttpMethod.PUT })
	public void writeBlock(RoutingContext ctx, @Param(UUID) String uuid,
		@Param(X_PARAM) long x, @Param(Y_PARAM) long y, @Param(Z_PARAM) long z,
		@Param(TIME_PARAM) long time, @Param(CHANNEL_PARAM) int chanel,
		@Param(ANGLE_PARAM) int angle)
	{

	}

	@Route(path = "/datasetserver/:" + UUID + "/:" + X_PARAM + "/:" + Y_PARAM +
		"/:" + Z_PARAM + "/:" + TIME_PARAM + "/:" + CHANNEL_PARAM + "/:" +
		ANGLE_PARAM, methods = { HttpMethod.GET })
	public void readBlock(RoutingContext ctx, @Param(UUID) String uuid,
		@Param(X_PARAM) long x, @Param(Y_PARAM) long y, @Param(Z_PARAM) long z,
		@Param(TIME_PARAM) long time, @Param(CHANNEL_PARAM) int chanel,
		@Param(ANGLE_PARAM) int angle)
	{

	}
}
