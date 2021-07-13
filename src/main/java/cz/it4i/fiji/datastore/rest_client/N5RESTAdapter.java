/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.imglib2.util.Cast;

import org.apache.http.HttpStatus;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.export.ExportMipmapInfo;
import cz.it4i.fiji.datastore.rest_client.DatasetDTO.ResolutionLevel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;


@Log4j2
public class N5RESTAdapter {

	final static Long SERVER_TIMEOUT = 3600000l;

	public static String coordsAsString(long[] position) {
		return LongStream.of(position).mapToObj(i -> "" + i).collect(
			Collectors.joining(","));
	}

	public static final Pattern PATH = Pattern.compile(
		"setup(\\p{Digit}+)/timepoint(\\p{Digit}+)/s(\\p{Digit}+)");

	private final DatasetDTO dto;

	private final DataType dataType;

	private final AbstractSequenceDescription<?, ?, ?> seq;

	public N5RESTAdapter(AbstractSequenceDescription<?, ?, ?> seq,
		Map<Integer, ExportMipmapInfo> perSetupMipmapInfo, BasicImgLoader imgLoader,
		Compression compression)
	{
		this.seq = seq;
		int angles = 1;
		int channels = 1;
		BasicViewSetup setup = seq.getViewSetupsOrdered().get(0);

		final int setupId = setup.getId();

		dataType = N5Utils.dataType(Cast.unchecked(imgLoader
			.getSetupImgLoader(setupId).getImageType()));
// @formatter:off
		dto = DatasetDTO.builder()
				.voxelType(dataType.toString())
				.dimensions(setup.getSize().dimensionsAsLongArray())
				.angles(angles)
				.channels(channels)
				.timepoints(seq.getTimePoints().size())
				.voxelUnit(setup.getVoxelSize().unit())
				.voxelResolution(dimensionsasArray(setup.getVoxelSize()))
				.compression(compression.getType())
				.resolutionLevels(getResolutionsLevels(perSetupMipmapInfo.get(setupId)))
				.build();
// @formatter:on
	}

	public N5WriterWithUUID constructN5Writer(String url) {
		log.debug("constructN5Writer> url={}", url);
		return new N5RESTWriter(url);
	}





	public DatasetDTO getDTO() {
		return dto;
	}

	private ResolutionLevel[] getResolutionsLevels(
		ExportMipmapInfo exportMipmapInfo)
	{
		int[][] resolutions = exportMipmapInfo.intResolutions;
		int[][] subdivisions = exportMipmapInfo.getSubdivisions();

		ResolutionLevel[] result = new ResolutionLevel[resolutions.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = ResolutionLevel.builder().resolutions(resolutions[i])
				.blockDimensions(subdivisions[i]).build();
		}
		return result;
	}

	private static DataBlock<?> constructDataBlock(long[] gridPosition,
		InputStream inputStream, DataType dataType) throws IOException
	{
		DataInputStream dis = new DataInputStream(inputStream);
		int[] size = new int[3];
		for (int i = 0; i < 3; i++) {
			size[i] = dis.readInt();
		}

		DataBlock<?> block = dataType.createDataBlock(size, gridPosition);
		byte[] buffer = new byte[block.getNumElements() * getSizeOfElement(
			dataType)];
		readFully(inputStream, buffer);
		block.readData(ByteBuffer.wrap(buffer));
		return block;
	}

	private static double[] dimensionsasArray(VoxelDimensions voxelSize) {
		double[] result = new double[voxelSize.numDimensions()];
		for (int i = 0; i < result.length; i++) {
			result[i] = voxelSize.dimension(i);
		}
		return result;
	}

	private static int getSizeOfElement(DataType dataType) {
		switch (dataType) {
			case UINT8:
			case INT8:
				return 1;
			case UINT16:
			case INT16:
				return 2;
			case UINT32:
			case INT32:
			case FLOAT32:
				return 4;
			case UINT64:
			case INT64:
			case FLOAT64:
				return 8;
			default:
				throw new IllegalArgumentException("Datatype " + dataType +
					" not supported");
		}
	}

	private static int readFully(InputStream in, byte[] b) throws IOException {
		int n = 0;
		while (n < b.length) {
			int count = in.read(b, n, b.length - n);
			if (count < 0) {
				break;
			}
			n += count;
		}
		return n;
	}

	private class N5RESTWriter implements N5WriterWithUUID {

		private static final String OPERATION_MODE = "read-write";

		private final String url;

		private UUID uuid;

		private boolean alreadyCreated;

		private DatasetRegisterServiceClient registerServiceClient;
		
		private final Map<Integer, DatasetServerClient> level2serverClient =
			new HashMap<>();

		private final Map<String, DatasetAttributes> path2Attributes =
			new HashMap<>();

		public N5RESTWriter(String url) {
			this.url = url;
			this.uuid = readUUID();
		}

		private UUID readUUID() {
			return null;
		}

		@Override
		public void createGroup(String pathName) throws IOException {
			if (!alreadyCreated) {
				Response response = getRegisterServiceClient().createEmptyDataset(dto);
				String result = getText((InputStream) response.getEntity());
				if (response.getStatus() != Status.OK.getStatusCode()) {
					throw new BadRequestException("createGroup " + result);
				}
				uuid = UUID.fromString(result);
				alreadyCreated = true;
			}
		}

		@Override
		public DataBlock<?> readBlock(String pathName,
			DatasetAttributes datasetAttributes, long[] gridPosition)
			throws IOException
		{
		
			AngleChannelTimepoint act = AngleChannelTimepoint.constructForSeqAndPath(
				seq, pathName);
			DatasetServerClient client = getServerClient(act.getLevelID());
			Response response = client.readBlock(gridPosition[0], gridPosition[1],
				gridPosition[2], act.getTimepointID(), act.getChannelID(), act
					.getAngleID());
			if (response.getStatus() != Status.OK.getStatusCode()) {
				log.warn("readBlock({},{},{}) - status = {}, msg = {}", pathName,
					datasetAttributes, ("[" +
					gridPosition[0] + ", " + gridPosition[1] + ", " + gridPosition[0] +
						"]"), "" + response.getStatusInfo().getStatusCode(), getText(
							(InputStream) response.getEntity()));
				return null;
			}
			InputStream is = response.readEntity(InputStream.class);
			return constructDataBlock(gridPosition, is, dataType);
		}

		@Override
		public <T> void writeBlock(String pathName,
			DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException
		{
			AngleChannelTimepoint act = AngleChannelTimepoint.constructForSeqAndPath(
				seq, pathName);
			DatasetServerClient client = getServerClient(act.getLevelID());
			
			long[] pos = dataBlock.getGridPosition();
		
			ModifiedByteArrayOutputStream baos;
			DataOutputStream os = new DataOutputStream(baos =
				new ModifiedByteArrayOutputStream(
					getSizeOfElement(datasetAttributes.getDataType()) * dataBlock
						.getNumElements() + dataBlock.getSize().length * Integer.BYTES));
			for (int i = 0; i < dataBlock.getSize().length; i++) {
				os.writeInt(dataBlock.getSize()[i]);
			}
			os.write(dataBlock.toByteBuffer().array());
			os.flush();
			log.debug("writeBlock path={},coord=[{}],bytes={}", pathName,
				coordsAsString(dataBlock.getGridPosition()), baos.size());
			client.writeBlock(pos[0], pos[1], pos[2], act.getTimepointID(), act
				.getChannelID(), act.getAngleID(), baos.getData());
		}

		@Override
		public DatasetAttributes getDatasetAttributes(String pathName)
			throws IOException
		{
		
			/*Matcher matcher = PATH.matcher(pathName);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("path = " + pathName +
					" not supported.");
			}
			int level = Integer.parseInt(matcher.group(3));
			return new DatasetAttributes(dto.getDimensions(), dto
				.getResolutionLevels()[level].getBlockDimensions(), dataType,
				compression);
				*/
			return path2Attributes.get(pathName);
		}

		@Override
		public void setDatasetAttributes(String pathName,
			DatasetAttributes datasetAttributes) throws IOException
		{
			path2Attributes.put(pathName, datasetAttributes);
		}

		@Override
		public void setAttributes(String pathName, Map<String, ?> attributes)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T getAttribute(String pathName, String key, Class<T> clazz)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean exists(String pathName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String[] list(String pathName) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Class<?>> listAttributes(String pathName)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(String pathName) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean deleteBlock(String pathName, long[] gridPosition)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public UUID getUUID() {
			return uuid;
		}

		@Override
		public void setUUID(UUID uuid) {
			this.uuid = uuid;
		}

		@Override
		public void close() {
			for (DatasetServerClient dsc : level2serverClient.values()) {
				dsc.stopDataServer();
			}
		}

		private synchronized DatasetRegisterServiceClient
			getRegisterServiceClient()
		{
			if (registerServiceClient == null) {
				registerServiceClient = RESTClientFactory.create(url,
					DatasetRegisterServiceClient.class);
			}
			return registerServiceClient;
		}

		private synchronized DatasetServerClient getServerClient(int levelId) {
			DatasetServerClient result = level2serverClient.get(levelId);
			if (result == null) {
				ResolutionLevel resolutionLevel = dto.getResolutionLevels()[levelId];
				Response response = getRegisterServiceClient().start(uuid.toString(),
					resolutionLevel.getResolutions()[0], resolutionLevel
						.getResolutions()[1], resolutionLevel.getResolutions()[2], "latest",
					OPERATION_MODE, SERVER_TIMEOUT);
				if (response.getStatus() == HttpStatus.SC_TEMPORARY_REDIRECT) {
					String uri = response.getLocation().toString();
					result = RESTClientFactory.create(uri,
						DatasetServerClient.class);
					level2serverClient.put(levelId, result);
				}
				else {
					log.warn("Response for url /{}/{}/{}/{}/{}/{} was: {}", uuid,
						resolutionLevel.getResolutions()[0], resolutionLevel
							.getResolutions()[1], resolutionLevel.getResolutions()[2],
						"latest", OPERATION_MODE, response
							.getStatusInfo());
				}
			}
			return result;
		}

		private String getText(InputStream entity) {
			return new BufferedReader(new InputStreamReader(entity)).lines().collect(
				Collectors.joining("\n"));
		}

	}

	private static class ModifiedByteArrayOutputStream extends
		ByteArrayOutputStream
	{

		private ModifiedByteArrayOutputStream() {
			super();
		}

		private ModifiedByteArrayOutputStream(int size) {
			super(size);
		}

		byte[] getData() {
			return buf;
		}
	}

	@EqualsAndHashCode
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	static class AngleChannelTimepoint {

		@Getter
		final int angleID;

		@Getter
		final int channelID;

		@Getter
		final int timepointID;

		@Getter
		final int levelID;

		public static AngleChannelTimepoint constructForSeqAndPath(
			AbstractSequenceDescription<?, ?, ?> seq, String pathName)
		{
			Matcher matcher = PATH.matcher(pathName);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("path = " + pathName +
					" not supported.");
			}
			int setupID = Integer.parseInt(matcher.group(1));
			int timepointID = Integer.parseInt(matcher.group(2));
			int levelID = Integer.parseInt(matcher.group(3));

			BasicViewSetup bvs = seq.getViewSetups().get(setupID);
			int channel = 0;
			int angle = 0;
			if (bvs instanceof ViewSetup) {
				ViewSetup vs = (ViewSetup) bvs;
				channel = vs.getChannel().getId();
				angle = vs.getAngle().getId();
			}

			return new AngleChannelTimepoint(angle, channel, timepointID, levelID);
		}
	}

}
