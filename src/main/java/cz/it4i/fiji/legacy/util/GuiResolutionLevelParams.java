package cz.it4i.fiji.legacy.util;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

@Plugin(type = Command.class, headless = true, name = "Resolution level parameters dialog")
public class GuiResolutionLevelParams implements Command {
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	public String headerMessage;

	@Parameter(label = "Down-size factor in x:", min = "1", persist = false, initializer = "setFromPrefs")
	public int down_x;
	@Parameter(label = "Down-size factor in y:", min = "1", persist = false)
	public int down_y;
	@Parameter(label = "Down-size factor in z:", min = "1", persist = false)
	public int down_z;

	@Parameter(label = "Block size in pixels in x:", min = "1", persist = false, callback = "reportBlockSize")
	public int block_x;
	@Parameter(label = "Block size in pixels in y:", min = "1", persist = false, callback = "reportBlockSize")
	public int block_y;
	@Parameter(label = "Block size in pixels in z:", min = "1", persist = false, callback = "reportBlockSize")
	public int block_z;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	public String sizeMessage;
	//
	@Parameter(persist = false)
	public int pxSizeInBytes = 2;

	@Parameter(persist = false)
	public int resLevelNumber = 999;

	@Parameter
	public PrefService prefs;

	public void reportBlockSize() {
		sizeMessage = "Ideal block size is just below 1024 kB, current is "
				+(block_x*block_y*block_z*pxSizeInBytes/1024)+" kB.";
	}

	public void setFromPrefs() {
		if (resLevelNumber == 1) {
			headerMessage = "Provide blocks size for the full resolution (base) level:";
			//NB: down_xyz values are provided from the caller for this special level
		} else {
			headerMessage = "Provide parameters for the resolution level "+resLevelNumber+":";
			down_x = prefs.getInt(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_down_x", 2);
			down_y = prefs.getInt(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_down_y", 2);
			down_z = prefs.getInt(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_down_z", 2);
		}

		block_x = prefs.getInt(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_block_x", 64);
		block_y = prefs.getInt(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_block_y", 64);
		block_z = prefs.getInt(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_block_z", 64);
		reportBlockSize();
	}

	public void storeIntoPrefs() {
		prefs.put(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_down_x", down_x);
		prefs.put(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_down_y", down_y);
		prefs.put(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_down_z", down_z);
		prefs.put(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_block_x", block_x);
		prefs.put(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_block_y", block_y);
		prefs.put(GuiResolutionLevelParams.class, "level"+resLevelNumber+"_block_z", block_z);
	}

	@Override
	public void run() {
		userClickedOK = true;
		storeIntoPrefs();
	}

	public boolean userClickedOK = false;
}
