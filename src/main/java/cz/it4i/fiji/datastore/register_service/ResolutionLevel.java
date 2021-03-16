/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import cz.it4i.fiji.datastore.BaseEntity;
import lombok.Getter;
import lombok.Setter;

public class ResolutionLevel extends BaseEntity {
	
	@Getter
	@Setter
	private int levelId;
	
	@Getter
	@Setter
	private int[] resolutions;
	
	@Getter
	@Setter
	private int[] blockDimensions;
}
