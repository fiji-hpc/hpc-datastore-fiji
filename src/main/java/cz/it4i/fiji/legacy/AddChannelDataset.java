/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Modify>Add channel to dataset")
public class AddChannelDataset implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be modified:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	//@Parameter(label = "Number of channels being added:", min = "1", stepSize = "1")
	//public int noOfNewChannels = 1;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC ModifyDataset", LogLevel.INFO);

		try {
			//myLogger.info("Adding "+noOfNewChannels+" channel(s) into "+datasetID+" at "+url);
			myLogger.info("Adding "+1+" channel(s) into "+datasetID+" at "+url);
			final HttpURLConnection connection = (HttpURLConnection)new URL("http://"+url+"/datasets/"+datasetID+"/channels").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","text/plain");
			connection.setDoOutput(false);
			//connection.setDoOutput(true);
			connection.connect();
			//connection.getOutputStream().write(noOfNewChannels);
			myLogger.info("Server responded: "+connection.getResponseMessage());
		}
		catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}
