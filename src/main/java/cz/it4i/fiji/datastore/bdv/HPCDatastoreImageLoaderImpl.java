/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.view.Views;

import org.janelia.saalfeldlab.n5.DataType;
import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import bdv.AbstractViewerSetupImgLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import bdv.img.remote.AffineTransform3DJsonSerializer;
import bdv.util.ConstantRandomAccessible;
import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoaderMetaData;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoaderPlugin;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;

@Plugin(type = HPCDatastoreImageLoaderPlugin.class)
public class HPCDatastoreImageLoaderImpl implements HPCDatastoreImageLoaderPlugin {

	protected String baseUrl;

	protected HPCDatastoreImageLoaderMetaData metadata;

	protected Map<ViewLevelId, int[]> cellsDimensions;

	protected VolatileGlobalCellCache cache;

	protected HPCDatastoreArrayLoader<?, ?> loader;

	/**
	 * TODO
	 */
	protected final Map<Integer, SetupImgLoader<?, ?>> setupImgLoaders;

	private AbstractSequenceDescription<?, ?, ?> sequenceDescription;

	private boolean isOpen = false;


	public HPCDatastoreImageLoaderImpl() {
		setupImgLoaders = new HashMap<>();
	}

	@Override
	public void init(final String aBaseUrl,
		AbstractSequenceDescription<?, ?, ?> aSequenceDescription, Element elem)
		throws SpimDataException
	{
		this.baseUrl = aBaseUrl;
		this.sequenceDescription = aSequenceDescription;
	}

	@Override
	public SetupImgLoader<?, ?> getSetupImgLoader(final int setupId) {
		tryopen();
		return setupImgLoaders.get(setupId);
	}

	@Override
	public VolatileGlobalCellCache getCacheControl() {
		tryopen();
		return cache;
	}

	private <T extends NativeType<T>, V extends Volatile<T> & NativeType<V>> void
		open() throws IOException
	{
		if (!isOpen) {
			synchronized (this) {
				if (isOpen) return;
				isOpen = true;
				
				final String datasetBaseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(
					'/',
					baseUrl.length() - 2));
				final String apiBaseUrl = datasetBaseUrl.substring(0, datasetBaseUrl
					.lastIndexOf('/', datasetBaseUrl.lastIndexOf('/') - 1) + 1);
				
				
				final URL url = new URL(datasetBaseUrl);
				final GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.registerTypeAdapter(AffineTransform3D.class,
					new AffineTransform3DJsonSerializer());
				DatasetDTO datasetDTO = gsonBuilder.create().fromJson(
					new InputStreamReader(url.openStream()), DatasetDTO.class);
				metadata = new HPCDatastoreImageLoaderMetaData(datasetDTO,
					sequenceDescription, DataType.fromString(datasetDTO
						.getVoxelType()));
				final DataTypeFactory<?, ?, T, V> factory = DataTypeFactory
					.getByDataType(metadata.getDataType());

				loader = new HPCDatastoreArrayLoader<>(factory.dataType,
					apiBaseUrl, metadata, sequenceDescription, factory.getProducer());
				cache = new VolatileGlobalCellCache(metadata.getMaxNumLevels(), 10);
				cellsDimensions = metadata.createCellsDimensions();
				for (final int setupId : metadata.getPerSetupMipmapInfo().keySet()) {
					setupImgLoaders.put(setupId, new SetupImgLoader<>(setupId, factory
						.getType(), factory
							.getVolatileType()));
				}
			}
		}
	}

	private void tryopen() {
		try {
			open();
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private MipmapInfo getMipmapInfo(final int setupId) {
		tryopen();
		return metadata.getPerSetupMipmapInfo().get(setupId);
	}

	/**
	 * Checks whether the given image data is present on the server.
	 *
	 * @return true, if the given image data is present.
	 */
	private boolean existsImageData(final ViewLevelId id) {
		return getDimsAndExistence(id).exists();
	}

	/**
	 * For images that are missing in the hdf5, a constant image is created. If
	 * the dimension of the missing image is known (see
	 * {@link #getDimsAndExistence(ViewLevelId)}) then use that. Otherwise create
	 * a 1x1x1 image.
	 */
	private <T> RandomAccessibleInterval<T> getMissingDataImage(
		final ViewLevelId id, final T constant)
	{
		final long[] d = getDimsAndExistence(id).getDimensions();
		return Views.interval(new ConstantRandomAccessible<>(constant, 3),
			new FinalInterval(d));
	}

	private DimsAndExistence getDimsAndExistence(final ViewLevelId id) {
		tryopen();
		return metadata.getDimsAndExistence().get(id);
	}


	/**
	 * Create a {@link VolatileCachedCellImg} backed by the cache. The
	 * {@code type} should be either {@link UnsignedShortType} and
	 * {@link VolatileUnsignedShortType}.
	 */
	private <T extends NativeType<T>> RandomAccessibleInterval<T>
		prepareCachedImage(final ViewLevelId id,
			final LoadingStrategy loadingStrategy, final T type)
	{
		tryopen();
		if (cache == null) throw new RuntimeException("no connection open");

//		final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
		if (!existsImageData(id)) {
			System.err.println(String.format(
				"image data for timepoint %d setup %d level %d could not be found.", id
					.getTimePointId(), id.getViewSetupId(), id.getLevel()));
			return getMissingDataImage(id, type);
		}

		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final MipmapInfo mipmapInfo = getMipmapInfo(setupId);

		final long[] dimensions = metadata.getDimsAndExistence().get(id)
			.getDimensions();
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[level];
		final CellGrid grid = new CellGrid(dimensions, cellDimensions);

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints(loadingStrategy, priority,
			false);
		return cache.createImg(grid, timepointId, setupId, level, cacheHints,
			loader, type);
	}

	private class SetupImgLoader<T extends NativeType<T>, V extends Volatile<T> & NativeType<V>>
		extends
		AbstractViewerSetupImgLoader<T, V>
	{

		private final int setupId;

		protected SetupImgLoader(final int setupId, T t, V v) {
			super(t, v);
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval<T> getImage(
			final int timepointId, final int level, final ImgLoaderHint... hints)
		{
			final ViewLevelId id = new ViewLevelId(timepointId, setupId, level);
			return prepareCachedImage(id, LoadingStrategy.BLOCKING, type);
		}

		@Override
		public RandomAccessibleInterval<V> getVolatileImage(
			final int timepointId, final int level, final ImgLoaderHint... hints)
		{
			final ViewLevelId id = new ViewLevelId(timepointId, setupId, level);
			return prepareCachedImage(id, LoadingStrategy.BUDGETED, volatileType);
		}

		@Override
		public double[][] getMipmapResolutions() {
			return getMipmapInfo(setupId).getResolutions();
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms() {
			return getMipmapInfo(setupId).getTransforms();
		}

		@Override
		public int numMipmapLevels() {
			return getMipmapInfo(setupId).getNumLevels();
		}
	}

}
