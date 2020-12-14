/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import lombok.Getter;
import mpicbg.spim.data.SpimDataException;

@Default
@RequestScoped
public class DatasetServerImpl implements Closeable {


	public static class WritedData {

		@Getter
		private DataInput inputStream;

		private byte[] buffer;

		public WritedData(InputStream inputStream) {
			this.inputStream = new DataInputStream(inputStream);
		}

		public ByteBuffer read(int bytes) throws IOException {
			if (buffer == null || buffer.length < bytes) {
				buffer = new byte[bytes];
			}
			inputStream.readFully(buffer, 0, bytes);
			return ByteBuffer.wrap(buffer, 0, bytes);
		}
	}

	private N5Access n5Access;

	@Inject
	private ApplicationConfiguration configuration;


	@PostConstruct
	private void init() throws SpimDataException, IOException {
		// TODO
		// n5Access = N5Access.loadExisting(configuration.getDatasetPath());
	}

	@Override
	public void close() {

	}

	public ByteBuffer read(long[] gridPosition, int time, int channel, int angle,
		int[] resolutionLevel) throws IOException
	{
		return n5Access.read(gridPosition, time, channel, angle, resolutionLevel);
	}

	public void write(long[] gridPosition, int time, int channel, int angle,
		int[] resolutionLevel, WritedData data) throws IOException
	{
		N5Access.DatasetLocation4Writing datasetLocation4Writing = n5Access
			.getDatasetLocation4Writing(time, channel, angle, resolutionLevel);
		n5Access.write(gridPosition, datasetLocation4Writing, data.read(
			datasetLocation4Writing.getNumOfBytes()));
	}


}
