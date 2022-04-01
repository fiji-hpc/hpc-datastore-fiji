/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;


import static cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel.constructLevels;
import static cz.it4i.fiji.datastore.rest_client.DataBlockRoutines.getSizeOfElement;
import static cz.it4i.fiji.datastore.rest_client.Routines.getText;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.imglib2.util.Cast;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel;
import cz.it4i.fiji.datastore.core.ViewRegistrationDTO;
import cz.it4i.fiji.datastore.core.ViewTransformDTO;
import cz.it4i.fiji.datastore.rest_client.PerAnglesChannels.AngleChannel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.VoxelDimensions;


@Log4j2
public class N5RESTAdapter {



	public static String coordsAsString(long[] position) {
		return LongStream.of(position).mapToObj(i -> "" + i).collect(
			Collectors.joining(","));
	}

	public static final Pattern PATH = Pattern.compile(
		"setup(\\p{Digit}+)/timepoint(\\p{Digit}+)/s(\\p{Digit}+)");

	private static final long DEFAULT_DATASERVER_TIMEOUT = 60000l;

	@Getter
	private final DatasetDTO dto;

	private final DataType dataType;

	private final PerAnglesChannels perAnglesChannels;

	public N5RESTAdapter(AbstractSequenceDescription<?, ?, ?> seq,
		ViewRegistrations viewRegistrations, int[][] resolutions,
		int[][] blockDimensions,
		BasicImgLoader imgLoader,
		Compression compression, String label)
	{
		BasicViewSetup setup = seq.getViewSetupsOrdered().get(0);
		this.perAnglesChannels = PerAnglesChannels.construct(seq);
		int angles = perAnglesChannels.getAngles();
		int channels = perAnglesChannels.getChannels();

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
				.resolutionLevels(constructLevels(resolutions, blockDimensions))
				.label(label)
				.viewRegistrations(createViewRegistrations(viewRegistrations))
				.build();
// @formatter:on
	}

	private List<ViewRegistrationDTO> createViewRegistrations(
		ViewRegistrations viewRegistrations)
	{
		List<ViewRegistrationDTO> result = new LinkedList<>();
		for (ViewRegistration iVr : viewRegistrations.getViewRegistrationsOrdered())
		{
			AngleChannel ac = perAnglesChannels.getAngleChannel(iVr.getViewSetupId());
			ViewRegistrationDTO vr = ViewRegistrationDTO.builder().angle(ac
				.getAngleIndex()).channel(ac.getChannelIndex()).time(iVr
					.getTimePointId()).transformations(extractTransformations(iVr))
				.build();
			result.add(vr);

		}

		return result;
	}

	private static List<ViewTransformDTO> extractTransformations(
		ViewRegistration viewRegistration)
	{
		return viewRegistration.getTransformList().stream().map(
			t -> new ViewTransformDTO(t.getName(), t.asAffine3D().getRowPackedCopy()))
			.collect(Collectors.toList());
		
	}

	public N5WriterWithUUID constructN5Writer(String url) {
		return constructN5Writer(url, DEFAULT_DATASERVER_TIMEOUT);
	}

	public N5WriterWithUUID constructN5Writer(String url,
		long dataserverTimeout)
	{
		log.debug("constructN5Writer> url={}", url);
		return new N5RESTWriter(url, dataserverTimeout);
	}

	private static double[] dimensionsasArray(VoxelDimensions voxelSize) {
		double[] result = new double[voxelSize.numDimensions()];
		for (int i = 0; i < result.length; i++) {
			result[i] = voxelSize.dimension(i);
		}
		return result;
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

		private long dataserverTimeout;

		public N5RESTWriter(String url, long aDataserverTimeout) {
			this.url = url;
			this.uuid = readUUID();
			this.dataserverTimeout = aDataserverTimeout;
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
			DatasetAttributes datasetAttributes, long... gridPosition)
			throws IOException
		{
		
			AngleChannelTimepoint act = AngleChannelTimepoint.constructForSeqAndPath(
				perAnglesChannels, pathName);
			DatasetServerClient client = getServerClient(act.getLevelID());

			return Routines.readBlock(dataType, client, gridPosition, act
				.getTimepointID(), act.getChannelID(), act.getAngleID());
		}

		@Override
		public <T> void writeBlock(String pathName,
			DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException
		{
			AngleChannelTimepoint act = AngleChannelTimepoint.constructForSeqAndPath(
				perAnglesChannels, pathName);
			DatasetServerClient client = getServerClient(act.getLevelID());
			
			long[] pos = dataBlock.getGridPosition();
		
			ModifiedByteArrayOutputStream baos;
			DataOutputStream os = new DataOutputStream(baos =
				new ModifiedByteArrayOutputStream(getSizeOfElement(datasetAttributes
					.getDataType()) * dataBlock.getNumElements() + dataBlock
						.getSize().length * Integer.BYTES));
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
		public <T> T getAttribute(String pathName, String key, Type type)
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
		public boolean deleteBlock(String pathName, long... gridPosition)
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
				try {
					dsc.stopDataServer();
				}
				catch (ProcessingException exc) {
					if (exc.getCause() instanceof ConnectException) {
						log.info("Server {} is already stopped.", dsc.toString());

					}
					else {
						log.warn("stop", exc);
					}

				}
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

		private synchronized DatasetServerClient getServerClient(int levelId)
			throws IOException
		{
			DatasetServerClient result = level2serverClient.get(levelId);
			if (result == null) {
				ResolutionLevel resolutionLevel = dto.getResolutionLevels()[levelId];
				result = Routines.startDatasetServer(getRegisterServiceClient(), uuid
					.toString(), resolutionLevel.getResolutions(), "latest",
					OPERATION_MODE, dataserverTimeout);
				level2serverClient.put(levelId, result);
			}
			return result;
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
			PerAnglesChannels perAnglesChannels, String pathName)
		{
			Matcher matcher = PATH.matcher(pathName);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("path = " + pathName +
					" not supported.");
			}
			int setupID = Integer.parseInt(matcher.group(1));
			int timepointID = Integer.parseInt(matcher.group(2));
			int levelID = Integer.parseInt(matcher.group(3));

			AngleChannel ach = perAnglesChannels.getAngleChannel(setupID);
			return new AngleChannelTimepoint(ach.getAngleIndex(), ach
				.getChannelIndex(), timepointID, levelID);
		}
	}

}
