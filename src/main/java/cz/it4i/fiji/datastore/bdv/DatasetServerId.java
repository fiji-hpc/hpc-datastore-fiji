/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.EqualsAndHashCode.Exclude;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
class DatasetServerId {

	@Exclude
	private final String uuid;

	@Exclude
	private final int[] resolution;

	private final int level;
	private final String version;

}
