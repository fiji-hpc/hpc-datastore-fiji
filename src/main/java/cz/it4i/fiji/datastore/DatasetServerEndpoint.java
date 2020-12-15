/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.MODE_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_Z_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.UUID;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.VERSION_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Z_PARAM;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import cz.it4i.fiji.datastore.DatasetServerImpl.WritedData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimDataException;


@Slf4j
@Path("/")
@SessionScoped
public class DatasetServerEndpoint implements Serializable {

	private static final long serialVersionUID = 3030620649903413986L;

	private static final String TIME_PARAM = "TIME";

	private static final String CHANNEL_PARAM = "CHANNEL";

	private static final String ANGLE_PARAM = "ANGLE";

	private static final String BLOCKS_PARAM = "BLOCKS";

	private static final Pattern URL_BLOCKS_PATTERN = Pattern.compile(
	"(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)");

	@Inject
	private DatasetServerImpl datasetServer;

	@Inject
	private CheckUUIDVersionTS checkversionUUIDTS;

	// @formatter:off
	@Path("/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + MODE_PARAM + "}"
			+ "/{" + VERSION_PARAM + "}")
	// @formatter:on

	@GET
	public Response confirm(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(MODE_PARAM) String mode)
	{
		Response resp = checkversionUUIDTS.run(uuid, version);
		if (resp != null) {
			return resp;
		}
		try {
			datasetServer.init(java.util.UUID.fromString(uuid));
		}
		catch (SpimDataException | IOException exc) {
			log.warn("init", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
		return Response.ok().entity(String.format(
			"Dataset UUID=%s, version=%s, level=[%d,%d,%d] ready for %s.",
			uuid, version, rX, rY, rZ, mode)).type(MediaType.TEXT_PLAIN).build();
	}

//@formatter:off
	@Path("/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/{" + X_PARAM + "}"
			+ "/{" + Y_PARAM + "}"
			+ "/{" +	Z_PARAM + "}"
			+ "/{" + TIME_PARAM + "}"
			+ "/{" + CHANNEL_PARAM + "}"
			+ "/{" + ANGLE_PARAM +		"}"
			+ "{" + BLOCKS_PARAM + ":/?.*}")
	// @formatter:on
	@GET
	public Response readBlock(@PathParam(R_X_PARAM) int rX,
		@PathParam(R_Y_PARAM) int rY, @PathParam(R_Z_PARAM) int rZ,
		@PathParam(X_PARAM) long x, @PathParam(Y_PARAM) long y,
		@PathParam(Z_PARAM) long z, @PathParam(TIME_PARAM) int time,
		@PathParam(CHANNEL_PARAM) int channel, @PathParam(ANGLE_PARAM) int angle,
		@PathParam(BLOCKS_PARAM) String blocks)
	{
		try {
			List<BlockIdentification> blocksId = new LinkedList<>();
			blocksId.add(new BlockIdentification(new long[] { x, y, z }, time,
				channel, angle));
			extract(blocks, blocksId);
			List<BlockIdentification> notExistentBlocks = new LinkedList<>();
			ByteBuffer result = null;
			for (BlockIdentification bi : blocksId) {
				ByteBuffer block = datasetServer.read(new long[] { bi.gridPosition[0],
					bi.gridPosition[1], bi.gridPosition[2] }, bi.time, bi.channel,
					bi.angle, new int[] { rX, rY, rZ });

				if (block == null) {
					notExistentBlocks.add(bi);
				}
				if (notExistentBlocks.isEmpty()) {
					if (result == null && block != null) {
						result = ByteBuffer.allocate(block.capacity() * blocksId
							.size());
					}
					if (result != null) {
						result.put(block);
					}
				}
			}
			if (notExistentBlocks.isEmpty() && result != null) {
				return Response.ok(result.array()).type(
					MediaType.APPLICATION_OCTET_STREAM).build();
			}
			return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
				.entity("Blocks [" + String.join(",", notExistentBlocks.stream().map(
					Object::toString).collect(Collectors.toList())) +
					"] not found on resolution level {rX:" + rX + ", rY:" + rY + ", rZ:" +
					rZ + "}.").build();
		}
		catch (IOException | NullPointerException exc) {
			log.warn("read", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}

	}

	// @formatter:off
	@Path("{" + UUID + "}"
			+"/{" + R_X_PARAM + "}"
			+"/{" + R_Y_PARAM + "}"
			+"/{" + R_Z_PARAM +	"}"
			+"/{" + VERSION_PARAM + "}"
			+"/{" + X_PARAM + "}"
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
		@PathParam(BLOCKS_PARAM) String blocks, InputStream inputStream)
	{
		List<BlockIdentification> blocksId = new LinkedList<>();
		blocksId.add(new BlockIdentification(new long[] { x, y, z }, time, channel,
			angle));
		extract(blocks, blocksId);
		WritedData data = new WritedData(inputStream);
		try {

			for (BlockIdentification blockId : blocksId) {
				datasetServer.write(blockId.gridPosition, blockId.time, blockId.channel,
					blockId.angle, new int[] { rX, rY, rZ }, data);
			}
		}
		catch (IOException exc) {
			log.warn("write", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
		return Response.ok().build();
	}

	private void extract(String blocks, List<BlockIdentification> blocksId) {

		Matcher matcher = URL_BLOCKS_PATTERN.matcher(blocks);
		while (matcher.find()) {
			blocksId.add(new BlockIdentification(new long[] { getLong(matcher, 1),
				getLong(matcher, 2), getLong(matcher, 3) }, getInt(matcher, 4), getInt(
					matcher, 4), getInt(matcher, 4)));
		}
	}

	private int getInt(Matcher matcher, int i) {
		return Integer.parseInt(matcher.group(i));
	}

	private long getLong(Matcher matcher, int i) {
		return Long.parseLong(matcher.group(i));
	}

	@AllArgsConstructor
	private static class BlockIdentification {

		@Getter
		private final long[] gridPosition;

		@Getter
		private final int time;

		@Getter
		private final int channel;

		@Getter
		private final int angle;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (long i : gridPosition) {
				sb.append(i).append("/");
			}
			sb.append(time).append("/").append(channel).append("/").append(angle);
			return sb.toString();
		}
	}
}
