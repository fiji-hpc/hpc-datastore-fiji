/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.janelia.saalfeldlab.n5.DataBlock;


class DataBlockInputStream extends InputStream {

	private Collection<ByteBuffer> collection = new LinkedList<>();

	private Iterator<ByteBuffer> innerIterator;

	private ByteBuffer innerByteBuffer;

	@Override
	public int read() throws IOException {
		ByteBuffer bb = getByteBuffer();
		if (bb == null) {
			return -1;
		}
		return Byte.toUnsignedInt(bb.get());
	}

	private ByteBuffer getByteBuffer() {
		while (innerByteBuffer == null || !innerByteBuffer.hasRemaining()) {
			Iterator<ByteBuffer> iterator = getIterator();
			if (!iterator.hasNext()) {
				innerByteBuffer = null;
				break;
			}
			innerByteBuffer = iterator.next();
		}
		return innerByteBuffer;
	}

	private Iterator<ByteBuffer> getIterator() {
		if (innerIterator == null) {
			innerIterator = collection.iterator();
		}
		return innerIterator;
	}

	public void add(DataBlock<?> block) {
		if (innerIterator != null) {
			throw new IllegalStateException("Cannot add new datablock.");
		}
		collection.add(getByteBuffer(block.getSize()));
		collection.add(block.toByteBuffer());
	}

	private ByteBuffer getByteBuffer(int[] size) {
		ByteBuffer result = ByteBuffer.allocate(size.length * Integer.BYTES);
		result.asIntBuffer().put(size);
		return result;
	}

}
