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
import net.imglib2.type.numeric.real.FloatType;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
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
import lombok.extern.slf4j.Slf4j;
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


@Slf4j
public class N5Access {

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

	public static class DatasetLocation4Writing {

		private final String path;
		private final ViewSetup viewSetup;
		private final DatasetAttributes datasetAttributes;

		private DatasetLocation4Writing(SpimData spimData, N5Writer writer,
			int time,
			int channel, int angle, int[] resolutionLevel) throws IOException
		{
			viewSetup = getViewSetup(spimData, channel, angle);
			if (viewSetup == null) {
				throw new IllegalArgumentException(String.format(
					"Channel=%d and angle=%d not found.", channel, angle));
			}
			path = getPath(writer, time, viewSetup, resolutionLevel);
			if (path == null) {
				throw new IllegalArgumentException(String.format(
					"Channel=%d, angle=%d and timepoint=%d not found.", channel, angle,
					time));
			}
			log.info("Path: {}", path);

			datasetAttributes = writer.getDatasetAttributes(path);

		}

		public void writeBlock(N5Writer writer, long[] gridPosition,
			ByteBuffer data) throws IOException
		{
			DataBlock<?> block = datasetAttributes.getDataType().createDataBlock(
				datasetAttributes.getBlockSize(), gridPosition);
			block.readData(data);
			writer.writeBlock(path, datasetAttributes, block);
		}

		public int getNumOfBytes() {
			return DataBlock.getNumElements(datasetAttributes.getBlockSize()) *
				getSizeOfElement();
		}

		private int getSizeOfElement() {
			switch (datasetAttributes.getDataType()) {
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
					throw new IllegalArgumentException("Datatype " + datasetAttributes
						.getDataType() +
						" not supported");
			}
		}
	}

	private Path baseDirectory;
	private N5Writer writer;
	private SpimData spimData;

	public static N5Access loadExisting(String path) throws IOException,
		SpimDataException
	{
		N5Access result = new N5Access(path);
		result.loadFromXML(path);
		return result;
	}

	public static N5Access createNew(String path, N5Description dsc)
		throws IOException, SpimDataException
	{
		N5Access result = new N5Access(path);
		SpimData data = result.createNew(dsc.voxelType, dsc.dimensions,
			dsc.voxelDimensions, dsc.timepoints, dsc.channels, dsc.angles,
			dsc.compression, dsc.mipmapInfo);
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
		String path = getPath(spimData, writer, time, channel, angle,
			resolutionLevel);
		if (path == null) {
			return null;
		}
		log.info("Path: {}", path);
		return writer.readBlock(path, writer.getDatasetAttributes(path),
			gridPosition).toByteBuffer();
	}

	public void write(long[] gridPosition,
		DatasetLocation4Writing location4Writing, ByteBuffer data)
		throws IOException
	{
		location4Writing.writeBlock(writer, gridPosition, data);
	}



	public DatasetLocation4Writing getDatasetLocation4Writing(int time,
		int channel, int angle, int[] resolutionLevel) throws IOException
	{
		return new DatasetLocation4Writing(spimData, writer, time, channel, angle,
			resolutionLevel);
	}

	private void loadFromXML(String path) throws SpimDataException {
		spimData = new XmlIoSpimData().load(path);
	}

	private void saveToXML(SpimData aData, String path) throws SpimDataException {
		new XmlIoSpimData().save(aData, path);
		this.spimData = aData;
	}

	private SpimData createNew(DataType voxelType, long[] dimensions,
		VoxelDimensions voxelDimensions, int timepoints, int channels, int angles,
		Compression compression, ExportMipmapInfo mipmapInfo) throws IOException
	{

		final Collection<TimePoint> timepointsCol = IntStream.range(0, timepoints)
			.<TimePoint> mapToObj(TimePoint::new).collect(Collectors.toList());

		Collection<ViewSetup> viewSetups = generateViewSetups(dimensions,
			voxelDimensions, channels, angles);

		Map<Integer, ExportMipmapInfo> exportMipmapInfo =
			assignMipmapInfoToViewSetups(viewSetups, mipmapInfo);
		SequenceDescription sequenceDescription = new SequenceDescription(new TimePoints(
			timepointsCol), viewSetups, new ImgLoaderImpl<>(N5Utils.type(voxelType),
				dimensions));
		WriteSequenceToN5.writeN5File(sequenceDescription, exportMipmapInfo, compression, baseDirectory.toFile(),
			new LoopbackHeuristicAdapter(), new AfterEachPlaneAdapter(), 1,
			new ProgressWriterAdapter());
		sequenceDescription = new SequenceDescription(new TimePoints(timepointsCol),
			viewSetups, new N5ImageLoader(baseDirectory.toFile(),
				sequenceDescription));
		SpimData result = new SpimData(baseDirectory.toFile(), sequenceDescription,
			new ViewRegistrations(generateViewRegistrations(timepointsCol,
				viewSetups)));
		removeAllBlocks(viewSetups, timepointsCol);
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
					angleObj,
					illumination);
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

	private void removeAllBlocks(Collection<ViewSetup> viewSetups,
		final Collection<TimePoint> timepointsCol) throws IOException
	{
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

	private static String getPath(SpimData spimData, N5Writer writer, int timeId,
		int channel, int angle,
		int[] resolutionLevel)
	{
		ViewSetup viewSetup = getViewSetup(spimData, channel, angle);
		if (viewSetup == null) {
			return null;
		}
		return getPath(writer, timeId, viewSetup, resolutionLevel);
	}

	private static String getPath(N5Writer writer, int timeId,
		ViewSetup viewSetup, int[] resolutionLevel)
	{
		Integer levelId = getLevelId(writer, viewSetup, timeId, resolutionLevel);
		if (levelId == null) {
			return null;
		}
		return BdvN5Format.getPathName(viewSetup.getId(), timeId, levelId);
	}

	private static Integer getLevelId(N5Writer writer, ViewSetup viewSetup,
		int timId, int[] resolutionLevel)
	{
		String baseGroup = BdvN5Format.getPathName(viewSetup.getId(), timId);

		try {
			Pattern levelGroupPattern = Pattern.compile("s(\\p{Digit}+)");
			String[] values = writer.list(baseGroup);
			Matcher m2 = levelGroupPattern.matcher(values[1]);
			log.info("Matches={}", m2.matches());
			// @formatter:off			
			return Arrays.asList(writer.list(baseGroup))
														.stream().map(levelGroupPattern::matcher)
														.filter(Matcher::matches)
														.filter(m -> matchResolutionLevel(writer,baseGroup, m.group(), resolutionLevel))
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

	private static boolean matchResolutionLevel(N5Writer writer, String baseGroup,
		String subGroup,
		int[] resolutionLevel)
	{
		return Arrays.equals(resolutionLevel, getAttribute(writer, baseGroup + "/" +
			subGroup, "downsamplingFactors", int[].class, () -> new int[] {}));
	}

	private static ViewSetup getViewSetup(SpimData spimData, int channel,
		long angle)
	{
		return spimData.getSequenceDescription().getViewSetupsOrdered().stream().filter(
			lvs -> lvs.getChannel().getId() == channel && lvs.getAngle()
				.getId() == angle).findFirst().orElse(null);
	}

	private static <T> T getAttribute(N5Writer writer, String pathName,
		String attrName,
		Class<T> clazz,
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
