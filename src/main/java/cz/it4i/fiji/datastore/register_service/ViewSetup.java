/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import javax.persistence.Entity;

import cz.it4i.fiji.datastore.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
public class ViewSetup extends BaseEntity {
	
	private static final long serialVersionUID = -9083652135631009033L;

	@Getter
	@Setter
	private int index;
	
	@Getter
	@Setter
	private int channelId;
	
	@Getter
	@Setter
	private int angleId;
}
