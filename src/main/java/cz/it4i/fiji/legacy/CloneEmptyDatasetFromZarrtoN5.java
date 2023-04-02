package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Create>Create empty dataset from Zarr")
public class CloneEmptyDatasetFromZarrtoN5 implements Command{
    @Override
    public void run() {

    }
}
