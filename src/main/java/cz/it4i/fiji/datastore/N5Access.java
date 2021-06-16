/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import bdv.img.n5.BdvN5Format;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.ViewSetup;


@Log4j2
public class N5Access {

	public static int getSizeOfElement(DataType dataType) {
		switch (dataType) {
			case UINT8:
			case INT8:
				return 1;
			case UINT16:
			case INT16:
				return 2;
			case UINT32:
			case INT32:
			case FLOAT32:
				return 4;
			case UINT64:
			case INT64:
			case FLOAT64:
				return 8;
			default:
				throw new IllegalArgumentException("Datatype " + dataType + " not supported");
		}
	}





	private N5Writer writer;
	private SpimData spimData;


	public static DataBlock<?> constructDataBlock(long[] gridPosition,
		InputStream inputStream, DataType dataType) throws IOException
	{
		DataInputStream dis = new DataInputStream(inputStream);
		int[] size = new int[3];
		for (int i = 0; i < 3; i++) {
			size[i] = dis.readInt();
		}

		DataBlock<?> block = dataType.createDataBlock(size, gridPosition);
		byte[] buffer = new byte[block.getNumElements() * getSizeOfElement(
			dataType)];
		readFully(inputStream, buffer);
		block.readData(ByteBuffer.wrap(buffer));
		return block;
	}



	public N5Access(Path pathToXML, N5Writer aWriter)
		throws SpimDataException
	{
		loadFromXML(pathToXML);
		writer = aWriter;
	}

	/**
	 * TODO: Exceptions indicating not existent, block, angle, time, channel
	 * 
	 * @param gridPosition
	 * @param time
	 * @param channel
	 * @param angle
	 * @param resolutionLevel
	 * @return readed ByteBuffer or null;
	 * @throws IOException
	 */
	public DataBlock<?> read(long[] gridPosition, int time, int channel,
		int angle,
		int[] resolutionLevel) throws IOException
	{
		String path = getPath(spimData, writer, time, channel, angle,
			resolutionLevel);
		if (path == null) {
			return null;
		}
		return writer.readBlock(path, writer.getDatasetAttributes(path),
			gridPosition);
	}

	public void write(long[] gridPosition, int time, int channel, int angle,
		int[] resolutionLevel, InputStream inputStream) throws IOException
	{
		String path = getPath(spimData, writer, time, channel, angle,
			resolutionLevel);
		DatasetAttributes attributes = writer.getDatasetAttributes(path);
		DataBlock<?> dataBlock = constructDataBlock(gridPosition, attributes,
			inputStream);
		checkBlockSize(dataBlock, attributes.getBlockSize());
		writer.writeBlock(path, attributes, dataBlock);
	}

	public DataType getType(int time, int channel, int angle,
		int[] resolutionLevel)
	{
		String path = getPath(spimData, writer, time, channel, angle,
			resolutionLevel);
		try {
			return writer.getDatasetAttributes(path).getDataType();
		}
		catch (IOException exc) {
			log.error("getType", exc);
			return null;
		}
	}

	private void loadFromXML(Path path) throws SpimDataException {
		spimData = new XmlIoSpimData().load(path.toString());
	}



	private void checkBlockSize(DataBlock<?> dataBlock, int[] blockSize) {
		for (int i = 0; i < blockSize.length; i++) {
			if (dataBlock.getSize()[i] < 0 || blockSize[i] < dataBlock.getSize()[i]) {
				throw new IllegalArgumentException(String.format(
					"Block dimension should be [%s] but is [%s]", getDimensionRange(0,
						blockSize), getDimension(dataBlock.getSize())));
			}
		}
	}



	private String getDimension(int[] size) {
		return IntStream.of(size).mapToObj(i -> "" + i).collect(Collectors.joining(
			","));
	}



	private String getDimensionRange(final int min, int[] size) {
		return IntStream.of(size).mapToObj(i -> min + "-" + i).collect(Collectors
			.joining(
			","));
	}



	private static DataBlock<?> constructDataBlock(long[] gridPosition,
		DatasetAttributes attributes, InputStream inputStream) throws IOException
	{
		DataType dataType = attributes.getDataType();

		return constructDataBlock(gridPosition, inputStream, dataType);
	}

	private static String getPath(SpimData spimData, N5Writer writer, int time,
		int channel, int angle, int[] resolutionLevel)
	{
		ViewSetup viewSetup = getViewSetup(spimData, channel, angle);
		if (viewSetup == null) {
			throw new IllegalArgumentException(String.format(
				"Channel=%d and angle=%d not found.", channel, angle));
		}
		return getPath(writer, time, viewSetup, resolutionLevel);
	}

	private static String getPath(N5Writer writer, int timeId,
		ViewSetup viewSetup, int[] resolutionLevel)
	{
		Integer levelId = getLevelId(writer, viewSetup, timeId, resolutionLevel);
		if (levelId == null) {
			return null;
		}
		return BdvN5Format.getPathName(viewSetup.getId(), timeId, levelId);
	}

	private static Integer getLevelId(N5Writer writer, ViewSetup viewSetup,
		int timId, int[] resolutionLevel)
	{
		String baseGroup = BdvN5Format.getPathName(viewSetup.getId(), timId);

		try {
			Pattern levelGroupPattern = Pattern.compile("s(\\p{Digit}+)");
			// @formatter:off			
			return Arrays.asList(writer.list(baseGroup))
														.stream().map(levelGroupPattern::matcher)
														.filter(Matcher::matches)
														.filter(m -> matchResolutionLevel(writer,baseGroup, m.group(), resolutionLevel))
														.map(m -> m.group(1))
														.findAny()
														.map(Integer::valueOf)
														.orElse(null);
		// @formatter:on
		}
		catch (IOException exc) {
			log.warn("Listing group :" + baseGroup, exc);
			return -1;
		}
	}

	private static boolean matchResolutionLevel(N5Writer writer, String baseGroup,
		String subGroup,
		int[] resolutionLevel)
	{
		return Arrays.equals(resolutionLevel, getAttribute(writer, baseGroup + "/" +
			subGroup, "downsamplingFactors", int[].class, () -> new int[] {}));
	}

	private static ViewSetup getViewSetup(SpimData spimData, int channel,
		long angle)
	{
		return spimData.getSequenceDescription().getViewSetupsOrdered().stream().filter(
			lvs -> lvs.getChannel().getId() == channel && lvs.getAngle()
				.getId() == angle).findFirst().orElse(null);
	}

	private static <T> T getAttribute(N5Writer writer, String pathName,
		String attrName,
		Class<T> clazz,
		Supplier<T> defaultResult)
	{
		try {
			return writer.getAttribute(pathName, attrName, clazz);
		}
		catch (IOException exc) {
			return defaultResult.get();
		}

	}

	private static int readFully(InputStream in, byte[] b)
			throws IOException
		{
			int n = 0;
			while (n < b.length) {
				int count = in.read(b, n, b.length - n);
				if (count < 0) {
					break;
				}
				n += count;
			}
			return n;
		}


}
