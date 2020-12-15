/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import cz.it4i.fiji.datastore.register_service.Dataset;
import lombok.extern.slf4j.Slf4j;

@Path("/dbproofs")
@Slf4j
public class DbProofs {

	/*@Inject
	@Named("myDataSource")
	private DataSource dataSource;
	
	@PersistenceContext
	private EntityManager entityManager;
	*/
	@POST
	public void createTable() {
		log.info("insertData");
		/*	EntityTransaction trx = entityManager.getTransaction();
			trx.begin();
			entityManager.persist(Dataset.builder().build());
			trx.commit();
		*/
	}
}
