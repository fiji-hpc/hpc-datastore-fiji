/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.img.n5.BdvN5Format;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.ViewSetup;

public class DatasetServerImpl<T> implements Closeable {

	private final SpimData data;
	private final N5Reader reader;

	public DatasetServerImpl(String path) throws SpimDataException, IOException {
		final XmlIoSpimData io = new XmlIoSpimData();
		data = io.load(path);
		reader = new N5FSReader(path.replaceAll("\\.xml$", ".n5"));
	}

	public DataBlock<T> read(long x, long y, long z, int time, int channel,
		long angle) throws IOException
	{
		String path = getPath(time, channel, angle);
		@SuppressWarnings("unchecked")
		DataBlock<T> result = (DataBlock<T>) reader.readBlock(path, reader
			.getDatasetAttributes(path), new long[] { x, y, z });
		return result;
	}

	@Override
	public void close() {

	}

	private String getPath(int time, int channel, long angle) {
		
		ViewSetup vs = data.getSequenceDescription().getViewSetupsOrdered().stream()
			.filter(lvs -> lvs.getAngle().getId() == time && lvs.getChannel()
				.getId() == channel)
			.findFirst().orElse(null);
		return BdvN5Format.getPathName(vs.getId(), time, 0);
	}

}
