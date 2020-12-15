/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.io.Serializable;
import java.util.UUID;

/*import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;*/

//@Repository(forEntity = Dataset.class)
public abstract class DatasetRepository extends
	AbstractEntityRepository<Dataset, Long> implements Serializable
{

	private static final long serialVersionUID = 7503192760728646786L;

//	@Query("select d from Dataset d where d.uuid = ?1")
	public abstract Dataset findByUUID(UUID uuid);
}
