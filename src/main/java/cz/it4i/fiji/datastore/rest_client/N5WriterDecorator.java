/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class N5WriterDecorator implements N5Writer {

	private final N5Writer writer;

	@Override
	public <T> void setAttribute(String pathName, String key, T attribute)
		throws IOException
	{
		writer.setAttribute(pathName, key, attribute);
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes)
		throws IOException
	{
		writer.setAttributes(pathName, attributes);
	}

	@Override
	public void setDatasetAttributes(String pathName,
		DatasetAttributes datasetAttributes) throws IOException
	{
		writer.setDatasetAttributes(pathName, datasetAttributes);
	}

	@Override
	public void createGroup(String pathName) throws IOException {
		writer.createGroup(pathName);
	}

	@Override
	public boolean remove(String pathName) throws IOException {
		return writer.remove(pathName);
	}

	@Override
	public boolean remove() throws IOException {
		return writer.remove();
	}

	@Override
	public void createDataset(String pathName,
		DatasetAttributes datasetAttributes) throws IOException
	{
		writer.createDataset(pathName, datasetAttributes);
	}

	@Override
	public void createDataset(String pathName, long[] dimensions, int[] blockSize,
		DataType dataType, Compression compression) throws IOException
	{
		writer.createDataset(pathName, dimensions, blockSize, dataType,
			compression);
	}

	@Override
	public Version getVersion() throws IOException {
		return writer.getVersion();
	}

	@Override
	public <T> void writeBlock(String pathName,
		DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
		throws IOException
	{
		writer.writeBlock(pathName, datasetAttributes, dataBlock);
	}

	@Override
	public boolean deleteBlock(String pathName, long... gridPosition)
		throws IOException
	{
		return writer.deleteBlock(pathName, gridPosition);
	}

	@Override
	public <T> T getAttribute(String pathName, String key, Class<T> clazz)
		throws IOException
	{
		return writer.getAttribute(pathName, key, clazz);
	}

	@Override
	public <T> T getAttribute(String pathName, String key, Type type)
		throws IOException
	{
		return writer.getAttribute(pathName, key, type);
	}

	@Override
	public DatasetAttributes getDatasetAttributes(String pathName)
		throws IOException
	{
		return writer.getDatasetAttributes(pathName);
	}

	@Override
	public DataBlock<?> readBlock(String pathName,
		DatasetAttributes datasetAttributes, long... gridPosition)
		throws IOException
	{
		DataBlock<?> result = writer.readBlock(pathName, datasetAttributes,
			gridPosition);
		if (result == null) {
			log.trace("cell[{}] has no data", (Supplier<String>) () -> Arrays.stream(
				gridPosition).mapToObj(val -> Long.toString(val)).collect(Collectors
					.joining(", ")));
			result = datasetAttributes.getDataType().createDataBlock(datasetAttributes
				.getBlockSize(), gridPosition);
		}
		return result;
	}

	@Override
	public boolean exists(String pathName) {
		return writer.exists(pathName);
	}

	@Override
	public boolean datasetExists(String pathName) throws IOException {
		return writer.datasetExists(pathName);
	}

	@Override
	public String[] list(String pathName) throws IOException {
		return writer.list(pathName);
	}

	@Override
	public Map<String, Class<?>> listAttributes(String pathName)
		throws IOException
	{
		return writer.listAttributes(pathName);
	}



}
