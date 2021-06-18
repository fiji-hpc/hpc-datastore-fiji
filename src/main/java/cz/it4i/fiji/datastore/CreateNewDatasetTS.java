/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetPathRoutines.getXMLPath;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.list.AbstractListImg;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.export.ProgressWriter;
import bdv.export.n5.WriteSequenceToN5;
import bdv.img.n5.BdvN5Format;
import bdv.img.n5.N5ImageLoader;
import lombok.Builder;
import lombok.NonNull;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;


public class CreateNewDatasetTS {

	public void run(Path path, N5Description dsc) throws IOException,
		SpimDataException
	{
		Path pathToXML = getXMLPath(path, DatasetFilesystemHandler.INITIAL_VERSION);

		Path pathToDir = DatasetPathRoutines.getDataPath(pathToXML);
		SpimData data = createNew(pathToDir, dsc.voxelType, dsc.dimensions,
			dsc.voxelDimensions, dsc.timepoints, dsc.channels, dsc.angles,
			dsc.compression, dsc.mipmapInfo);
		new XmlIoSpimData().save(data, pathToXML.toString());
	}

	private SpimData createNew(Path pathToDir, DataType voxelType,
		long[] dimensions, VoxelDimensions voxelDimensions, int timepoints,
		int channels, int angles, Compression compression,
		ExportMipmapInfo mipmapInfo) throws IOException
	{

		final Collection<TimePoint> timepointsCol = IntStream.range(0, timepoints)
			.<TimePoint> mapToObj(TimePoint::new).collect(Collectors.toList());

		Collection<ViewSetup> viewSetups = generateViewSetups(dimensions,
			voxelDimensions, channels, angles);

		Map<Integer, ExportMipmapInfo> exportMipmapInfo =
			assignMipmapInfoToViewSetups(viewSetups, mipmapInfo);
		SequenceDescription sequenceDescription = new SequenceDescription(
			new TimePoints(timepointsCol), viewSetups, new ImgLoaderImpl<>(N5Utils
				.type(voxelType), dimensions));
		WriteSequenceToN5.writeN5File(sequenceDescription, exportMipmapInfo,
			compression, pathToDir.toFile(), new LoopbackHeuristicAdapter(),
			new AfterEachPlaneAdapter(), 1, new ProgressWriterAdapter());
		sequenceDescription = new SequenceDescription(new TimePoints(timepointsCol),
			viewSetups, new N5ImageLoader(pathToDir.toFile(), sequenceDescription));
		SpimData result = new SpimData(pathToDir.toFile(), sequenceDescription,
			new ViewRegistrations(generateViewRegistrations(timepointsCol,
				viewSetups)));
		removeAllBlocks(pathToDir, viewSetups, timepointsCol);
		return result;
	}

	private Map<Integer, ExportMipmapInfo> assignMipmapInfoToViewSetups(
		Collection<ViewSetup> viewSetups, ExportMipmapInfo mipmapInfo)
	{

		Map<Integer, ExportMipmapInfo> result = new HashMap<>();
		for (ViewSetup viewSetup : viewSetups) {
			result.put(viewSetup.getId(), mipmapInfo);
		}
		return result;
	}

	private Collection<ViewSetup> generateViewSetups(long[] dimensions,
		VoxelDimensions voxelDimensions, int channels, int angles)
	{
		Collection<ViewSetup> viewSetups = new LinkedList<>();
		Illumination illumination = new Illumination(0);
		int setupId = 0;
		for (int channel = 0; channel < channels; channel++) {
			for (int angle = 0; angle < angles; angle++) {
				Angle angleObj = new Angle(angle);
				Channel channelObj = new Channel(channel);
				ViewSetup vs = new ViewSetup(setupId, "setup" + setupId,
					new FinalDimensions(dimensions), voxelDimensions, channelObj,
					angleObj, illumination);
				viewSetups.add(vs);
				setupId++;
			}
		}
		return viewSetups;
	}

	private Collection<ViewRegistration> generateViewRegistrations(
		Collection<TimePoint> timepoints, Collection<ViewSetup> viewSetups)
	{
		Collection<ViewRegistration> result = new LinkedList<>();
		for (ViewSetup viewSetup : viewSetups) {
			for (TimePoint timePoint : timepoints) {
				result.add(new ViewRegistration(timePoint.getId(), viewSetup.getId()));
			}
		}
		return result;
	}

	private void removeAllBlocks(Path pathToDir,
		Collection<ViewSetup> viewSetups, final Collection<TimePoint> timepointsCol)
		throws IOException
	{
		N5Writer writer = new N5FSWriter(pathToDir.toString());
		for (TimePoint timePoint : timepointsCol) {
			for (ViewSetup viewSetup : viewSetups) {
				String path = BdvN5Format.getPathName(viewSetup.getId(), timePoint
					.getId());
				for (String levelPath : writer.list(path)) {
					String subPath = String.format("%s/%s", path, levelPath);
					for (String datasetPath : writer.list(subPath)) {
						writer.remove(String.format("%s/%s", subPath, datasetPath));
					}
				}
			}
		}
	}

	@Builder
	public static class N5Description {

		@NonNull
		private final DataType voxelType;

		@NonNull
		private final long[] dimensions;

		@NonNull
		private final VoxelDimensions voxelDimensions;

		@Builder.Default
		private final int timepoints = 1;

		@Builder.Default
		private final int channels = 1;

		@Builder.Default
		private final int angles = 1;

		@NonNull
		private final Compression compression;

		@NonNull
		private final ExportMipmapInfo mipmapInfo;

	}

	private static class LoopbackHeuristicAdapter implements LoopbackHeuristic {

		@Override
		public boolean decide(RandomAccessibleInterval<?> originalImg,
			int[] factorsToOriginalImg, int previousLevel,
			int[] factorsToPreviousLevel, int[] chunkSize)
		{
			return false;
		}
	}

	private static class AfterEachPlaneAdapter implements AfterEachPlane {

		@Override
		public void afterEachPlane(boolean usedLoopBack) {
			// it is only adapter
		}

	}

	private static class ProgressWriterAdapter implements ProgressWriter {

		@Override
		public PrintStream out() {
			return System.out;
		}

		@Override
		public PrintStream err() {
			return System.err;
		}

		@Override
		public void setProgress(double completionRatio) {
			// it is only adapter
		}
	}

	private static class ImgLoaderImpl<T> implements ImgLoader {

		private SetupImgLoader<T> loader;

		public ImgLoaderImpl(T type, long[] dimensions) {
			loader = new SetupImgLoaderImpl<>(type, dimensions);
		}

		@Override
		public SetupImgLoader<?> getSetupImgLoader(int setupId) {
			return loader;
		}

		private static class SetupImgLoaderImpl<U> implements SetupImgLoader<U> {

			private U type;
			private RandomAccessibleInterval<U> interval;

			public SetupImgLoaderImpl(U type, long[] dimensions) {
				this.type = type;
				this.interval = new EmptyImg<>(type, dimensions);
			}

			@Override
			public RandomAccessibleInterval<U> getImage(int timepointId,
				ImgLoaderHint... hints)
			{
				return interval;
			}

			@Override
			public U getImageType() {
				return type;
			}

			@Override
			public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId,
				boolean normalize, ImgLoaderHint... hints)
			{
				return null;
			}

			@Override
			public Dimensions getImageSize(int timepointId) {
				return null;
			}

			@Override
			public VoxelDimensions getVoxelSize(int timepointId) {
				return null;
			}
		}
	}

	private static class EmptyImg<T> extends AbstractListImg<T> {

		private T type;

		public EmptyImg(T type, long[] size) {
			super(size);
			this.type = type;
		}

		@Override
		public Img<T> copy() {
			return this;
		}

		@Override
		protected T get(int index) {
			return type;
		}

		@Override
		protected void set(int index, T value) {

		}
	}

}
