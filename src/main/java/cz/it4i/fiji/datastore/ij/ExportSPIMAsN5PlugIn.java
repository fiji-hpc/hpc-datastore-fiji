
package cz.it4i.fiji.datastore.ij;

import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.MipmapInfo;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoaderMetaData;
import cz.it4i.fiji.datastore.rest_client.DatasetIndex;
import cz.it4i.fiji.datastore.rest_client.N5RESTAdapter;
import cz.it4i.fiji.datastore.rest_client.N5WriterWithUUID;
import cz.it4i.fiji.datastore.rest_client.WriteSequenceToN5;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.ViewSetup;

/**
 * ImageJ plugin to export the current image to xml/n5.
 *
 * @author Tobias Pietzsch
 */
@Log4j2
@Plugin(type = Command.class,
	menuPath = "Plugins>BigDataViewer>Export SPIM Data into DataStore")
public class ExportSPIMAsN5PlugIn implements Command {

	private static final String LAST_SET_MIPMAP_MANUAL = "LAST_SET_MIPMAP_MANUAL";

	private static final String LAST_COMPRESSION_DEFAULT_SETTINGS = "LAST_COMPRESSION_DEFAULT_SETTINGS";

	private static final String LAST_COMPRESSION_CHOICE = "LAST_COMPRESSION_CHOICE";

	private static final String LAST_CHUNK_SIZE = "LAST_CHUNK_SIZE";

	private static final String LAST_SUBSAMPLING = "LAST_SUBSAMPLING";

	private static final String LAST_SERVER_URL = "LAST_SERVER_URL";

	private static final String LAST_SPIM_DATA = "LAST_SPIM_DATA";

	private static final String LAST_DATASERVER_TIMEOUT = "LAST_DATASERVER_TIMEOUT";
	private static final int DEFAULT_DATASERVER_TIMEOUT = 1200;

	private static final String LAST_LABEL = "LAST_LABEL";

	@Parameter
	private PrefService prefService;

	@Parameter(type = ItemIO.OUTPUT, label="UUID of the created dataset:")
	public String newDatasetUUID;
	@Parameter(type = ItemIO.OUTPUT, label="Label of the created dataset:")
	public String newDatasetLabel;


	@Override
	public void run() {
		if (ij.Prefs.setIJMenuBar) System.setProperty("apple.laf.useScreenMenuBar",
			"true");
		loadPrefs();
		// show dialog to get output paths, resolutions, subdivisions, min-max
		// option
		final Parameters params = new ParameterConstructor().getParameters();
		if (params == null) return;
		savePrefs();

		final ProgressWriter progressWriter = new ProgressWriterIJ();
		progressWriter.out().println("starting export to remote N5...");

		// create ImgLoader wrapping the image

		final BasicImgLoader imgLoader = params.spimData.getSequenceDescription()
			.getImgLoader();

		// TODO reimplement following
		final Runnable clearCache = () -> {};
		final boolean isVirtual = false;

		
		
		ViewSetup viewSetupZero = params.spimData.getSequenceDescription()
			.getViewSetupsOrdered().get(0);

		String punit = viewSetupZero.getVoxelSize().unit();
		if (punit == null || punit.isEmpty()) punit = "px";


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

			@SuppressWarnings("unused")
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
			AbstractSequenceDescription<?, ?, ?> seq = params.spimData
				.getSequenceDescription();
			ViewRegistrations viewRegistrations = params.spimData
				.getViewRegistrations();

			final N5RESTAdapter adapter = new N5RESTAdapter(seq, viewRegistrations,
				params.resolutions, params.subdivisions, imgLoader, params.compression,
				params.label);
			
			class N5WriterProviderProxy {
				private N5WriterWithUUID n5Writer;

				N5WriterWithUUID getN5Writer() {
					if (n5Writer == null) {
						n5Writer = adapter.constructN5Writer(params.serverURL
							.toString(), params.dataserverTimeout * 1000l);
					}
					return n5Writer;
				}

				UUID getUUID() {
					return n5Writer.getUUID();
				}
			}
			
			N5WriterProviderProxy provider = new N5WriterProviderProxy();
			Map<Integer, MipmapInfo> perSetupMipmapInfo =
				HPCDatastoreImageLoaderMetaData.createPerSetupMipmapInfo(seq
					.getViewSetupsOrdered(), adapter.getDto());
			final DatasetIndex datasetIndex = new DatasetIndex(adapter.getDto(), seq);
			WriteSequenceToN5.writeN5File(seq, perSetupMipmapInfo, params.compression,
				() -> datasetIndex.getWriter(lastSPIMdata, params.serverURL, provider
					.getN5Writer()), loopbackHeuristic, afterEachPlane,
				numCellCreatorThreads, new SubTaskProgressWriter(progressWriter, 0,
					0.95));
			newDatasetUUID = provider.getUUID().toString();
			newDatasetLabel = params.label;
			progressWriter.setProgress(1.0);
			progressWriter.out().println("done");
			log.info("Created dataset UUID: " + newDatasetUUID);
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}

	}

	private void loadPrefs() {
		lastSPIMdata = prefService.get(getClass(), LAST_SPIM_DATA, "");
		lastSetMipmapManual = prefService.getBoolean(getClass(),
			LAST_SET_MIPMAP_MANUAL, false);
		lastSubsampling = prefService.get(getClass(), LAST_SUBSAMPLING,
			"{{1,1,1}}");
		lastChunkSizes = prefService.get(getClass(), LAST_CHUNK_SIZE,
			"{{64,64,64}}");
		lastCompressionChoice = prefService.getInt(getClass(),
			LAST_COMPRESSION_CHOICE, 0);
		lastCompressionDefaultSettings = prefService.getBoolean(getClass(),
			LAST_COMPRESSION_DEFAULT_SETTINGS, true);
		lastServerURL = prefService.get(getClass(), LAST_SERVER_URL,
			"http://localhost:9080");
		lastLabel = prefService.get(getClass(), LAST_LABEL, "");
		lastDataserverTimeout = prefService.getInt(getClass(),
			LAST_DATASERVER_TIMEOUT, DEFAULT_DATASERVER_TIMEOUT);
	}

	private void savePrefs() {
		prefService.put(getClass(), LAST_SPIM_DATA, lastSPIMdata);
		prefService.put(getClass(), LAST_SET_MIPMAP_MANUAL, lastSetMipmapManual);
		prefService.put(getClass(), LAST_SUBSAMPLING, lastSubsampling);
		prefService.put(getClass(), LAST_CHUNK_SIZE, lastChunkSizes);
		prefService.put(getClass(), LAST_COMPRESSION_CHOICE,
			lastCompressionChoice);
		prefService.put(getClass(), LAST_COMPRESSION_DEFAULT_SETTINGS,
			lastCompressionDefaultSettings);
		prefService.put(getClass(), LAST_SERVER_URL, lastServerURL);
		prefService.put(getClass(), LAST_LABEL, lastLabel);
		prefService.put(getClass(), LAST_DATASERVER_TIMEOUT, lastDataserverTimeout);
	}

	protected static class Parameters {

		final boolean setMipmapManual;

		final int[][] resolutions;

		final int[][] subdivisions;

		final URL serverURL;

		final Compression compression;

		final SpimData spimData;

		final int dataserverTimeout;

		final String label;

		public Parameters(final boolean setMipmapManual, final int[][] resolutions,
			final int[][] subdivisions, final URL serverURL,
			final Compression compression, SpimData spimData, int aDataserverTimeout,
			String label)
		{
			this.setMipmapManual = setMipmapManual;
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.serverURL = serverURL;
			this.compression = compression;
			this.spimData = spimData;
			this.dataserverTimeout = aDataserverTimeout;
			this.label = label;
		}
	}

	static String lastSPIMdata;

	static boolean lastSetMipmapManual = true;

	static String lastSubsampling = "{{16,16,16}, {16,16,16}}";

	static String lastChunkSizes = "{{64,64,64},{64,64,64}}";

	static int lastCompressionChoice = 0;

	static boolean lastCompressionDefaultSettings = true;

	static String lastServerURL = "http://localhost:9080";

	static int lastDataserverTimeout = DEFAULT_DATASERVER_TIMEOUT;

	static String lastLabel = "";

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
				final Object cSpimData = gd.getStringFields().lastElement();
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
				gd.addStringField("Label", lastLabel, 25);
				gd.addStringField("Timeout for Dataserver[s]", "" +
					lastDataserverTimeout, 25);

				tryLoadSPIMData(lastSPIMdata);
				if (autoChunkSizes != null && autoSubsampling != null && !cManualMipmap
					.getState())
				{
					tfChunkSizes.setText(autoChunkSizes);
					tfSubsampling.setText(autoSubsampling);
				}

				gd.addDialogListener((dialog, e) -> {
					tryLoadSPIMData(gd.getNextString());
					gd.getNextBoolean();
					gd.getNextString();
					gd.getNextString();
					gd.getNextChoiceIndex();
					gd.getNextBoolean();
					gd.getNextString();
					gd.getNextString();
					
					if (autoSubsampling != null && autoChunkSizes != null &&
						e instanceof ItemEvent && e
							.getID() == ItemEvent.ITEM_STATE_CHANGED && (e
								.getSource() == cManualMipmap || e.getSource() == cSpimData))
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
				lastLabel = gd.getNextString();
				String timeoutValue = gd.getNextString();
				try {
					lastDataserverTimeout = Integer.parseInt(timeoutValue);
				}
				catch (NumberFormatException x) {
					IJ.showMessage("Cannot parse lastTimeout " + timeoutValue);
					continue;
				}
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
					serverURL, compression, SPIMData, lastDataserverTimeout, lastLabel);
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
