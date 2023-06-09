/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import bdv.img.hdf5.MipmapInfo;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import bdv.img.cache.CacheArrayLoader;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoaderMetaData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;

class HPCDatastoreArrayLoader<T, A> implements CacheArrayLoader<T>
{

	private final AbstractSequenceDescription<?, ?, ?> sequenceDescription;
	private final ServerPool<A> serverPool;
	private final DataType dataType;
	private final Function<A, T> producer;
	private final int bytesPerElement;
	private final Map<Integer, MipmapInfo> setupToMipmap;

	public HPCDatastoreArrayLoader(DataType dataType, final String baseUrl,
		HPCDatastoreImageLoaderMetaData metadata,
		AbstractSequenceDescription<?, ?, ?> sequenceDescription,
		Function<A, T> producer)
	{
		this.dataType = dataType;
		this.serverPool = new ServerPool<>(baseUrl, metadata);
		this.sequenceDescription = sequenceDescription;
		this.producer = producer;
		this.bytesPerElement = getBytesPerElement(dataType);
		this.setupToMipmap = metadata.getPerSetupMipmapInfo();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T loadArray(final int timepoint, final int setup,
		final int level, final int[] dimensions, final long[] min)
		throws InterruptedException
	{
		final long[] coords = getCellGridCoords(setupToMipmap.get(setup).getSubdivisions()[level], min);
		final ViewSetupValues setupValues = ViewSetupValues.construct(setup,
			sequenceDescription);
		final DatasetServer<A> server = serverPool.getServerClient(
			setupValues, level);
		DataBlock<A> block;
		try {
			block = server.readBlock(coords, timepoint, setupValues);
			if (block == null) {
				block = (DataBlock<A>) dataType.createDataBlock(dimensions, coords);
			}
		}
		catch (IOException exc) {
			throw new RuntimeException(exc);
		}


		return producer.apply(block.getData());
	}

	private long[] getCellGridCoords(int[] dimensions, long[] min) {

		long[] result = new long[dimensions.length];
		for (int i = 0; i < min.length; i++) {
			result[i] = min[i] / dimensions[i];
		}
		return result;
	}

	@Override
	public int getBytesPerElement() {
		return bytesPerElement;
	}

	private static int getBytesPerElement(DataType type) {
		return type.createDataBlock(new int[] { 1 }, new long[] { 0 })
			.toByteBuffer()
			.capacity();
	}
}
