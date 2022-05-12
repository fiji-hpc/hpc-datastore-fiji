package cz.it4i.fiji.legacy;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.command.Command;
import org.scijava.log.LogService;

import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>BigDataViewer>Save XML (Datastore)")
public class BdvSaveXmlWithDatastore implements Command {
	@Parameter(label = "URL of a DatasetsRegisterService:", persistKey = "datasetserverurl")
	public String url = "someHostname:9080";

	@Parameter(label = "UUID of the dataset to be modified:", persistKey = "datasetdatasetid")
	public String datasetID = "someDatasetUUID";

	@Parameter(label = "Save as .xml:", style = "file,extensions:xml")
	public File outputXml;

	@Parameter
	public LogService logService;

	@Override
	public void run() {
		String queryUrl = "http://"+url+"/datasets/"+datasetID+"/all";
		logService.info("Polling URL: "+queryUrl);
		try (InputStream in = new URL(queryUrl).openStream();
		     OutputStream out = new FileOutputStream(outputXml)) {

			final byte[] buffer = new byte[8192];
			int size = in.read(buffer);
			while (size > 0) {
				out.write(buffer, 0, size);
				size = in.read(buffer);
			}
			out.flush();

			logService.info("Written file: "+outputXml);
		}
		catch (IOException e) {
			logService.error("Some connection problem while fetching XML: "+e.getMessage());
			logService.error("It is likely that UUID is wrong.");
			e.printStackTrace();
		}
	}
}
