package cz.it4i.fiji.datastore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.it4i.fiji.rest.util.DatasetInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetInfoTest {
	@Test
	public void WriteAndReadJSON() {
		//test print out
		final DatasetInfo dSrc = new DatasetInfo();
		System.out.println("=PLAIN=======================");
		System.out.println(dSrc);

		try {
			//convert to JSON...
			final ObjectMapper om = new ObjectMapper();
			final String json = om.writeValueAsString(dSrc);

			System.out.println("=JSON========================");
			System.out.println(json);
			System.out.println("=============================");

			//...and back
			final DatasetInfo dTgt = om.readValue(json, DatasetInfo.class);

			assertEquals(dSrc.voxelType,dTgt.voxelType);
			assertEquals(dSrc.timepoints,dTgt.timepoints);
			assertEquals(dSrc.channels,dTgt.channels);
			assertEquals(dSrc.angles,dTgt.angles);
			assertEquals(dSrc.voxelUnit,dTgt.voxelUnit);
			assertEquals(dSrc.voxelResolution,dTgt.voxelResolution);
			assertEquals(dSrc.timepointResolution,dTgt.timepointResolution);
			assertEquals(dSrc.channelResolution,dTgt.channelResolution);
			assertEquals(dSrc.angleResolution,dTgt.angleResolution);
			assertEquals(dSrc.compression,dTgt.compression);
			assertEquals(dSrc.resolutionLevels,dTgt.resolutionLevels);
			assertEquals(dSrc.versions,dTgt.versions);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void ExampleCreateDatasetInfo() {
		final DatasetInfo di = new DatasetInfo();
		di.voxelType = "uint64";
		di.dimensions = Arrays.asList(100,100,20);
		di.timepoints = 2;
		di.channels = 1;
		di.angles = 1;

		di.voxelUnit = "microns";
		di.voxelResolution = Arrays.asList(0.2,0.2,0.6);

		di.timepointResolution = new DatasetInfo.ResolutionWithOwnUnit(90, "second");
		di.channelResolution = new DatasetInfo.ResolutionWithOwnUnit(1, "band");
		di.angleResolution = new DatasetInfo.ResolutionWithOwnUnit();

		di.compression = "raw";

		di.resolutionLevels = new ArrayList<>();
		di.resolutionLevels.add( new DatasetInfo.ResolutionLevel(1,1,1, 64,64,32) );
		di.resolutionLevels.add( new DatasetInfo.ResolutionLevel() );
		di.versions = Collections.emptyList();

		System.out.println(di);
	}
}