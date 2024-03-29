/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy.util;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public class Imglib2Types {

	final static List<TypeHandler<?>> SUPPORTEDCONVERSIONS = Arrays.asList(
			new TypeHandler<>(  "int8", new ByteType(),          BufferProcessors.BYTE_BUFFERS),
			new TypeHandler<>( "uint8", new UnsignedByteType(),  BufferProcessors.BYTE_BUFFERS),
			new TypeHandler<>( "int16", new ShortType(),         BufferProcessors.SHORT_BUFFERS),
			new TypeHandler<>("uint16", new UnsignedShortType(), BufferProcessors.SHORT_BUFFERS),
			new TypeHandler<>( "int32", new IntType(),           BufferProcessors.INT_BUFFERS),
			new TypeHandler<>("uint32", new UnsignedIntType(),   BufferProcessors.INT_BUFFERS),
			new TypeHandler<>( "int64", new LongType(),          BufferProcessors.LONG_BUFFERS),
			new TypeHandler<>("uint64", new UnsignedLongType(),  BufferProcessors.LONG_BUFFERS),
			new TypeHandler<>("float32", new FloatType(),        BufferProcessors.FLOAT_BUFFERS),
			new TypeHandler<>("float64", new DoubleType(),       BufferProcessors.DOUBLE_BUFFERS)
			//NB: types should match the org.janelia.saalfeldlab.n5.DataType enum
	);

	public static class TypeHandler<T extends NativeType<T> & RealType<T>> {
		public TypeHandler(final String typeName, final T typeType, final BufferProcessors.BufferProcessor bp) {
			httpType = typeName;
			nativeAndRealType = typeType;
			this.bp = bp;
		}

		final public String httpType;
		final public T nativeAndRealType;
		final BufferProcessors.BufferProcessor bp;

		public PlanarImgFactory<T> createPlanarImgFactory() {
			return new PlanarImgFactory<>(nativeAndRealType);
		}

		public void blockIntoImgInterval(final byte[] bytes, final int length, final IterableInterval<T> img) {
			final ByteBuffer blockData = ByteBuffer.wrap(bytes);
			final Cursor<T> imageData = img.cursor();
			int bl = 0;
			while (bl < length) {
				imageData.next().setReal( bp.get(blockData) );
				++bl;
			}
		}

		public void imgIntervalIntoBlock(final IterableInterval<T> img, final int length, final byte[] bytes) {
			final ByteBuffer blockData = ByteBuffer.wrap(bytes);
			final Cursor<T> imageData = img.cursor();
			int bl = 0;
			while (bl < length) {
				bp.put(blockData, imageData.next().getRealDouble() );
				++bl;
			}
		}
	}


	static public TypeHandler getTypeHandler(final String datastoreVoxelType)
	throws NoSuchElementException {
		return SUPPORTEDCONVERSIONS
				.stream().filter( t -> datastoreVoxelType.startsWith(t.httpType) ).findFirst()
				.get();
	}

	static public TypeHandler getTypeHandler(final Type<?> imglib2Type)
	throws NoSuchElementException {
		Class clazz = imglib2Type.getClass();
		return SUPPORTEDCONVERSIONS
				.stream().filter( t -> clazz.isInstance(t.nativeAndRealType) ).findFirst()
				.get();
	}
}
