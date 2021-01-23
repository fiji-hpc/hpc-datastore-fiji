/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@XmlRootElement
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DatasetDTO {

	@ToString
	public static class Resolution {

		@Getter
		@Setter
		double value;

		@Getter
		@Setter
		String unit;
	}

	@Builder
	@ToString
	public static class ResolutionLevel {

		@Getter
		@Setter
		int[] resolutions;

		@Getter
		@Setter
		int[] blockDimensions;

	}

	@Setter
	@Getter
	private String voxelType;

	@Setter
	@Getter
	private long[] dimensions;

	@Getter
	@Setter
	@Builder.Default
	private int timepoints = 1;

	@Getter
	@Setter
	@Builder.Default
	private int channels = 1;

	@Getter
	@Setter
	@Builder.Default
	private int angles = 1;

	@Getter
	@Setter
	private String voxelUnit;

	@Getter
	@Setter
	private double[] voxelResolution;

	@Getter
	@Setter
	private Resolution timepointResolution;

	@Getter
	@Setter
	private Resolution channelResolution;

	@Getter
	@Setter
	private Resolution angleResolution;

	@Getter
	@Setter
	private String compression;

	@Getter
	@Setter
	private ResolutionLevel[] resolutionLevels;
}