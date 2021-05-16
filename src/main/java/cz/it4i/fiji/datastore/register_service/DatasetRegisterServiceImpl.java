/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;

import bdv.export.ExportMipmapInfo;
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.DataStoreException;
import cz.it4i.fiji.datastore.N5Access;
import cz.it4i.fiji.datastore.N5Access.N5Description;
import cz.it4i.fiji.datastore.management.DataServerManager;
import cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel;
import lombok.NonNull;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;

@Default
@RequestScoped
public class DatasetRegisterServiceImpl {

	@Inject
	ApplicationConfiguration configuration;

	@Inject
	DataServerManager dataServerManager;

	private Map<String, Compression> name2compression = null;

	@Inject
	DatasetRepository datasetDAO;

	@Transactional
	public UUID createEmptyDataset(DatasetDTO dataset) throws IOException,
		SpimDataException
	{
		UUID result = UUID.randomUUID();
		String path = configuration.getDatasetPath(result);
		N5Access.createNew(path, convert(dataset));
		datasetDAO.persist(Dataset.builder().uuid(result).path(path).build());
		return result;
	}

	public URL start(UUID uuid, String version, OperationMode mode,
		Long timeout) throws DataStoreException
	{
		try {
			return dataServerManager.startDataServer(uuid, version, mode, timeout);
		}
		catch (IOException exc) {
			throw new DataStoreException(exc);
		}
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

}
