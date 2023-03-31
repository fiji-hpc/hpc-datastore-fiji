/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import cz.it4i.fiji.datastore.service.DataStoreRequest;
import cz.it4i.fiji.datastore.service.DataStoreService;
import cz.it4i.fiji.rest.util.DatasetInfo;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.io.IOException;
import java.util.List;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Modify>Add new version to dataset")
public class AddVersionDataset implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be modified:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	@Parameter(type = ItemIO.OUTPUT)
	public int newlyAddedVersion = 0;

	@Parameter(type = ItemIO.OUTPUT)
	public String universalReferenceHint = "Try the 'latest' keyword instead of a particular version in scripts...";

	@Parameter
	public DataStoreService dataStoreService;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC ModifyDataset", LogLevel.INFO);

		try {
			myLogger.info("Quering "+datasetID+" from "+url);
			DatasetInfo di = DatasetInfo.createFrom(url, datasetID);
			myLogger.info(di);
			final int previouslyLastVer = di.versions.stream().reduce(Integer::max).get();

			List<Integer> resolutions = di.resolutionLevels.get(0).resolutions;
			DataStoreRequest request = new DataStoreRequest(url,datasetID,
					resolutions.get(0), resolutions.get(1), resolutions.get(2),
					"new","write",5000);

			dataStoreService.getActiveServingUrl(request);
			//NB: if we got here, the new version must have been created

			/*
			//ask again to see which one has been added
			DatasetInfo diNV = DatasetInfo.createFrom(url, datasetID);
			newlyAddedVersion = diNV.versions.stream().reduce(Integer::max).get();

			myLogger.info("Earlier versions: "+di.versions);
			myLogger.info("Current versions: "+diNV.versions);
			myLogger.info("Old latest version: "+previouslyLastVer);
			myLogger.info("Newly added version: "+newlyAddedVersion);
			*/
			newlyAddedVersion = previouslyLastVer+1;
		}
		catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}
