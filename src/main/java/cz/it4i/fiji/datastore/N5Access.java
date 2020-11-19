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
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
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
		createNew("/tmp/n5", new int[] { 1000, 1000, 100 }, 100,
			3, 1000, new RawCompression());
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
		int timepoints, int channels, int angles, Compression compression)
		throws IOException, SpimDataException
	{
		N5Access result = new N5Access(path);
		result.createNew(dimensions, timepoints, channels, angles, compression);
		result.loadFromXML(path);
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

	private void createNew(int[] dimensions, int timepoints, int channels,
		int angles, Compression compression) throws IOException
	{

		final Collection<TimePoint> timepointIds = IntStream.range(0, timepoints)
			.<TimePoint> mapToObj(TimePoint::new).collect(Collectors.toList());

		Collection<ViewSetup> viewSetups = generateViewSetups(dimensions,
			channels, angles);

		Map<Integer, ExportMipmapInfo> exportMipmapInfo = generateMipmapInfo(
			viewSetups, dimensions);

		WriteSequenceToN5.writeN5File(new SequenceDescription(new TimePoints(
			timepointIds), viewSetups, new ImgLoaderImpl<>(new IntType())),
			exportMipmapInfo, compression, baseDirectory
				.toFile(), new LoopbackHeuristicAdapter(), new AfterEachPlaneAdapter(),
			1, new ProgressWriterAdapter());
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
			}
			setupId++;
		}
		return viewSetups;
	}

	private Map<Integer, ExportMipmapInfo> generateMipmapInfo(
		Collection<ViewSetup> viewSetups, int[] dimensions)
	{
		ExportMipmapInfo info = new ExportMipmapInfo(new int[][] { dimensions }, new int[][] {
			{ 1, 1, 1 } });
		Map<Integer, ExportMipmapInfo> result = new HashMap<>();
		for (ViewSetup viewSetup : viewSetups) {
			result.put(viewSetup.getId(), info);
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

		public ImgLoaderImpl(T type) {
			loader = new SetupImgLoaderImpl(type);
		}

		@Override
		public SetupImgLoader<?> getSetupImgLoader(int setupId) {
			return loader;
		}

		private static class SetupImgLoaderImpl<U> implements SetupImgLoader<U> {

			private U type;

			public SetupImgLoaderImpl(U type) {
				this.type = type;
			}

			@Override
			public RandomAccessibleInterval<U> getImage(int timepointId,
				ImgLoaderHint... hints)
			{
				return null;
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



}
