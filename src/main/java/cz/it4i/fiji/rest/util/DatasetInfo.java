package cz.it4i.fiji.rest.util;

import java.net.URL;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatasetInfo {
	public String uuid;
	public String voxelType;

	//6D space as 3D+1+1+1
	public List<Integer> dimensions;
	public int timepoints;
	public int channels;
	public int angles;

	//sizes and units along the 6 axes
	public String voxelUnit;
	public List<Double> voxelResolution;
	public ResolutionWithOwnUnit timepointResolution;
	public ResolutionWithOwnUnit channelResolution;
	public ResolutionWithOwnUnit angleResolution;

	public String compression;

	public List<ResolutionLevel> resolutionLevels;
	public List<Integer> versions;
	public String label;

	/** a common label provider with default fallback response/label */
	public static final String NO_LABEL_MSG = "(no label is attached)";
	public String getLabel() {
		if (label == null || label.length() == 0) return NO_LABEL_MSG;
		return label;
	}

	public double[][] transformations;

	public double[][] viewRegistrations;

	public List<Integer> timepointIds;

	@Override
	public String toString() {
		return "UUID = " + uuid +
				"\nlabel = " + label +
				"\nvoxelType = " + voxelType +
				"\ndimensions,timepoints,channels,angles = " +
					dimensions + ", " + timepoints + "," + channels + "," + angles +
				"\nvoxelResolution = " + voxelResolution + " " + voxelUnit +
				"\ntimepointResolution = " + timepointResolution +
				"\nchannelResolution = " + channelResolution +
				"\nangleResolution = " + angleResolution +
				"\ncompression = " + compression +
				"\nresolutionLevels = [\n  " +// resolutionLevels +
					resolutionLevels.stream().map(l -> l.toString()+"\n  ").collect(Collectors.joining()) +
				"]\nversions = " + versions;
	}


	// ========= compound-types corresponding to compound-elements in the JSON messages =========
	public static class ResolutionWithOwnUnit {
		public ResolutionWithOwnUnit() {
			//2nd arg intentionally as a String
			this(0.0, "null");
		}
		public ResolutionWithOwnUnit(final double value, final String unit) {
			this.value = value;
			this.unit = unit;
		}

		public double value;
		public String unit;

		@Override
		public String toString() {
			return value + " " + (unit != null ? unit : "null");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || o.getClass() != ResolutionWithOwnUnit.class) return false;
			final ResolutionWithOwnUnit that = (ResolutionWithOwnUnit)o;
			return this.value == that.value && this.unit.equals(that.unit);
		}
		@Override
		public int hashCode() {
			return (int)(value*unit.hashCode());
		}
	}

	public static class ResolutionLevel {
		public ResolutionLevel() {
			//intentionally invalid values!
			this(-1,-1,-1, -1,-1,-1);
		}
		public ResolutionLevel(final int res_x,   final int res_y,   final int res_z,
		                       final int block_x, final int block_y, final int block_z) {
			resolutions = Arrays.asList(res_x, res_y, res_z);
			blockDimensions = Arrays.asList(block_x, block_y, block_z);
		}

		public List<Integer> resolutions;
		public List<Integer> blockDimensions;
		public int[] dimensions = new int[3];

		public void setDimensions(final List<Integer> fullResDims) {
			for (int d = 0; d < 3; ++d)
				dimensions[d] = (int)Math.floor( (double)fullResDims.get(d) / (double)resolutions.get(d) );
		}

		@Override
		public String toString() {
			return "down-level " + resolutions
					+ " (-> img dims ["+dimensions[0]+","+dimensions[1]+","+dimensions[2]+"])"
					+ " with blocks " + blockDimensions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || o.getClass() != ResolutionLevel.class) return false;
			final ResolutionLevel that = (ResolutionLevel)o;
			return this.resolutions.equals(that.resolutions)
					&& this.blockDimensions.equals(that.blockDimensions);
		}
		@Override
		public int hashCode() {
			return resolutions.hashCode()-blockDimensions.hashCode();
		}
	}


	// ========= constructing... =========
	/** default c'tor for testing only */
	public DatasetInfo() {
		voxelType = "uint64";
		dimensions = Arrays.asList(100,100,20);
		timepoints = 2;
		channels = 1;
		angles = 1;

		voxelUnit = "microns";
		voxelResolution = Arrays.asList(0.2,0.2,0.6);

		timepointResolution = new ResolutionWithOwnUnit(90, "second");
		channelResolution = new ResolutionWithOwnUnit(1, "band");
		angleResolution = new ResolutionWithOwnUnit();

		compression = "raw";

		resolutionLevels = new ArrayList<>();
		resolutionLevels.add( new ResolutionLevel(1,1,1, 64,64,32) );
		resolutionLevels.add( new ResolutionLevel() );
		versions = Collections.emptyList();
	}

	public static DatasetInfo createFrom(final String hostnameURL, final String datasetID)
	throws IOException
	{
		final ObjectMapper om = new ObjectMapper();
		final DatasetInfo di = om.readValue(new URL("http://"+hostnameURL+"/datasets/"+datasetID), DatasetInfo.class);
		//finish what ObjectMapper couldn't do...
		for (ResolutionLevel l : di.resolutionLevels)
			l.setDimensions(di.dimensions);
		return di;
	}
}
