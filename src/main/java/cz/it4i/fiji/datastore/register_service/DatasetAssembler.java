/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;



public class DatasetAssembler {

	
	public static Dataset createDomainObject(DatasetDTO dto) {
		// @formatter:off
		return Dataset
				.builder()
				.voxelType(dto.getVoxelType())
				.dimensions(dto.getDimensions())
				.voxelUnit(dto.getVoxelUnit())
				.voxelResolution(dto.getVoxelResolution())
				.timepoints(dto.getTimepoints()).timepointResolution(createDomainObject(dto.getTimepointResolution()))
				.channels(dto.getChannels()).channelResolution(createDomainObject(dto.getChannelResolution()))
				.angles(dto.getAngles()).angleResolution(createDomainObject(dto.getAngleResolution()))
				.compression(dto.getCompression())
				.resolutionLevel(createDomainObject(dto.getResolutionLevels()))
				.build();
	// @formatter:on
	}

	private static Resolution createDomainObject(DatasetDTO.Resolution dto) {
		return new Resolution(dto.value, dto.unit);
	}
	
	
	private static Collection<ResolutionLevel> createDomainObject(
		DatasetDTO.ResolutionLevel[] resolutionLevels)
	{

		Stream<ResolutionLevel> resLevels = Arrays.asList(resolutionLevels)
			.stream().map(dto -> new ResolutionLevel(dto.resolutions,
				dto.blockDimensions));
		
		return Streams.zip(LongStream.iterate(1, i -> i + 1).mapToObj(i -> Long
			.valueOf(i)), resLevels, (i, r) -> {
				r.setId(i);
				return r;
			}).collect(Collectors.toList());
	}

	public static DatasetDTO createDatatransferObject(Dataset dataset) {
		// @formatter:off
		return DatasetDTO
				.builder()
				.voxelType(dataset.getVoxelType())
				.dimensions(dataset.getDimensions())
				.voxelUnit(dataset.getVoxelUnit())
				.voxelResolution(dataset.getVoxelResolution())
				.timepoints(dataset.getTimepoints()).timepointResolution(createDatatransferObject(dataset.getTimepointResolution()))
				.channels(dataset.getChannels()).channelResolution(createDatatransferObject(dataset.getChannelResolution()))
				.angles(dataset.getAngles()).angleResolution(createDatatransferObject(dataset.getAngleResolution()))
				.compression(dataset.getCompression())
				.resolutionLevels(createDatatransferObject(dataset.getResolutionLevel()))
				.build();
	// @formatter:on
	}

	private static
		cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel[]
		createDatatransferObject(
		Collection<ResolutionLevel> resolutionLevel)
	{

		cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel[] result =
			new cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel[resolutionLevel
				.size()];
		int i = 0;
		for (ResolutionLevel rl : resolutionLevel) {
			result[i++] =
				new cz.it4i.fiji.datastore.register_service.DatasetDTO.ResolutionLevel(
					rl.getResolutions(), rl.getBlockDimensions());
		}
		return result;
	}

	private static cz.it4i.fiji.datastore.register_service.DatasetDTO.Resolution
		createDatatransferObject(Resolution resolution)
	{
		return new DatasetDTO.Resolution(resolution.getValue(), resolution
			.getUnit());
	}
}
