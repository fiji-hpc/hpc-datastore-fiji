/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import cz.it4i.fiji.legacy.util.GuiBdvBrowseDialog;
import ij.IJ;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import java.io.IOException;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>BigDataViewer>Open in BDV (Datastore)")
public class BdvOpenViewerViaDatastore implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be modified:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	@Parameter
	public LogService logService;

	@Override
	public void run() {
		final String serverUrl = "http://"+url+"/datasets/"+datasetID;
		logService.info("Polling URL: "+serverUrl);

		try
		{
			new GuiBdvBrowseDialog().startBrowser(serverUrl);
		}
		catch ( final IOException e )
		{
			IJ.showMessage( "Error connecting to server at " + serverUrl );
			logService.error("Error browsing server "+serverUrl+": "+e.getMessage());
			e.printStackTrace();
		}
	}
}
