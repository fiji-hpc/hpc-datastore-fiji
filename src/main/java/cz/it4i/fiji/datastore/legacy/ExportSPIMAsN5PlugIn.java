
package cz.it4i.fiji.datastore.legacy;

import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.spimdata.SequenceDescriptionMinimal;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import lombok.extern.slf4j.Slf4j;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;

/**
 * ImageJ plugin to export the current image to xml/n5.
 *
 * @author Tobias Pietzsch
 */
@Slf4j
@Plugin(type = Command.class,
	menuPath = "Plugins>BigDataViewer>Export SPIM data as remote XML/N5")
public class ExportSPIMAsN5PlugIn implements Command {

	public static void main(final String[] args) {
		log.debug("main");
		new ImageJ();
		new ExportSPIMAsN5PlugIn().run();

	}

	@Override
	public void run() {
		if (ij.Prefs.setIJMenuBar) System.setProperty("apple.laf.useScreenMenuBar",
			"true");
		/*
				SpimData spimdata = null;
				
		*/
		// show dialog to get output paths, resolutions, subdivisions, min-max
		// option
		final Parameters params = new ParameterConstructor().getParameters();
		if (params == null) return;

		final ProgressWriter progressWriter = new ProgressWriterIJ();
		progressWriter.out().println("starting export...");

		// create ImgLoader wrapping the image

		final ImgLoader imgLoader = params.spimData.getSequenceDescription()
			.getImgLoader();

		// TODO reimplement following
		final Runnable clearCache = () -> {};
		final boolean isVirtual = false;
		/*final Runnable clearCache;
		final boolean isVirtual = imp.getStack() != null && imp.getStack().isVirtual();
		if ( isVirtual )
		{
			final VirtualStackImageLoader< ?, ?, ? > il;
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				il = VirtualStackImageLoader.createUnsignedByteInstance( imp );
				break;
			case ImagePlus.GRAY16:
				il = VirtualStackImageLoader.createUnsignedShortInstance( imp );
				break;
			case ImagePlus.GRAY32:
			default:
				il = VirtualStackImageLoader.createFloatInstance( imp );
				break;
			}
			imgLoader = il;
			clearCache = il.getCacheControl()::clearCache;
		}
		else
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				imgLoader = ImageStackImageLoader.createUnsignedByteInstance( imp );
				break;
			case ImagePlus.GRAY16:
				imgLoader = ImageStackImageLoader.createUnsignedShortInstance( imp );
				break;
			case ImagePlus.GRAY32:
			default:
				imgLoader = ImageStackImageLoader.createFloatInstance( imp );
				break;
			}
			clearCache = () -> {};
		}*/

		ViewSetup viewSetupZero = params.spimData.getSequenceDescription()
			.getViewSetupsOrdered().get(0);

		// get calibration and image size
		final double pw = viewSetupZero.getVoxelSize().dimension(0);
		final double ph = viewSetupZero.getVoxelSize().dimension(1);
		final double pd = viewSetupZero.getVoxelSize().dimension(2);
		String punit = viewSetupZero.getVoxelSize().unit();
		if (punit == null || punit.isEmpty()) punit = "px";
		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions(punit, pw,
			ph, pd);

		final int w = (int) viewSetupZero.getSize().dimension(0);
		final int h = (int) viewSetupZero.getSize().dimension(1);
		final int d = (int) viewSetupZero.getSize().dimension(2);
		final FinalDimensions size = new FinalDimensions(w, h, d);

		final int numTimepoints = params.spimData.getSequenceDescription()
			.getTimePoints().size();
		final int numSetups = params.spimData.getSequenceDescription()
			.getViewSetupsOrdered().size();

		// create SourceTransform from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0);

		// write n5
		final HashMap<Integer, BasicViewSetup> setups = new HashMap<>(numSetups);
		for (int s = 0; s < numSetups; ++s) {
			final BasicViewSetup setup = new BasicViewSetup(s, String.format(
				"channel %d", s + 1), size, voxelSize);
			setup.setAttribute(new Channel(s + 1));
			setups.put(s, setup);
		}
		final ArrayList<TimePoint> timepoints = new ArrayList<>(numTimepoints);
		for (int t = 0; t < numTimepoints; ++t)
			timepoints.add(new TimePoint(t));
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(
			new TimePoints(timepoints), setups, imgLoader, null);

		Map<Integer, ExportMipmapInfo> perSetupExportMipmapInfo;
		perSetupExportMipmapInfo = new HashMap<>();
		final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo(params.resolutions,
			params.subdivisions);
		for (final BasicViewSetup setup : seq.getViewSetupsOrdered())
			perSetupExportMipmapInfo.put(setup.getId(), mipmapInfo);

		// LoopBackHeuristic:
		// - If saving more than 8x on pixel reads use the loopback image over
		// original image
		// - For virtual stacks also consider the cache size that would be
		// required for all original planes contributing to a "plane of
		// blocks" at the current level. If this is more than 1/4 of
		// available memory, use the loopback image.
		// TODO reimplement following
		// final long planeSizeInBytes = imp.getWidth() * imp.getHeight() *
		// imp.getBytesPerPixel();
		final long planeSizeInBytes = -1;
		final long ijMaxMemory = IJ.maxMemory();
		final int numCellCreatorThreads = Math.max(1, PluginHelper.numThreads() -
			1);
		final LoopbackHeuristic loopbackHeuristic = new LoopbackHeuristic() {

			@Override
			public boolean decide(final RandomAccessibleInterval<?> originalImg,
				final int[] factorsToOriginalImg, final int previousLevel,
				final int[] factorsToPreviousLevel, final int[] chunkSize)
			{
				if (previousLevel < 0) return false;

				if (Intervals.numElements(factorsToOriginalImg) / Intervals.numElements(
					factorsToPreviousLevel) >= 8) return true;

				if (isVirtual) {
					final long requiredCacheSize = planeSizeInBytes *
						factorsToOriginalImg[2] * chunkSize[2];
					if (requiredCacheSize > ijMaxMemory / 4) return true;
				}

				return false;
			}
		};

		final AfterEachPlane afterEachPlane = new AfterEachPlane() {

			@Override
			public void afterEachPlane(final boolean usedLoopBack) {
				if (!usedLoopBack && isVirtual) {
					final long free = Runtime.getRuntime().freeMemory();
					final long total = Runtime.getRuntime().totalMemory();
					final long max = Runtime.getRuntime().maxMemory();
					final long actuallyFree = max - total + free;

					if (actuallyFree < max / 2) clearCache.run();
				}
			}

		};

		try {
			final N5RESTAdapter adapter = new N5RESTAdapter(seq,
				perSetupExportMipmapInfo, imgLoader, params.compression);
			WriteSequenceToN5.writeN5File(seq, perSetupExportMipmapInfo,
				params.compression, () -> adapter.constructN5Writer(params.serverURL
					.toString()), loopbackHeuristic, afterEachPlane,
				numCellCreatorThreads, new SubTaskProgressWriter(progressWriter, 0,
					0.95));
			progressWriter.setProgress(1.0);
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		progressWriter.out().println("done");
	}

	protected static class Parameters {

		final boolean setMipmapManual;

		final int[][] resolutions;

		final int[][] subdivisions;

		final URL serverURL;

		final Compression compression;

		final SpimData spimData;

		public Parameters(final boolean setMipmapManual, final int[][] resolutions,
			final int[][] subdivisions, final URL serverURL,
			final Compression compression, SpimData spimData)
		{
			this.setMipmapManual = setMipmapManual;
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.serverURL = serverURL;
			this.compression = compression;
			this.spimData = spimData;
		}
	}

	static String lastSPIMdata = "";

	static boolean lastSetMipmapManual = false;

	static String lastSubsampling = "";

	static String lastChunkSizes = "";

	static int lastCompressionChoice = 0;

	static boolean lastCompressionDefaultSettings = true;

	static String lastServerURL = "http://localhost:9080";

	private static class ParameterConstructor {

		private final XmlIoSpimData io = new XmlIoSpimData();
		private SpimData SPIMData;
		private String autoSubsampling;
		private String autoChunkSizes;
		
		protected Parameters getParameters() {
			while (true) {
				final GenericDialogPlus gd = new GenericDialogPlus(
					"Export for BigDataViewer as XML/N5");

				gd.addFileField("SPIM_data", lastSPIMdata);
				gd.addCheckbox("manual_mipmap_setup", lastSetMipmapManual);
				final Checkbox cManualMipmap = (Checkbox) gd.getCheckboxes()
					.lastElement();
				gd.addStringField("Subsampling_factors", lastSubsampling, 25);
				final TextField tfSubsampling = (TextField) gd.getStringFields()
					.lastElement();
				gd.addStringField("N5_chunk_sizes", lastChunkSizes, 25);
				final TextField tfChunkSizes = (TextField) gd.getStringFields()
					.lastElement();

				gd.addMessage("");
				final String[] compressionChoices = new String[] {
					"raw (no compression)", "bzip", "gzip", "lz4", "xz" };
				gd.addChoice("compression", compressionChoices,
					compressionChoices[lastCompressionChoice]);
				gd.addCheckbox("default settings", lastCompressionDefaultSettings);

				gd.addMessage("");
				gd.addStringField("Export_URL", lastServerURL, 25);

				tryLoadSPIMData(lastSPIMdata);

				gd.addDialogListener((dialog, e) -> {
					tryLoadSPIMData(gd.getNextString());
					gd.getNextBoolean();
					gd.getNextString();
					gd.getNextString();
					gd.getNextChoiceIndex();
					gd.getNextBoolean();
					gd.getNextString();
					
					if (autoSubsampling != null && autoChunkSizes != null &&
						e instanceof ItemEvent && e
							.getID() == ItemEvent.ITEM_STATE_CHANGED && e
								.getSource() == cManualMipmap)
					{
						final boolean useManual = cManualMipmap.getState();
						tfSubsampling.setEnabled(useManual);
						tfChunkSizes.setEnabled(useManual);
						if (!useManual) {
							tfSubsampling.setText(autoSubsampling);
							tfChunkSizes.setText(autoChunkSizes);
						}
					}
					return true;
				});

				tfSubsampling.setEnabled(lastSetMipmapManual);
				tfChunkSizes.setEnabled(lastSetMipmapManual);
				if (!lastSetMipmapManual) {
					tfSubsampling.setText(autoSubsampling);
					tfChunkSizes.setText(autoChunkSizes);
				}

				gd.showDialog();
				if (gd.wasCanceled()) return null;
				lastSPIMdata = gd.getNextString();
				lastSetMipmapManual = gd.getNextBoolean();
				lastSubsampling = gd.getNextString();
				lastChunkSizes = gd.getNextString();
				lastCompressionChoice = gd.getNextChoiceIndex();
				lastCompressionDefaultSettings = gd.getNextBoolean();
				lastServerURL = gd.getNextString();

				// parse mipmap resolutions and cell sizes
				final int[][] resolutions = PluginHelper.parseResolutionsString(
					lastSubsampling);
				final int[][] subdivisions = PluginHelper.parseResolutionsString(
					lastChunkSizes);
				if (resolutions.length == 0) {
					IJ.showMessage("Cannot parse subsampling factors " + lastSubsampling);
					continue;
				}
				if (subdivisions.length == 0) {
					IJ.showMessage("Cannot parse n5 chunk sizes " + lastChunkSizes);
					continue;
				}
				else if (resolutions.length != subdivisions.length) {
					IJ.showMessage(
						"subsampling factors and n5 chunk sizes must have the same number of elements");
					continue;
				}
				URL serverURL;
				try {
					serverURL = new URL(lastServerURL);
				}
				catch (MalformedURLException exc) {
					IJ.showMessage("Invalid server URL" + lastServerURL);
					continue;
				}
				tryLoadSPIMData(lastSPIMdata);
				if (SPIMData == null) {
					IJ.showMessage("Invalid SPIM xml file " + lastSPIMdata);
				}
				

				final Compression compression;
				switch (lastCompressionChoice) {
					default:
					case 0: // raw (no compression)
						compression = new RawCompression();
						break;
					case 1: // bzip
						compression = lastCompressionDefaultSettings
							? new Bzip2Compression() : getBzip2Settings();
						break;
					case 2: // gzip
						compression = lastCompressionDefaultSettings ? new GzipCompression()
							: getGzipSettings();
						break;
					case 3:// lz4
						compression = lastCompressionDefaultSettings ? new Lz4Compression()
							: getLz4Settings();
						break;
					case 4:// xz" };
						compression = lastCompressionDefaultSettings ? new XzCompression()
							: getXzSettings();
						break;
				}
				if (compression == null) return null;

				return new Parameters(lastSetMipmapManual, resolutions, subdivisions,
					serverURL, compression, SPIMData);
			}
		}

		static int lastBzip2BlockSize = BZip2CompressorOutputStream.MAX_BLOCKSIZE;

		protected Bzip2Compression getBzip2Settings() {
			while (true) {
				final GenericDialogPlus gd = new GenericDialogPlus(
					"Bzip2 compression settings");
				gd.addNumericField(String.format("block size (%d-%d)",
					BZip2CompressorOutputStream.MIN_BLOCKSIZE,
					BZip2CompressorOutputStream.MAX_BLOCKSIZE), lastBzip2BlockSize, 0);
				gd.addMessage("as 100k units");

				gd.showDialog();
				if (gd.wasCanceled()) return null;

				lastBzip2BlockSize = (int) gd.getNextNumber();
				if (lastBzip2BlockSize < BZip2CompressorOutputStream.MIN_BLOCKSIZE ||
					lastBzip2BlockSize > BZip2CompressorOutputStream.MAX_BLOCKSIZE)
				{
					IJ.showMessage(String.format("Block size must be in range [%d, %d]",
						BZip2CompressorOutputStream.MIN_BLOCKSIZE,
						BZip2CompressorOutputStream.MAX_BLOCKSIZE));
					continue;
				}
				return new Bzip2Compression(lastBzip2BlockSize);
			}
		}

		static int lastGzipLevel = 6;

		static boolean lastGzipUseZlib = false;

		protected GzipCompression getGzipSettings() {
			while (true) {
				final GenericDialogPlus gd = new GenericDialogPlus(
					"Gzip compression settings");
				gd.addNumericField("level (0-9)", lastGzipLevel, 0);
				gd.addCheckbox("use Zlib", lastGzipUseZlib);

				gd.showDialog();
				if (gd.wasCanceled()) return null;

				lastGzipLevel = (int) gd.getNextNumber();
				lastGzipUseZlib = gd.getNextBoolean();
				if (lastGzipLevel < 0 || lastGzipLevel > 9) {
					IJ.showMessage("Level must be in range [0, 9]");
					continue;
				}
				return new GzipCompression(lastGzipLevel, lastGzipUseZlib);
			}
		}

		static int lastLz4BlockSize = 1 << 16;

		protected Lz4Compression getLz4Settings() {
			final int COMPRESSION_LEVEL_BASE = 10;
			final int MIN_BLOCK_SIZE = 64;
			final int MAX_BLOCK_SIZE = 1 << (COMPRESSION_LEVEL_BASE + 0x0F);

			while (true) {
				final GenericDialogPlus gd = new GenericDialogPlus(
					"LZ4 compression settings");
				gd.addNumericField(String.format("block size (%d-%d)", MIN_BLOCK_SIZE,
					MAX_BLOCK_SIZE), lastLz4BlockSize, 0, 8, null);

				gd.showDialog();
				if (gd.wasCanceled()) return null;

				lastLz4BlockSize = (int) gd.getNextNumber();
				if (lastLz4BlockSize < MIN_BLOCK_SIZE ||
					lastLz4BlockSize > MAX_BLOCK_SIZE)
				{
					IJ.showMessage(String.format("Block size must be in range [%d, %d]",
						MIN_BLOCK_SIZE, MAX_BLOCK_SIZE));
					continue;
				}
				return new Lz4Compression(lastLz4BlockSize);
			}
		}

		static int lastXzLevel = 6;

		protected XzCompression getXzSettings() {
			while (true) {
				final GenericDialogPlus gd = new GenericDialogPlus(
					"XZ compression settings");
				gd.addNumericField("level (0-9)", lastXzLevel, 0);
				gd.addMessage("LZMA2 preset level");
		
				gd.showDialog();
				if (gd.wasCanceled()) return null;
		
				lastXzLevel = (int) gd.getNextNumber();
				if (lastXzLevel < 0 || lastXzLevel > 9) {
					IJ.showMessage("Level must be in range [0, 9]");
					continue;
				}
				return new XzCompression(lastXzLevel);
			}
		}

		private void tryLoadSPIMData(String path) {
			try {
				SPIMData = io.load(path);
				ViewSetup viewSetupZero = SPIMData.getSequenceDescription()
					.getViewSetupsOrdered().get(0);

				// get calibration and image size
				final double pw = viewSetupZero.getVoxelSize().dimension(0);
				final double ph = viewSetupZero.getVoxelSize().dimension(1);
				final double pd = viewSetupZero.getVoxelSize().dimension(2);
				String punit = viewSetupZero.getVoxelSize().unit();
				if (punit == null || punit.isEmpty()) punit = "px";
				final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions(punit,
					pw, ph, pd);

				final int w = (int) viewSetupZero.getSize().dimension(0);
				final int h = (int) viewSetupZero.getSize().dimension(1);
				final int d = (int) viewSetupZero.getSize().dimension(2);
				final FinalDimensions size = new FinalDimensions(w, h, d);

				// propose reasonable mipmap settings
				final int maxNumElements = 64 * 64 * 64;
				final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps
					.proposeMipmaps(new BasicViewSetup(0, "", size, voxelSize),
						maxNumElements);
				autoSubsampling = ProposeMipmaps.getArrayString(autoMipmapSettings
					.getExportResolutions());
				autoChunkSizes = ProposeMipmaps.getArrayString(autoMipmapSettings
					.getSubdivisions());
			}
			catch (SpimDataException exc) {
				// ignore
				SPIMData = null;
				autoSubsampling = null;
				autoChunkSizes = null;
			}
		
		}
	}

}
