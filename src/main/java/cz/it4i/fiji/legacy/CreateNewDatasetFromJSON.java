/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.it4i.fiji.rest.util.DatasetInfo;
import ij.plugin.frame.Recorder;
import org.scijava.ItemIO;
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
import java.net.MalformedURLException;
import java.net.URL;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Create>Create new dataset from JSON")
public class CreateNewDatasetFromJSON implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "Specification in JSON:", persist = false)
	public String json;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	@Parameter(label = "Report corresponding macro command:", required = false)
	public boolean showRunCmd = false;

	@Parameter(type = ItemIO.OUTPUT, label="UUID of the created dataset:")
	public String newDatasetUUID;
	@Parameter(type = ItemIO.OUTPUT, label="Label of the created dataset:")
	public String newDatasetLabel;

	@Override
	public void run() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC CreateDataset", LogLevel.INFO);

		try {
			final HttpURLConnection connection = (HttpURLConnection)new URL("http://"+this.url+"/datasets").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","application/json");
			connection.setDoOutput(true);
			connection.connect();
			connection.getOutputStream().write(json.getBytes());

			newDatasetUUID = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
			newDatasetLabel = new ObjectMapper().readValue(json, DatasetInfo.class).getLabel();

			if (showRunCmd) {
				final String howToRun = "run(\"Create new dataset from JSON\", 'url="+this.url
					+ " json=" + this.json + " showruncmd=False');";
				myLogger.info(howToRun);
				Recorder.recordString(howToRun);
			}
			mainLogger.info("Created dataset UUID: " + newDatasetUUID);
		} catch (MalformedURLException e) {
			myLogger.error("Malformed URL, probably because of \"http://\" prefix. Please use only hostname:port without any spaces.");
			myLogger.error(e.getMessage());
		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}