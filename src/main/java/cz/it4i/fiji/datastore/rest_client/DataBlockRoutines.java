/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.ws.rs.core.Response;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

public final class DataBlockRoutines {

	private DataBlockRoutines() {
	}

	public static DataBlock<?> constructDataBlock(long[] gridPosition,
		Response response, DataType dataType) throws IOException
	{
		InputStream is = response.readEntity(InputStream.class);
		DataInputStream dis = new DataInputStream(is);
		int[] size = new int[3];
		for (int i = 0; i < 3; i++) {
			size[i] = dis.readInt();
			if (size[i] < 0) {
				return null;
			}
		}


		DataBlock<?> block = dataType.createDataBlock(size, gridPosition);
		byte[] buffer = new byte[block.getNumElements() * getSizeOfElement(
			dataType)];
		readFully(is, buffer);
		block.readData(ByteBuffer.wrap(buffer));
		return block;
	}

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
				throw new IllegalArgumentException("Datatype " + dataType +
					" not supported");
		}
	}

	private static int readFully(InputStream in, byte[] b) throws IOException {
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
