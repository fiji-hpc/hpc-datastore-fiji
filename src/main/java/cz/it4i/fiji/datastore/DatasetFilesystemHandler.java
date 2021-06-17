/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getBasePath;
import static cz.it4i.fiji.datastore.DatasetPathRoutines.getDataPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.ws.rs.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;

public class DatasetFilesystemHandler {

	private static final Pattern WHOLE_NUMBER_PATTERN = Pattern.compile("\\d+");

	private final Path pathOfDataset;

	private final String uuid;

	public DatasetFilesystemHandler(String auuid, Path path) {
		pathOfDataset = path;
		uuid = auuid;
	}

	public DatasetFilesystemHandler(String auuid, String path) {
		this(auuid, Paths.get(path));
	}

	public int createNewVersion() throws IOException {
		int latestVersion = getLatestVersion();
		int newVersion = latestVersion + 1;
		createNewVersion(getBasePath(pathOfDataset, latestVersion), getBasePath(
			pathOfDataset, newVersion));
		return newVersion;
	}

	public Collection<Integer> getAllVersions() throws IOException {
		Collection<Integer> result = new LinkedList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(pathOfDataset)) {
			for (Path p : (Iterable<Path>) (() -> ds.iterator())) {
				if (!isBlockFileDirOrVersion(p.toFile())) {
					continue;
				}
				Integer temp = Integer.valueOf(p.getFileName().toString());

				result.add(temp);
			}
		}
		return result;
	}

	public N5Writer getWriter() throws IOException {
		return new N5FSWriter(getDataPath(pathOfDataset, getLatestVersion())
			.toString());
	}

	public N5Writer getWriter(String version) throws IOException {
		if (!WHOLE_NUMBER_PATTERN.matcher(version).matches()) {
			return null;
		}
		return getWriter(Integer.parseInt(version));
	}

	public N5Writer getWriter(int versionNumber) throws IOException {
		Path result = getDataPath(pathOfDataset, versionNumber);
		if (!Files.exists(result)) {
			return null;
		}
		return new N5FSWriter(result.toString());
	}

	public int getLatestVersion() throws IOException {
		return Collections.max(getAllVersions());
	}

	public void deleteVersion(int version) throws IOException {
		Path versionPath = getBasePath(pathOfDataset, version);
		if(!Files.exists(versionPath)) {
			throw new NotFoundException("Dataset with uuid=" + uuid +
				" does not have version " + version);
		}
		// At least one version should remain
		if (getAllVersions().size() == 1) {
			throw new IllegalStateException("Version " + version +
				" is the last version in dataset " + uuid);
		}
		FileUtils.deleteDirectory(versionPath.toFile());
	}

	private void createNewVersion(Path src, Path dst) throws IOException {
		FileUtils.copyDirectory(src.toFile(), dst.toFile(),
			DatasetFilesystemHandler::isNotBlockFileOrDir);
	}

	private static boolean isBlockFileDirOrVersion(File file) {
		return WHOLE_NUMBER_PATTERN.matcher(file.getName().toString()).matches();
	}

	private static boolean isNotBlockFileOrDir(File file) {
		return !isBlockFileDirOrVersion(file);
	}

}
