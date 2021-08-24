package cz.it4i.fiji.datastore;

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
}
