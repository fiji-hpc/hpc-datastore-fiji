/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getXMLPath;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import cz.it4i.fiji.datastore.register_service.OperationMode;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import mpicbg.spim.data.SpimDataException;

@Default
@ApplicationScoped
public class DatasetServerImpl implements Closeable, Serializable {


	private static final long serialVersionUID = -2060288635563742563L;

	private static final Set<OperationMode> READING_MODES = EnumSet.of(
		OperationMode.READ, OperationMode.READ_WRITE);

	private static final Set<OperationMode> WRITING_MODES = EnumSet.of(
		OperationMode.WRITE, OperationMode.READ_WRITE);

	N5Access n5Access;

	@Inject
	ApplicationConfiguration configuration;

	UUID uuid;

	private int version;

	private boolean mixedVersion;

	private OperationMode mode;

	private int[] resolutionLevel;

	private DatasetFilesystemHandler datasetFilesystemHandler;

	public synchronized void init(UUID aUuid, int[] resolution, int aVersion,
		boolean aMixedVersion, OperationMode aMode) throws SpimDataException,
		IOException
	{
		uuid = aUuid;
		version = aVersion;
		mixedVersion = aMixedVersion;
		mode = aMode;
		resolutionLevel = resolution;
		datasetFilesystemHandler = new DatasetFilesystemHandler(uuid.toString(),
			configuration
			.getDatasetPath(uuid));
		initN5Access();
	}

	@Override
	public synchronized void close() {
		uuid = null;
		n5Access = null;
	}

	public DataBlock<?> read(long[] gridPosition, int time, int channel,
		int angle) throws IOException
	{
		if (!READING_MODES.contains(mode)) {
			throw new IllegalStateException("Cannot read in mode: " + mode);
		}
		return n5Access.read(gridPosition, time, channel, angle, resolutionLevel);
	}

	public void write(long[] gridPosition, int time, int channel, int angle,
		InputStream inputStream) throws IOException
	{
		if (!WRITING_MODES.contains(mode)) {
			throw new IllegalStateException("Cannot write in mode: " + mode);
		}
		n5Access.write(gridPosition, time, channel, angle, resolutionLevel,
			inputStream);
	}


	public DataType getType(int time, int channel, int angle, int[] level) {
		return n5Access.getType(time, channel, angle, level);
	}

	private void initN5Access() throws SpimDataException, IOException {
		n5Access = new N5Access(getXMLPath(configuration.getDatasetPath(uuid),
			datasetFilesystemHandler.getLatestVersion()),
			createN5Writer());
	}

	private N5Writer constructChainOfWriters() throws IOException {

		N5WriterItemOfChain result = null;
		List<Integer> versions = new LinkedList<>(datasetFilesystemHandler
			.getAllVersions());
		Collections.sort(versions);
		for (Integer i : versions) {
			if (i > version) {
				continue;
			}
			result = new N5WriterItemOfChain(datasetFilesystemHandler.getWriter(i),
				result);
		}
		return result;
	}

	private N5Writer createN5Writer() throws IOException {
		if (mixedVersion) {
			if (mode.allowsWrite()) {
				throw new IllegalArgumentException("Write is not possible for mixed version");
			}
			return constructChainOfWriters();
		}
		return datasetFilesystemHandler.getWriter(version);

	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
		ClassNotFoundException
	{
		in.defaultReadObject();
		try {
			initN5Access();
		}
		catch (SpimDataException exc) {
			throw new IOException(exc);
		}
	}

	public static void versionNotFound(int version) {
		throw new NotFoundException("version " + version + " not found");
	}

	private interface ExcludeReadWriteMethod {
		public DataBlock<?> readBlock(final String pathName,
		final DatasetAttributes datasetAttributes, final long[] gridPosition)
		throws IOException;
	
		public <T> void writeBlock(final String pathName,
			final DatasetAttributes datasetAttributes, final DataBlock<T> dataBlock)
			throws IOException;
	
	}

	@AllArgsConstructor
	private static class N5WriterItemOfChain implements N5Writer {

		@Delegate(excludes = { ExcludeReadWriteMethod.class })
		private final N5Writer innerWriter;

		private final N5WriterItemOfChain next;

		@Override
		public DataBlock<?> readBlock(String pathName,
			DatasetAttributes datasetAttributes, long[] gridPosition)
			throws IOException
		{
			DataBlock<?> result = innerWriter.readBlock(pathName, datasetAttributes,
				gridPosition);
			if (result != null) {
				return result;
			}

			if (next != null) {
				return next.readBlock(pathName, datasetAttributes, gridPosition);
			}
			return null;
		}

		@Override
		public <T> void writeBlock(String pathName,
			DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException
		{
			throw new UnsupportedOperationException(
				"Writting mode is not supported for version mixedLatest");
		}

	}
}
