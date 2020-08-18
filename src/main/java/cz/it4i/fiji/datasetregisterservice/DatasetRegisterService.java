/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datasetregisterservice;

import java.util.Collection;
import java.util.UUID;

public interface DatasetRegisterService {

	Collection<Dataset> getDatasest();

	Dataset getDataset(UUID uuid);

}
