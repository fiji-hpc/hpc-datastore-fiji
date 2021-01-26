/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.legacy;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.ws.rs.core.Response;

import net.imglib2.util.Cast;

import org.apache.http.HttpStatus;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.export.ExportMipmapInfo;
import cz.it4i.fiji.datastore.DatasetServerClient;
import cz.it4i.fiji.datastore.N5Access;
import cz.it4i.fiji.datastore.register_service.DatasetDTO;
import cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel;
import cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceClient;
import cz.it4i.fiji.rest.RESTClientFactory;
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;


@Slf4j
public class N5RESTAdapter {
	public static String coordsAsString(long[] position) {
		return LongStream.of(position).mapToObj(i -> "" + i).collect(
			Collectors.joining(","));
	}

	private final Compression compression;

	private final DatasetDTO dto;

	private final DataType dataType;

	private AbstractSequenceDescription<?, ?, ?> seq;

	public N5RESTAdapter(AbstractSequenceDescription<?, ?, ?> seq,
		Map<Integer, ExportMipmapInfo> perSetupMipmapInfo, BasicImgLoader imgLoader,
		Compression compression)
	{
		this.seq = seq;
		this.compression = compression;
		BasicViewSetup setup = seq.getViewSetupsOrdered().get(0);

		final int setupId = setup.getId();

		dataType = N5Utils.dataType(Cast.unchecked(imgLoader
			.getSetupImgLoader(setupId).getImageType()));
// @formatter:off
		dto = DatasetDTO.builder()
				.voxelType(dataType.toString())
				.dimensions(setup.getSize().dimensionsAsLongArray())
				.timepoints(seq.getTimePoints().size())
				.voxelUnit(setup.getVoxelSize().unit())
				.voxelResolution(dimensionsasArray(setup.getVoxelSize()))
				.compression(compression.getType())
				.resolutionLevels(getResolutionsLevels(perSetupMipmapInfo.get(setupId)))
				.build();
// @formatter:on
	}

	public N5Writer constructN5Writer(String url) {
		log.debug("constructN5Writer> url={}", url);
		return new N5RESTWriter(url);
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

	private static double[] dimensionsasArray(VoxelDimensions voxelSize) {
		double[] result = new double[voxelSize.numDimensions()];
		for (int i = 0; i < result.length; i++) {
			result[i] = voxelSize.dimension(i);
		}
		return result;
	}

	private class N5RESTWriter implements N5Writer {

		private final Pattern PATH = Pattern.compile(
			"setup(\\p{Digit}+)/timepoint(\\p{Digit}+)/s(\\p{Digit}+)");

		private final String url;

		private UUID uuid;

		private boolean alreadyCreated;

		private DatasetRegisterServiceClient registerServiceClient;
		
		private DatasetServerClient serverClient;

		public N5RESTWriter(String url) {
			this.url = url;
			this.uuid = readUUID();
		}

		private UUID readUUID() {
			return null;
		}

		@Override
		public <T> T getAttribute(String pathName, String key, Class<T> clazz)
			throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public DatasetAttributes getDatasetAttributes(String pathName)
			throws IOException
		{

			Matcher matcher = PATH.matcher(pathName);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("path = " + pathName +
					" not supported.");
			}
			int level = Integer.parseInt(matcher.group(3));
			return new DatasetAttributes(dto.getDimensions(), dto
				.getResolutionLevels()[level].getBlockDimensions(), dataType,
				compression);
		}

		@Override
		public DataBlock<?> readBlock(String pathName,
			DatasetAttributes datasetAttributes, long[] gridPosition)
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
		public void setAttributes(String pathName, Map<String, ?> attributes)
			throws IOException
		{
			// do nothing
		}

		@Override
		public void createGroup(String pathName) throws IOException {
			if (!alreadyCreated) {
				String result = getRegisterServiceClient().createEmptyDataset(dto);
				uuid = UUID.fromString(result);
				alreadyCreated = true;
			}
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
		public <T> void writeBlock(String pathName,
			DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException
		{
			DatasetServerClient client = getServerClient();

			Matcher matcher = PATH.matcher(pathName);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("path = " + pathName +
					" not supported.");
			}
			int setupID = Integer.parseInt(matcher.group(1));
			int timepointID = Integer.parseInt(matcher.group(2));

			BasicViewSetup bvs = seq.getViewSetups().get(setupID);
			int channel = 0;
			int angle = 0;
			if (bvs instanceof ViewSetup) {
				ViewSetup vs = (ViewSetup) bvs;
				channel = vs.getChannel().getId();
				angle = vs.getAngle().getId();
			}
			long[] pos = dataBlock.getGridPosition();

			ModifiedByteArrayOutputStream baos;
			DataOutputStream os = new DataOutputStream(baos =
				new ModifiedByteArrayOutputStream(
				N5Access.getSizeOfElement(datasetAttributes) * dataBlock
					.getNumElements() + 3 * 4));
			for (int i = 0; i < dataBlock.getSize().length; i++) {
				os.writeInt(dataBlock.getSize()[i]);
			}
			os.write(dataBlock.toByteBuffer().array());
			os.flush();
			log.debug("writeBlock path={},coord=[{}],bytes={}", pathName,
				coordsAsString(dataBlock.getGridPosition()), baos.size());
			client.writeBlock(pos[0], pos[1], pos[2], timepointID, channel, angle,
				baos.getData());
		}

		@Override
		public boolean deleteBlock(String pathName, long[] gridPosition)
			throws IOException
		{
			throw new UnsupportedOperationException();
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

		private synchronized DatasetServerClient getServerClient() {
			if (this.serverClient == null) {
				Response response = getRegisterServiceClient().start(uuid.toString(), 1,
					1, 1,
					"latest", "write",
					10000l);
				if (response.getStatus() == HttpStatus.SC_TEMPORARY_REDIRECT) {
					String uri = response.getLocation().toString();
					this.serverClient = RESTClientFactory.create(uri,
						DatasetServerClient.class);
					response = this.serverClient.confirm("write");
					log.debug("getServerClient> status={}", response.getStatus());
				}
			}
			return this.serverClient;
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

}
