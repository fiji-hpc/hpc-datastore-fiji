/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class DatasetJSON {

	@ToString
	public static class Resolution {

		@Getter
		@Setter
		double value;

		@Getter
		@Setter
		String unit;
	}

	@ToString
	public static class ResolutionLevel {

		@Getter
		@Setter
		int[] resolutionLevel;

		@Getter
		@Setter
		int[] blockSize;

	}

	@Setter
	@Getter
	long[] dimensions;

	@Getter
	@Setter
	int timepoints = 1;

	@Getter
	@Setter
	int channels = 1;

	@Getter
	@Setter
	int angles = 1;

	@Getter
	@Setter
	Resolution[] pixelResolutions;

	@Getter
	@Setter
	Resolution timepointResolution;

	@Getter
	@Setter
	Resolution channelResolution;

	@Getter
	@Setter
	Resolution angleResolution;

	@Getter
	@Setter
	ResolutionLevel[] resolutionLevels;
}