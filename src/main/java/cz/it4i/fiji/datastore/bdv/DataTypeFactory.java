/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.imglib2.Volatile;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

import org.janelia.saalfeldlab.n5.DataType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class DataTypeFactory<A, AO, T extends NativeType<T>, V extends Volatile<T> & NativeType<V>> {

	private final static Map<DataType, DataTypeFactory<?, ?, ?, ?>> factories =
		new HashMap<>();

	public static <A, A0, T extends NativeType<T>, V extends Volatile<T> & NativeType<V>>
		DataTypeFactory<A, A0, T, V> getByDataType(DataType dataType)
	{
		@SuppressWarnings("unchecked")
		DataTypeFactory<A, A0, T, V> result =
			(DataTypeFactory<A, A0, T, V>) factories.get(dataType);
		return result;
	}

	static {
		register(new DataTypeFactory<>(DataType.INT16, (
			short[] a) -> new VolatileShortArray(a, true), new ShortType(),
			new VolatileShortType()));
		register(new DataTypeFactory<>(DataType.UINT16, (
			short[] a) -> new VolatileShortArray(a, true), new UnsignedShortType(),
			new VolatileUnsignedShortType()));
	}

	private static void register(DataTypeFactory<?, ?, ?, ?> dataTypeFactory) {
		factories.put(dataTypeFactory.getDataType(), dataTypeFactory);
	}

	final DataType dataType;

	final Function<A, AO> producer;

	final T type;

	final V volatileType;

}
