/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;

import bdv.export.ExportMipmapInfo;
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.CreateNewDatasetTS;
import cz.it4i.fiji.datastore.CreateNewDatasetTS.N5Description;
import cz.it4i.fiji.datastore.DatasetFilesystemHandler;
import cz.it4i.fiji.datastore.DatasetServerImpl;
import cz.it4i.fiji.datastore.management.DataServerManager;
import cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;

@Log4j2
@Default
@RequestScoped
public class DatasetRegisterServiceImpl {

	public static final int[] IDENTITY_RESOLUTION = new int[] { 1, 1, 1 };

	@Inject
	ApplicationConfiguration configuration;

	@Inject
	DataServerManager dataServerManager;

	@Inject
	DatasetRepository datasetDAO;

	private Map<String, Compression> name2compression = null;

	@Transactional
	public UUID createEmptyDataset(DatasetDTO datasetDTO) throws IOException,
		SpimDataException
	{
		UUID result = UUID.randomUUID();
		Path path = configuration.getDatasetPath(result);
		new CreateNewDatasetTS().run(path, convert(datasetDTO));
		Dataset dataset = DatasetAssembler.createDomainObject(datasetDTO);
		dataset.setUuid(result);
		dataset.setPath(path.toString());
		datasetDAO.persist(dataset);
		return result;
	}

	@Transactional
	public void deleteDataset(String uuid) throws IOException {
		Dataset dataset = getDataset(uuid);
		log.debug("dataset with path {} is deleted", dataset.getPath());
		datasetDAO.delete(dataset);
		FileUtils.deleteDirectory(new File(dataset.getPath()));
	}

	public void deleteVersions(String uuid, List<Integer> versionList)
		throws IOException
	{
		Dataset dataset = getDataset(uuid);
		DatasetFilesystemHandler dfs = new DatasetFilesystemHandler(uuid, dataset
			.getPath());
		for (Integer version : versionList) {
			dfs.deleteVersion(version);
		}
	}

	public DatasetDTO query(String uuid) throws IOException {
		Dataset dataset = getDataset(uuid);
		return DatasetAssembler.createDatatransferObject(dataset);
	}

	public String getCommonMetadata(String uuid) {
		Dataset dataset = getDataset(uuid);
		return Strings.nullToEmpty(dataset.getMetadata());
	}

	@Transactional
	public void setCommonMetadata(String uuid, String commonMetadata) {
		Dataset dataset = getDataset(uuid);
		dataset.setMetadata(commonMetadata);
		datasetDAO.persist(dataset);
	}

	public URL start(String uuid, int[] r, String version, OperationMode mode,
		Long timeout) throws IOException
	{

		Dataset dataset = getDataset(uuid);
		if (null == dataset.getBlockDimension(r)) {
			throw new NotFoundException("Dataset with UUID=" + uuid +
				" has not resolution [" + IntStream.of(r).mapToObj(i -> "" + i).collect(
					Collectors.joining(",")) + "]");
		}
		int resolvedVersion = resolveVersion(dataset, version, mode);
		return dataServerManager.startDataServer(dataset.getUuid(), r,
			resolvedVersion, version.equals("mixedLatest"), mode, timeout);
	}

	public URL start(String uuid, List<int[]> resolutions, Long timeout)
		throws IOException
	{
		Dataset dataset = getDataset(uuid);
		// called only for checking that all resolutions exists
		getNonIdentityResolutions(dataset, resolutions);
		mergeVersions(dataset);
		return dataServerManager.startDataServer(dataset.getUuid(), resolutions,
			timeout);

	}

	public void rebuild(String uuid, List<int[]> resolutions) throws IOException {
		Dataset dataset = getDataset(uuid);
		List<cz.it4i.fiji.datastore.register_service.ResolutionLevel> resolutionLevels =
			getNonIdentityResolutions(dataset, resolutions);
		mergeVersions(dataset);
		rebuildResolutionLevels(dataset, resolutionLevels);
	}

	@SuppressWarnings("unused")
	private void rebuildResolutionLevels(Dataset dataset,
		List<cz.it4i.fiji.datastore.register_service.ResolutionLevel> resolutionLevels)
	{
		// TODO down sampling
	}

	private void mergeVersions(Dataset dataset) throws IOException {
		DatasetFilesystemHandler dfh = new DatasetFilesystemHandler(dataset);
		Collection<Integer> versions = dfh.getAllVersions();
		int minVersion = Collections.min(versions);
		for (int ver : versions.stream().filter(ver -> ver > minVersion).collect(
			Collectors.toList()))
		{
			dfh.deleteVersion(ver);
		}
		dfh.makeAsInitialVersion(minVersion);

	}

	private List<cz.it4i.fiji.datastore.register_service.ResolutionLevel>
		getNonIdentityResolutions(Dataset dataset, List<int[]> resolutions)
	{
		return resolutions.stream().map(res -> getNonIdentityResolution(dataset,
			res)).collect(Collectors.toList());
	}

	private cz.it4i.fiji.datastore.register_service.ResolutionLevel
		getNonIdentityResolution(Dataset dataset, int[] res)
	{
		if (Arrays.equals(IDENTITY_RESOLUTION, res)) {
			throw new IllegalArgumentException("Resolution [1,1,1] cannot be used.");
		}
		cz.it4i.fiji.datastore.register_service.ResolutionLevel result = dataset
			.getResolutionLevel(res);
		if (result == null) {
			throw new NotFoundException("Resolution " + Arrays.toString(res) +
				" not found in dataset " + dataset.getUuid());
		}
		return result;
	}

	private N5Description convert(DatasetDTO dataset) {
// @formatter:off
		return N5Description.builder()
				.voxelType(DataType.valueOf(dataset.getVoxelType().toUpperCase()))
				.dimensions(dataset.getDimensions())
				.voxelDimensions(new FinalVoxelDimensions(dataset.getVoxelUnit(), dataset.getVoxelResolution()))
				.timepoints(dataset.getTimepoints())
				.channels(dataset.getChannels())
				.angles(dataset.getAngles())
				.compression(createCompression(dataset.getCompression()))
				.mipmapInfo(createExportMipmapInfo(dataset.getResolutionLevels())).build();
// @formatter:on				
	}

	private @NonNull ExportMipmapInfo createExportMipmapInfo(
		ResolutionLevel[] resolutionLevels)
	{
		int[][] resolutions = new int[resolutionLevels.length][];
		int[][] subdivisions = new int[resolutionLevels.length][];
		for (int i = 0; i < resolutionLevels.length; i++) {
			resolutions[i] = Arrays.copyOf(resolutionLevels[i].resolutions,
				resolutionLevels[i].resolutions.length);
			subdivisions[i] = Arrays.copyOf(resolutionLevels[i].blockDimensions,
				resolutionLevels[i].blockDimensions.length);
		}
		return new ExportMipmapInfo(resolutions, subdivisions);
	}

	private @NonNull Compression createCompression(String compression) {
		return getCompressionMapping().getOrDefault(compression.toUpperCase(),
			new RawCompression());
	}

	private Dataset getDataset(String uuid) {
		Dataset dataset = datasetDAO.findByUUID(UUID.fromString(uuid)).orElse(null);
		if (dataset == null) {
			throw new NotFoundException("Dataset with uuid=" + uuid + " not found.");
		}
		return dataset;
	}

	private Map<String, Compression> getCompressionMapping() {
		if (name2compression == null) {
			name2compression = new HashMap<>();
			name2compression.put("BZIP2", new Bzip2Compression());
			name2compression.put("GZIP", new GzipCompression());
			name2compression.put("LZ4", new Lz4Compression());
			name2compression.put("RAW", new RawCompression());
			name2compression.put("XZ", new XzCompression());
		}
		return name2compression;
	}

	private void illegalVersionAndModeCombination(String version,
		OperationMode mode)
	{
		throw new IllegalArgumentException("" + mode +
			" mode is not valid for version " + version);
	}

	private int resolveVersion(Dataset dataset, String version,
		OperationMode mode) throws IOException
	{
		DatasetFilesystemHandler dfs = new DatasetFilesystemHandler(dataset
			.getUuid().toString(), dataset.getPath());
		switch (version) {
			case "latest":
				return dfs.getLatestVersion();
			case "new":
				if (mode == OperationMode.READ) {
					illegalVersionAndModeCombination(version, mode);
				}
				return dfs.createNewVersion();
			case "mixedLatest":
				if (mode == OperationMode.WRITE) {
					illegalVersionAndModeCombination(version, mode);
				}
				return dfs.getLatestVersion();
			default:
				try {
					int result = Integer.parseInt(version);
					if (!dfs.getAllVersions().contains(result)) {
						DatasetServerImpl.versionNotFound(result);
					}
					return result;
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException("version (" + version +
						") is not valid.");
				}

		}

	}

}
