package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

//TODO add more components
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>Create ZARR>Create new dataset")
public class CreateNewDatasetZarr extends CreateNewDataset {
    @Override
    protected String getDatasetType()
    {
        return "Zarr";
    }

}