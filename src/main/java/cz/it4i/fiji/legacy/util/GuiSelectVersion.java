/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy.util;

import cz.it4i.fiji.rest.util.DatasetInfo;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, headless = true, name = "Choose dataset version dialog")
public class GuiSelectVersion extends DynamicCommand {
	@Parameter(label = "URL of a DatasetsRegisterService:")
	public String url;

	@Parameter(label = "UUID of a dataset on that service:")
	public String datasetID;

	@Parameter(label = "Choose from available versions:", choices = {}, initializer = "readVersions")
	public String version;

	@Parameter
	public LogService mainLogger;
	protected Logger myLogger;

	void readVersions() {
		//logging facility
		myLogger = mainLogger.subLogger("HPC DeleteDataset", LogLevel.INFO);

		try {
			final DatasetInfo di = DatasetInfo.createFrom(url, datasetID);
			final List<String> versions = di.versions.stream().map(Object::toString).collect(Collectors.toList());
			getInfo().getMutableInput("version",String.class).setChoices(versions);
		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
			this.cancel("Connection problem");
		}
	}

	@Override
	public void run() {
		try {
			myLogger.info("Deleting version "+version+" from dataset "+datasetID+" at "+url);
			final HttpURLConnection connection = (HttpURLConnection)new URL("http://"+url+"/datasets/"+datasetID+"/"+version+"/delete").openConnection();
			connection.getInputStream(); //the communication happens only after this command
			myLogger.info("Server responded: "+connection.getResponseMessage());
		} catch (IOException e) {
			myLogger.error("Some connection problem:");
			e.printStackTrace();
		}
	}
}