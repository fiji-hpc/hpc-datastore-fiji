/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.core.DatasetDTO.ResolutionLevel;
import cz.it4i.fiji.datastore.rest_client.N5RESTAdapter.AngleChannelTimepoint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;

@Log4j2
public final class DatasetIndex {

	private static final Path IMAGEJ_DIRECTORY = Paths.get(".imagej");
	private static final Path DATA_STORE_DIRECTORY = Paths.get("hpc_datastore");
	private static final Path DATA_STORE_INDEX_FILE = Paths.get("main.index");

	private final Map<String, UUID> datasetPath2UUID = new HashMap<>();

	private final DatasetDTO dto;
	private PerAnglesChannels perAnglesChannels;



	public DatasetIndex(DatasetDTO dto,
		AbstractSequenceDescription<?, ?, ?> seq)
	{
		this.dto = dto;
		this.perAnglesChannels = PerAnglesChannels.construct(seq);
		loadIndex();
	}

	public DatasetDTO getDTO() {
		return dto;
	}
	
	public N5Writer getWriter(String path, URL url,
		N5WriterWithUUID innerWriter)
	{
		return new N5WriterFilter(path, url, innerWriter);
	}

	private synchronized UUID getUUID(String datasetPath, URL serverURL) {
		UUID result = datasetPath2UUID.get(getKey(datasetPath, serverURL));
		if (result == null) {
			return null;
		}
		DatasetRegisterServiceClient client = RESTClientFactory.create(serverURL
			.toString(), DatasetRegisterServiceClient.class);
		Response response = client.queryDataset(result.toString());
		if (response.getStatus() != Status.OK.getStatusCode()) {
			datasetPath2UUID.remove(getKey(datasetPath, serverURL));
			saveIndex();
			return null;
		}
		return result;
	}


	private void saveIndex() {
		Path filePath = getPath().resolve(DATA_STORE_INDEX_FILE);
		try (BufferedWriter bw = Files.newBufferedWriter(filePath)) {
			for (Entry<String, UUID> entry : datasetPath2UUID.entrySet()) {
				bw.append(entry.getKey() + "=" + entry.getValue());
				bw.newLine();
			}
		}
		catch (IOException exc) {
			log.error("saveIndex", exc);
		}
	}

	private synchronized void loadIndex() {
		Path filePath = getPath().resolve(DATA_STORE_INDEX_FILE);
		try {
			Files.createDirectories(filePath.getParent());
		}
		catch (IOException exc) {
			log.error("loadIndex", exc);
		}
		if (!Files.exists(filePath)) {
			return;
		}
		try (BufferedReader br = Files.newBufferedReader(filePath))
		{
			String line;
			while (null != (line = br.readLine())) {
				String[] tokens = line.split("=");
				datasetPath2UUID.put(tokens[0], UUID.fromString(tokens[1]));
			}
		}
		catch (IOException exc) {
			log.error("loadIndex", exc);
		}
	}

	private synchronized void registerDataset(String datasetPath, URL url,
		UUID uuid)
	{
		String key = getKey(datasetPath, url);
		datasetPath2UUID.put(key, uuid);
		try (BufferedWriter pw = Files.newBufferedWriter(getPath().resolve(
			DATA_STORE_INDEX_FILE),StandardOpenOption.CREATE, StandardOpenOption.APPEND))
		{
			pw.write(key + "=" + uuid);
			pw.newLine();
		}
		catch (IOException exc) {
			log.error("registerDataset", exc);
		}
	}


	private String getKey(String datasetPath, URL url) {
		return datasetPath + ";" + url;
	}

	private BlockIdentification createBlockIdentification(String path,
		DataBlock<?> dataBlock)
	{
		AngleChannelTimepoint act = AngleChannelTimepoint.constructForSeqAndPath(
			perAnglesChannels, path);
		ResolutionLevel resolutionLevel = dto.getResolutionLevels()[act
			.getLevelID()];
		return new BlockIdentification(resolutionLevel.getResolutions(), dataBlock
			.getGridPosition(), act.getTimepointID(), act.getChannelID(), act
				.getAngleID());
	}


	/**
	 * TODO use more generic approach how to obtain imagej directory
	 * 
	 * @return path to index file
	 */
	public static Path getPath() {
		String home = FileUtils.getUserDirectoryPath();
		if (home == null) {
			home = "";
		}
		return Paths.get(home).resolve(IMAGEJ_DIRECTORY).resolve(
			DATA_STORE_DIRECTORY);
	}


	private class N5WriterFilter implements N5WriterWithUUID {

		@Delegate(excludes = N5WriterExcludes.class)
		private final N5WriterWithUUID innerWriter;
		private final String datasetPath;
		private final Set<BlockIdentification> alreadyWritedBlocks =
			new HashSet<>();
		private UUID uuid;
		private URL serverURL;

		
		public N5WriterFilter(String path, URL url, N5WriterWithUUID innerWriter) {
			this.innerWriter = innerWriter;
			this.datasetPath = toRealPath(path);
			this.serverURL = url;
			this.uuid = DatasetIndex.this.getUUID(datasetPath, this.serverURL);
			if (uuid != null) {
				loadIndexOfBlocks();
			}
		}

		@Override
		public void createGroup(String pathName) throws IOException {
			if (uuid == null) {
				innerWriter.createGroup(pathName);
				uuid = innerWriter.getUUID();
				DatasetIndex.this.registerDataset(datasetPath, serverURL, uuid);
			}
			else {
				innerWriter.setUUID(uuid);
			}

		}

		@Override
		public <T> void writeBlock(String pathName,
			DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException
		{
			BlockIdentification bi = createBlockIdentification(pathName, dataBlock);

			if (prepareForWriting(bi)) {
				boolean writed = false;
				try {
					innerWriter.writeBlock(pathName, datasetAttributes, dataBlock);
					registerBlock(bi);
					writed = true;
				}
				finally {
					if (!writed) {
						writeFailed(bi);
					}
				}
			}
		}

		private synchronized void loadIndexOfBlocks() {
			Path path = getPath().resolve(uuid.toString());
			if (!Files.exists(path)) {
				return;
			}
			try (BufferedReader br = Files.newBufferedReader(path))
			{
				String line;
				while(null != (line = br.readLine())) {
					alreadyWritedBlocks.add(BlockIdentification.from(line));
				}
			}
			catch (IOException exc) {
				log.error("Load block index");
			}
		}

		synchronized private boolean prepareForWriting(BlockIdentification bi) {
			if (alreadyWritedBlocks.contains(bi)) {
				return false;
			}
			alreadyWritedBlocks.add(bi);
			return true;
		}

		private synchronized void registerBlock(BlockIdentification bi) {
			try (BufferedWriter bw = Files.newBufferedWriter(getPath().resolve(uuid
				.toString()), StandardOpenOption.APPEND, StandardOpenOption.CREATE))
			{
				bw.append(bi.toString()).append('\n');
			}
			catch (IOException exc) {
				log.error("Load block index", exc);
			}
		}

		private String toRealPath(String path) {
			final File file = new File(path);
			if (file.exists()) {
				try {
					return Paths.get(path).toRealPath().toAbsolutePath().toString();
				}
				catch (IOException exc) {
					log.warn("getPath", exc);
					return path;
				}
			}
			return path;
		}

		synchronized private void writeFailed(BlockIdentification bi) {
			alreadyWritedBlocks.remove(bi);
		}

	}

	private interface N5WriterExcludes {
		void createGroup(String pathName) throws IOException;

		<T> void writeBlock(String pathName, DatasetAttributes datasetAttributes,
			DataBlock<T> dataBlock) throws IOException;

		void createDataset(final String pathName,
			final DatasetAttributes datasetAttributes) throws IOException;

		void createDataset(final String pathName, final long[] dimensions,
			final int[] blockSize, final DataType dataType,
			final Compression compression);
	}

	@EqualsAndHashCode
	@AllArgsConstructor
	private static class BlockIdentification {

		static BlockIdentification from(String line) {
			String[] tokens = line.split("/");
			int i = 0;
			return new BlockIdentification(new int[] { Integer.parseInt(tokens[i++]),
				Integer.parseInt(tokens[i++]), Integer.parseInt(tokens[i++]) },
				new long[] { Long.parseLong(tokens[i++]), Long.parseLong(tokens[i++]),
					Long.parseLong(tokens[i++]) }, Integer.parseInt(tokens[i++]), Integer
						.parseInt(tokens[i++]), Integer.parseInt(tokens[i++]));
		}
		final int[] level;
		final long[] coords;
		final int time;
		final int channel;
		final int angle;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < level.length; i++) {
				sb.append(level[i]).append('/');
			}
			for (int i = 0; i < coords.length; i++) {
				sb.append(coords[i]).append('/');
			}
			sb.append(time).append('/').append(channel).append('/').append(angle);
			return sb.toString();
		}
	}
}