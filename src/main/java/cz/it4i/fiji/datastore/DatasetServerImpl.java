/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;

import bdv.img.n5.BdvN5Format;
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.ViewSetup;

@Default
@RequestScoped
@Slf4j
public class DatasetServerImpl implements Closeable {

	private SpimData data;
	private N5Writer writer;
	private Path baseDirectory;

	@Inject
	private ApplicationConfiguration configuration;

	public DatasetServerImpl() {
		log.debug("constructed");
	}

	@PostConstruct
	private void init() throws SpimDataException, IOException {
		String path = configuration.getDatasetPath();
		final XmlIoSpimData io = new XmlIoSpimData();
		data = io.load(path);
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

	@Override
	public void close() {

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

}
