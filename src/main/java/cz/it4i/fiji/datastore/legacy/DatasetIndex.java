/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.legacy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import cz.it4i.fiji.datastore.legacy.N5RESTAdapter.AngleChannelTimepoint;
import cz.it4i.fiji.datastore.register_service.DatasetDTO;
import cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;

@Log4j2
final class DatasetIndex {

	private static final Path IMAGEJ_DIRECTORY = Paths.get(".imagej");
	private static final Path DATA_STORE_DIRECTORY = Paths.get("hpc_datastore");
	private static final Path DATA_STORE_INDEX_FILE = Paths.get("main.index");

	private final Map<Path, UUID> datasetPath2UUID = new HashMap<>();

	private final DatasetDTO dto;


	private final AbstractSequenceDescription<?, ?, ?> seq;

	public DatasetIndex(DatasetDTO dto,
		AbstractSequenceDescription<?, ?, ?> seq)
	{
		this.dto = dto;
		this.seq = seq;
		loadIndex();
	}

	public DatasetDTO getDTO() {
		return dto;
	}
	
	public N5Writer getWriter(Path path, N5WriterWithUUID innerWriter) {
		try {
			return new N5WriterFilter(path, innerWriter);
		}
		catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	private synchronized UUID getUUID(Path datasetPath) {
		return datasetPath2UUID.get(datasetPath);
	}


	private synchronized void loadIndex() {
		Path filePath = getPath().resolve(DATA_STORE_INDEX_FILE);
		try {
			Files.createDirectories(filePath.getParent());
		}
		catch (IOException exc) {
			log.error("loadIndex", exc);
		}
		try (BufferedReader br = Files.newBufferedReader(filePath))
		{
			String line;
			while (null != (line = br.readLine())) {
				String[] tokens = line.split("=");
				datasetPath2UUID.put(Paths.get(tokens[0]).toRealPath(), UUID.fromString(
					tokens[1]));
			}
		}
		catch (IOException exc) {
			log.error("loadIndex", exc);
		}
	}

	private synchronized void registerDataset(Path datasetPath, UUID uuid) {
		datasetPath2UUID.put(datasetPath, uuid);
		try (BufferedWriter pw = Files.newBufferedWriter(getPath().resolve(
			DATA_STORE_INDEX_FILE),StandardOpenOption.CREATE, StandardOpenOption.APPEND))
		{
			pw.write(datasetPath + "=" + uuid);
			pw.newLine();
		}
		catch (IOException exc) {
			log.error("registerDataset", exc);
		}
	}


	private BlockIdentification createBlockIdentification(String path,
		DataBlock<?> dataBlock)
	{
		AngleChannelTimepoint act = AngleChannelTimepoint.constructForSeqAndPath(
			seq, path);
		ResolutionLevel resolutionLevel = dto.getResolutionLevels()[act
			.getLevelID()];
		return new BlockIdentification(resolutionLevel.getResolutions(), dataBlock
			.getGridPosition(), act.getTimepointID(), act.channelID, act
				.getChannelID());
	}


	/**
	 * TODO use more generic approach how to obtain imagej directory
	 * 
	 * @return path to index file
	 */
	private static Path getPath() {
		return Paths.get(System.getenv("HOME")).resolve(IMAGEJ_DIRECTORY).resolve(
			DATA_STORE_DIRECTORY);
	}


	private class N5WriterFilter implements N5Writer {

		@Delegate(excludes = N5WriterExcludes.class)
		private final N5WriterWithUUID innerWriter;
		private final Path datasetPath;
		private final Set<BlockIdentification> alreadyWritedBlocks =
			new HashSet<>();
		private UUID uuid;

		
		public N5WriterFilter(Path path, N5WriterWithUUID innerWriter)
			throws IOException
		{
			this.innerWriter = innerWriter;
			this.datasetPath = path.toRealPath();
			this.uuid = DatasetIndex.this.getUUID(datasetPath);
			if (uuid != null) {
				loadIndexOfBlocks();
			}
		}

		@Override
		public void createGroup(String pathName) throws IOException {
			if (uuid == null) {
				innerWriter.createGroup(pathName);
				uuid = innerWriter.getUUID();
				DatasetIndex.this.registerDataset(datasetPath, uuid);
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
			if (!alreadyWritedBlocks.contains(bi)) {
				innerWriter.writeBlock(pathName, datasetAttributes, dataBlock);
				registerBlock(bi);
			}
		}

		private synchronized void loadIndexOfBlocks() {
			try (BufferedReader br = Files.newBufferedReader(getPath().resolve(uuid
				.toString())))
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

		private synchronized void registerBlock(BlockIdentification bi) {
			alreadyWritedBlocks.add(bi);
			try (BufferedWriter bw = Files.newBufferedWriter(getPath().resolve(uuid
				.toString()), StandardOpenOption.APPEND))
			{
				bw.append(bi.toString()).append('\n');
			}
			catch (IOException exc) {
				log.error("Load block index");
			}
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