package cz.it4i.fiji.legacy;

import cz.it4i.fiji.datastore.ij.ExportSPIMAsN5PlugIn;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>HPC DataStore>Create>Create new dataset from XML/HDF5")
public class CreateNewDatasetFromHDF5 extends ExportSPIMAsN5PlugIn { }
