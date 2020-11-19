/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.list.AbstractListImg;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.RawCompression;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.export.ProgressWriter;
import bdv.export.n5.WriteSequenceToN5;
import bdv.img.n5.BdvN5Format;
import bdv.img.n5.N5ImageLoader;
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;


@Slf4j
public class N5Access {

	public static void main(String[] args) throws IOException, SpimDataException {
		createNew("/tmp/n5", new int[] { 1000, 1000, 100 }, 2, 2, 2,
			new RawCompression(), new int[][] { { 1, 1, 1 }, { 2, 2, 2 } },
			new int[][] { { 64, 64, 64 }, { 64, 64, 64 } });
	}

	private Path baseDirectory;
	private N5FSWriter writer;
	private SpimData data;

	public static N5Access loadExisting(String path) throws IOException,
		SpimDataException
	{
		N5Access result = new N5Access(path);
		result.loadFromXML(path);
		return result;
	}

	public static N5Access createNew(String path, int[] dimensions,
		int timepoints, int channels, int angles, Compression compression,
		int[][] resolutions, int[][] subdivisions)
		throws IOException, SpimDataException
	{
		N5Access result = new N5Access(path);
		SpimData data = result.createNew(dimensions, timepoints, channels, angles,
			compression, resolutions, subdivisions);
		result.saveToXML(data, path);
		return result;
	}

	private N5Access(String path) throws IOException {
		baseDirectory = Paths.get(path.replaceAll("\\.xml$", ".n5"));
		writer = new N5FSWriter(baseDirectory.toString());
	}

	/**
	 * TODO: Exceptions indicating not existent, block, angle, time, channel
	 * 
	 * @param gridPosition
	 * @param time
	 * @param channel
	 * @param angle
	 * @param resolutionLevel
	 * @return readed ByteBuffer or null;
	 * @throws IOException
	 */
	public ByteBuffer read(long[] gridPosition, int time, int channel, int angle,
		int[] resolutionLevel) throws IOException
	{
		String path = getPath(time, channel, angle, resolutionLevel);
		if (path == null) {
			return null;
		}
		log.info("Path: {}", path);
		DataBlock<?> block = writer.readBlock(path, writer.getDatasetAttributes(
			path), gridPosition);
		return block.toByteBuffer();
	}

	private void loadFromXML(String path) throws SpimDataException {
		data = new XmlIoSpimData().load(path);
	}

	private void saveToXML(SpimData aData, String path) throws SpimDataException {
		new XmlIoSpimData().save(aData, path);
		this.data = aData;
	}

	private SpimData createNew(int[] dimensions, int timepoints, int channels,
		int angles, Compression compression, int[][] resolutions,
		int[][] subdivisions) throws IOException
	{

		final Collection<TimePoint> timepointsCol = IntStream.range(0, timepoints)
			.<TimePoint> mapToObj(TimePoint::new).collect(Collectors.toList());

		Collection<ViewSetup> viewSetups = generateViewSetups(dimensions,
			channels, angles);

		Map<Integer, ExportMipmapInfo> exportMipmapInfo = generateMipmapInfo(
			viewSetups, resolutions, subdivisions);
		long[] lDimensions = Arrays.stream(dimensions).asLongStream().toArray();
		SequenceDescription sequenceDescription = new SequenceDescription(new TimePoints(
			timepointsCol), viewSetups, new ImgLoaderImpl<>(new IntType(),
					lDimensions));
		WriteSequenceToN5.writeN5File(sequenceDescription, exportMipmapInfo, compression, baseDirectory.toFile(),
			new LoopbackHeuristicAdapter(), new AfterEachPlaneAdapter(), 1,
			new ProgressWriterAdapter());
		sequenceDescription = new SequenceDescription(new TimePoints(timepointsCol),
			viewSetups, new N5ImageLoader(baseDirectory.toFile(),
				sequenceDescription));
		return new SpimData(baseDirectory.toFile(), sequenceDescription,
			new ViewRegistrations(generateViewRegistrations(timepointsCol,
				viewSetups)));
	}

	private Map<Integer, ExportMipmapInfo> generateMipmapInfo(
		Collection<ViewSetup> viewSetups, int[][] resolutions, int[][] subdivisions)
	{
		ExportMipmapInfo info = new ExportMipmapInfo(resolutions, subdivisions);
		Map<Integer, ExportMipmapInfo> result = new HashMap<>();
		for (ViewSetup viewSetup : viewSetups) {
			result.put(viewSetup.getId(), info);
		}
		return result;
	}

	private Collection<ViewSetup> generateViewSetups(int[] dimensions,
		int channels, int angles)
	{
		Collection<ViewSetup> viewSetups = new LinkedList<>();
		int setupId = 0;
		for (int channel = 0; channel < channels; channel++) {
			for (int angle = 0; angle < angles; angle++) {
				Angle angleObj = new Angle(angle);
				Channel channelObj = new Channel(channel);
				ViewSetup vs = new ViewSetup(setupId, "setup" + setupId,
					new FinalDimensions(dimensions), null, channelObj, angleObj, null);
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

	private String getPath(int timId, int channel, int angle,
		int[] resolutionLevel)
	{
		ViewSetup viewSetup = getViewSetup(channel, angle);
		if (viewSetup == null) {
			return null;
		}

		Integer levelId = getLevelId(viewSetup, timId, resolutionLevel);
		if (levelId == null) {
			return null;
		}
		return BdvN5Format.getPathName(viewSetup.getId(), timId, levelId);
	}

	private Integer getLevelId(ViewSetup viewSetup, int timId,
		int[] resolutionLevel)
	{
		String baseGroup = BdvN5Format.getPathName(viewSetup.getId(), timId);
		double[] resolution = getAttribute(BdvN5Format.getPathName(viewSetup
			.getId(), timId), "resolution", double[].class, () -> new double[] { 1.,
				1., 1. });

		if (!Arrays.equals(resolution, new double[] { 1., 1., 1. })) {
			throw new UnsupportedOperationException("Resolution " + String.join(",",
				getAttribute(baseGroup, "resolution", String[].class, () -> null)) +
				" is not supported. Only supported resolution is [1.0, 1.0, 1.0].");
		}

		try {
			Pattern levelGroupPattern = Pattern.compile("s(\\p{Digit}+)");
			String[] values = writer.list(baseGroup);
			Matcher m2 = levelGroupPattern.matcher(values[1]);
			log.info("Matches={}", m2.matches());
			// @formatter:off			
			return Arrays.asList(writer.list(baseGroup))
														.stream().map(levelGroupPattern::matcher)
														.filter(Matcher::matches)
														.filter(m -> matchResolutionLevel(baseGroup, m.group(), resolutionLevel))
														.map(m -> m.group(1))
														.findAny()
														.map(Integer::valueOf)
														.orElse(null);
		// @formatter:on
		}
		catch (IOException exc) {
			log.warn("Listing group :" + baseGroup, exc);
			return -1;
		}
	}

	private boolean matchResolutionLevel(String baseGroup, String subGroup,
		int[] resolutionLevel)
	{
		return Arrays.equals(resolutionLevel, getAttribute(baseGroup + "/" +
			subGroup, "downsamplingFactors", int[].class, () -> new int[] {}));
	}

	private ViewSetup getViewSetup(int channel, long angle) {
		return data.getSequenceDescription().getViewSetupsOrdered().stream().filter(
			lvs -> lvs.getChannel().getId() == channel && lvs.getAngle()
				.getId() == angle).findFirst().orElse(null);
	}

	private <T> T getAttribute(String pathName, String attrName, Class<T> clazz,
		Supplier<T> defaultResult)
	{
		try {
			return writer.getAttribute(pathName, attrName, clazz);
		}
		catch (IOException exc) {
			return defaultResult.get();
		}

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
