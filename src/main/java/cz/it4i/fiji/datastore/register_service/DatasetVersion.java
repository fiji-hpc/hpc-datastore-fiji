/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.util.Set;

import cz.it4i.fiji.datastore.BaseEntity;
import lombok.Getter;
import lombok.Setter;

//@Entity
public class DatasetVersion extends BaseEntity {

	private static final long serialVersionUID = -8255836802533553273L;

	@Getter
	@Setter
	private long value;

	@Getter
	@Setter
	private Set<String> location;
}
