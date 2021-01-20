/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import cz.it4i.fiji.datastore.register_service.Dataset;
import lombok.extern.slf4j.Slf4j;

@Path("/dbproofs")
@Slf4j
public class DbProofs {

	
	@PersistenceContext
	EntityManager entityManager;


	@POST
	@Transactional
	public void createTable() {
		log.info("insertData");
		entityManager.persist(Dataset.builder().build());
	}
}
