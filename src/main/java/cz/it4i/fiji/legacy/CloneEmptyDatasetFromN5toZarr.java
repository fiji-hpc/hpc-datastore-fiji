package cz.it4i.fiji.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.it4i.fiji.legacy.util.GuiResolutionLevelParams;
import cz.it4i.fiji.rest.util.DatasetInfo;
import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.prefs.PrefService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>HPC DataStore>ZARR Convert>Create new dataset from existing N5")

public class CloneEmptyDatasetFromN5toZarr implements Command {
    @Parameter(label = "URL of a reference DatasetsRegisterService:", persistKey = "datasetserverurl")
    public String src_url = "someHostname:9080";

    @Parameter(label = "UUID of a reference N5 dataset on that service:", persistKey = "datasetdatasetid")
    public String src_uuid = "someDatasetUUID";

    @Parameter(label = "URL of a target DatasetsRegisterService:", persistKey = "datasetserverurl")
    public String tgt_url = "someHostname:9080";

    @Parameter(label = "Modify params before creating the new dataset:")
    public boolean shallUpdateParams = false;

    @Parameter
    public PrefService prefs;

    @Parameter
    public CommandService cs;
    @Parameter
    public PluginService ps;

    @Parameter
    public LogService mainLogger;
    protected Logger myLogger;

    @Parameter(label = "Report corresponding macro command:", required = false)
    public boolean showRunCmd = false;

    @Parameter(type = ItemIO.OUTPUT, label="UUID of the created Zarr dataset:")
    public String newDatasetUUID;
    @Parameter(type = ItemIO.OUTPUT, label="Label of the created Zarr  dataset:")
    public String newDatasetLabel;
    @Override
    public void run() {
        //logging facility
        myLogger = mainLogger.subLogger("HPC CloneDataset", LogLevel.INFO);

        try {
            //retrieve reference data
            myLogger.info("Reading "+src_uuid+" from "+src_url);
            DatasetInfo di = DatasetInfo.createFrom(src_url, src_uuid);
            myLogger.info(di.toString());

            if (!shallUpdateParams) {
                final String json = new ObjectMapper().writeValueAsString(di);
                myLogger.info("CREATED JSON:\n"+json);

                final Future<CommandModule> subcall = cs.run(CreateNewDatasetFromJSON.class, true,
                        "url",tgt_url, "json",json, "showRunCmd",showRunCmd);
                newDatasetUUID = (String)subcall.get().getOutput("newDatasetUUID");
                newDatasetLabel = (String)subcall.get().getOutput("newDatasetLabel");
            } else {
                //we gonna preset the prefs store so that the "params of resolution level" dialog(s)
                //will get preloaded with the reference values
                final GuiResolutionLevelParams prefsHandler = new GuiResolutionLevelParams();
                prefsHandler.prefs = prefs;
                for (int level = 0; level < di.resolutionLevels.size(); ++level) {
                    final DatasetInfo.ResolutionLevel l = di.resolutionLevels.get(level);
                    prefsHandler.resLevelNumber=level+1;
                    prefsHandler.down_x = l.resolutions.get(0);
                    prefsHandler.down_y = l.resolutions.get(1);
                    prefsHandler.down_z = l.resolutions.get(2);
                    prefsHandler.block_x = l.blockDimensions.get(0);
                    prefsHandler.block_y = l.blockDimensions.get(1);
                    prefsHandler.block_z = l.blockDimensions.get(2);
                    prefsHandler.storeIntoPrefs();
                }

                final CommandModule cm = new CommandModule( new CommandInfo(CreateNewDataset.class) );
                cm.setInput("datasetType","Zarr");
                cm.setInput("url", tgt_url);
                cm.setInput("label", di.label);
                cm.setInput("voxelType", di.voxelType);
                cm.setInput("fullResSizeX", di.dimensions.get(0));
                cm.setInput("fullResSizeY", di.dimensions.get(1));
                cm.setInput("fullResSizeZ", di.dimensions.get(2));
                cm.setInput("timepoints", di.timepoints);
                cm.setInput("channels", di.channels);
                cm.setInput("angles", di.angles);
                cm.setInput("res_x", di.voxelResolution.get(0));
                cm.setInput("res_y", di.voxelResolution.get(1));
                cm.setInput("res_z", di.voxelResolution.get(2));
                cm.setInput("res_unit", di.voxelUnit);

                if (di.timepointResolution != null) {
                    cm.setInput("time_res", di.timepointResolution.value);
                    cm.setInput("time_unit", di.timepointResolution.unit);
                }
                if (di.channelResolution != null) {
                    cm.setInput("channel_res", di.channelResolution.value);
                    cm.setInput("channel_unit", di.channelResolution.unit);
                }
                if (di.angleResolution != null) {
                    cm.setInput("angle_res", di.angleResolution.value);
                    cm.setInput("angle_unit", di.angleResolution.unit);
                }
                cm.setInput("numberOfAllResLevels", di.resolutionLevels.size());
                cm.setInput("compression", di.compression);
                cm.setInput("showRunCmd", showRunCmd);

                // 1) this is a standard list of pre- and post-processors,
                //    this is how ModuleService.java creates them for every call of run()
                final List<PreprocessorPlugin> pre  = ps.createInstancesOfType(PreprocessorPlugin.class);
                final List<PostprocessorPlugin> post = ps.createInstancesOfType(PostprocessorPlugin.class);

                // 2) but we find and remove one particular service...
                for (int i = 0; i < pre.size(); ++i)
                    if (pre.get(i).getClass().getSimpleName().startsWith("LoadInputsPreprocessor")) {
                        pre.remove(i);
                        break;
                    }

                // 3) finally, the command (module actually) is started
                final Future<Module> subcall = cs.moduleService().run(cm,pre,post);

                newDatasetUUID = (String)subcall.get().getOutput("newDatasetUUID");
                newDatasetLabel = (String)subcall.get().getOutput("newDatasetLabel");
            }
        } catch (IOException | ModuleException e) {
            myLogger.error("Some error accessing the reference service and dataset: "+e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            //dialog interrupted, do nothing special...
            myLogger.error("eee!? "+e.getMessage());
        }
    }
}
