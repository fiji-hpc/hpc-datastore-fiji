/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getBasePath;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDataPath;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getXMLPath;
import static java.lang.Math.max;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;

import cz.it4i.fiji.datastore.register_service.OperationMode;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimDataException;

@Log4j2
@Default
@ApplicationScoped
public class DatasetServerImpl implements Closeable, Serializable {


	private static final long serialVersionUID = -2060288635563742563L;

	private static final Pattern WHOLE_NUMBER_PATTERN = Pattern.compile("\\d+");

	N5Access n5Access;

	@Inject
	ApplicationConfiguration configuration;

	UUID uuid;

	private String version;

	private OperationMode mode;

	private int[] resolutionLevel;

	private Path datasetBasePath;

	public synchronized void init(UUID aUuid, int[] resolution, String aVersion,
		OperationMode aMode) throws SpimDataException,
		IOException
	{
		uuid = aUuid;
		version = aVersion;
		mode = aMode;
		resolutionLevel = resolution;
		datasetBasePath = configuration.getDatasetPath(uuid);
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
		return n5Access.read(gridPosition, time, channel, angle, resolutionLevel);
	}

	public void write(long[] gridPosition, int time, int channel, int angle,
		InputStream inputStream) throws IOException
	{
		n5Access.write(gridPosition, time, channel, angle, resolutionLevel,
			inputStream);
	}


	public DataType getType(int time, int channel, int angle, int[] level) {
		return n5Access.getType(time, channel, angle, level);
	}

	private void initN5Access() throws SpimDataException, IOException {
		n5Access = new N5Access(getXMLPath(configuration.getDatasetPath(uuid), 0),
			createN5Writer());
	}

	private N5Writer constructChainOfWriters() throws IOException {

		N5WriterItemOfChain result = null;
		List<Integer> versions = new LinkedList<>(getAllVersions());
		Collections.sort(versions);
		for (Integer i : versions) {
			result = new N5WriterItemOfChain(new N5FSWriter(getDataPath(
				datasetBasePath, i).toString()), result);
		}
		return result;
	}

	@SuppressWarnings({ "null" })
	private N5Writer createN5Writer() throws IOException {
		
		switch(version) {
			case "latest":
				return new N5FSWriter(getDataPath(datasetBasePath,
					getLatestVersion()).toString());
			case "new":
				if (mode == OperationMode.READ) {
					illegalVersionAndModeCombination();
				}
				int latestVersion = getLatestVersion();
				int newVersion = latestVersion + 1;
				createNewVersion(getBasePath(datasetBasePath, latestVersion), getBasePath(
					datasetBasePath,
						newVersion));
				return new N5FSWriter(getDataPath(datasetBasePath, newVersion)
					.toString());
			case "mixedLatest":
				if (mode == OperationMode.WRITE) {
					illegalVersionAndModeCombination();
				}
				return constructChainOfWriters();

			default:
				Path result = getPathByVersionIfExists();
				if (result == null) {
					versionNotFound();
				}
				return new N5FSWriter(result.toString());
		}
		
	}

	private void createNewVersion(Path src, Path dst) throws IOException {
		log.info("create new version {}", dst);
		FileUtils.copyDirectory(src.toFile(), dst.toFile(),
			DatasetServerImpl::isNotBlockFileOrDir);
	
	}

	private int getLatestVersion() throws IOException {
		int result = 0;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(datasetBasePath)) {
			for (Path p : (Iterable<Path>) (() -> ds.iterator())) {
				if (!isBlockFileDirOrVersion(p.toFile())) {
					continue;
				}
				int temp = Integer.parseInt(p.getFileName().toString());
	
				result = max(temp, result);
			}
		}
		return result;
	}

	private Collection<Integer> getAllVersions() throws IOException {
		Collection<Integer> result = new LinkedList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(datasetBasePath)) {
			for (Path p : (Iterable<Path>) (() -> ds.iterator())) {
				if (!isBlockFileDirOrVersion(p.toFile())) {
					continue;
				}
				Integer temp = Integer.getInteger(p.getFileName().toString());

				result.add(temp);
			}
		}
		return result;
	}

	private Path getPathByVersionIfExists() {
		if (!WHOLE_NUMBER_PATTERN.matcher(version).matches()) {
			return null;
		}
		Path result = getDataPath(datasetBasePath, Integer.parseInt(version));
		if (!Files.exists(result)) {
			return null;
		}
		return result;
	}

	private void illegalVersionAndModeCombination() {
		throw new IllegalArgumentException("" + mode +
			" mode is not valid for version " + version);
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

	private void versionNotFound() throws FileNotFoundException {
		throw new FileNotFoundException("version " + version + " not found");
	}

	private static boolean isBlockFileDirOrVersion(File file) {
		return WHOLE_NUMBER_PATTERN.matcher(file.getName().toString()).matches();
	}

	private static boolean isNotBlockFileOrDir(File file) {
		return !isBlockFileDirOrVersion(file);
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
			try {
				return innerWriter.readBlock(pathName, datasetAttributes, gridPosition);
			}
			catch (IOException exc) {
				if (next != null) {
					return next.readBlock(pathName, datasetAttributes, gridPosition);
				}
				throw exc;
			}
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
