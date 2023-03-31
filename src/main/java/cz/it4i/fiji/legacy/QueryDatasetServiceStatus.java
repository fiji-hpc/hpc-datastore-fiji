/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.LogService;
import cz.it4i.fiji.datastore.service.DataStoreService;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Query>DatasetService cache")
public class QueryDatasetServiceStatus implements Command {
	@Parameter
	public LogService mainLogger;

	@Parameter
	public DataStoreService dataStoreService;

	@Override
	public void run() {
		mainLogger.info( dataStoreService );
	}
}
