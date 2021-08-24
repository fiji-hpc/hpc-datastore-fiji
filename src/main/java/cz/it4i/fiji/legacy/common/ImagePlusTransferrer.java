package cz.it4i.fiji.legacy.common;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.view.Views;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import cz.it4i.fiji.legacy.util.Imglib2Types;
import cz.it4i.fiji.legacy.util.TimeProfiling;

public class ImagePlusTransferrer extends ImagePlusDialogHandler {

	// note: this class is bound to a dialog, and a dialog is taking care
	// of one transfer; if there is another consequent transfer requested via
	// the dialog, it will happen via a separate object of this class...
	// that was to say, this class is not designed to be re-entrant
	// ('cause it needs not to be)

	// ----------------------------------------------
	// common attributes to transfers, irrespective of the transfer direction:
	final int[] blockSize = new int[3];                  //x,y,z size of a normal/inner block
	final int[] shortedBlockSize = new int[3];           //x,y,z size of a block in the diagonal corner
	final boolean[] shortedBlockFlag = new boolean[3];   //aux flag to signal if a block is inner/edge for each spatial axis

	void setupBlockSizes(final Imglib2Types.TypeHandler<?> th) {
		for (int d = 0; d < 3; ++d) {
			blockSize[d] = currentResLevel.blockDimensions.get(d);
			shortedBlockSize[d] = currentResLevel.dimensions[d] % blockSize[d];
			if (shortedBlockSize[d] == 0) shortedBlockSize[d] = blockSize[d];
		}
		fullBlockByteSize = blockSize[0]*blockSize[1]*blockSize[2] * th.nativeAndRealType.getBitsPerPixel()/8;

		myLogger.info("inner block sizes: "+blockSize[0]+","+blockSize[1]+","+blockSize[2]
				+" ("+fullBlockByteSize+" Bytes)");
		myLogger.info(" last block sizes: "+shortedBlockSize[0]+","+shortedBlockSize[1]+","+shortedBlockSize[2]);
	}

	protected void checkBlockSizeAndPassOrThrow(final int given, final int expected, final char axis)
	throws IllegalStateException {
		if (given != expected)
			throw new IllegalStateException("Got block of "+axis+"-size "+given+" px that does not match "
					+ "the expected block size "+expected+" px.");
	}

	// ----------------------------------------------
	// transfer controls
	int fullBlockByteSize;
	int maxOneReadTransferByteSize  = 1 << 27; //128 MB
	int maxOneWriteTransferByteSize = 1 << 23; //8 MB
	//NB: server fails to receive larger, consider using here HttpURLConnection.setFixedLengthStreamingMode

	static class OneTransfer {
		public OneTransfer(final String URL, final int noOfBlocks) {
			this.URL = URL;
			this.noOfBlocks = noOfBlocks;
		}
		public final String URL;
		public final int noOfBlocks;

		@Override
		public String toString() {
			return (this.noOfBlocks+" blocks from "+this.URL);
		}
	}
	final List<OneTransfer> transferPlan = new LinkedList<>();

	void setupTransferPlan(final int maxTransferByteSize) {
		final String baseURL = requestDatasetServer();
		int maxBlocks = maxTransferByteSize / fullBlockByteSize; //to prevent from long transfers (and long URIs)
		if (maxBlocks > 256) maxBlocks = 256;                    //to prevent from long URIs

		if (maxBlocks == 0)
			throw new IllegalStateException("Given max transfer size "+maxTransferByteSize
					+" Bytes cannot host blocks of max size "+fullBlockByteSize+" Bytes");

		transferPlan.clear();
		StringBuilder currentURL = null;
		int currentBlocksCnt = maxBlocks;

		//iterate over the blocks and build up the request URLs
		for (int z = minZ; z <= maxZ; z += blockSize[2])
			for (int y = minY; y <= maxY; y += blockSize[1])
				for (int x = minX; x <= maxX; x += blockSize[0]) {
					//time to start a new URL?
					if (currentBlocksCnt == maxBlocks) {
						//save old?
						if (currentURL != null)
							transferPlan.add( new OneTransfer(currentURL.toString(),currentBlocksCnt) );

						//start new
						currentURL = new StringBuilder(baseURL);
						currentBlocksCnt = 0;
					}

					currentURL.append(x/blockSize[0]+"/"
							+ y/blockSize[1]+"/"
							+ z/blockSize[2]+"/"
							+ timepoint+"/"
							+ channel+"/"
							+ angle+"/");
					++currentBlocksCnt;
				}

		//add also the last one
		transferPlan.add( new OneTransfer(currentURL.toString(),currentBlocksCnt) );
	}

	void printTransferPlan() {
		for (OneTransfer t : transferPlan)
			myLogger.info("Planning "+t);
	}


	// ----------------------------------------------
	public <T extends NativeType<T> & RealType<T>>
	Dataset readWithAType() {
		//future return value
		Dataset outDatasetImg = null;

		try {
			final Imglib2Types.TypeHandler<T> th = Imglib2Types.getTypeHandler(di.voxelType);
			final Img<T> img = th.createPlanarImgFactory().create(maxX-minX+1,maxY-minY+1,maxZ-minZ+1);

			//the expected block sizes for sanity checking of the incoming blocks
			setupBlockSizes(th);
			setupTransferPlan(maxOneReadTransferByteSize);
			printTransferPlan();

			//shared buffers to be re-used (to reduce calls to the operator 'new')
			byte[] pxData = new byte[0];
			final byte[] header = new byte[12];
			final ByteBuffer wrapperOfHeader = ByteBuffer.wrap(header);

			long totalHeaders = 0;
			long totalData = 0;

			InputStream dataSrc = null;
			int remainingBlocks = 0;

			long timeTotal = TimeProfiling.tic();

			//iterate over the blocks and read them in into the image
			for (int z = minZ; z <= maxZ; z += blockSize[2])
				for (int y = minY; y <= maxY; y += blockSize[1])
					for (int x = minX; x <= maxX; x += blockSize[0]) {
						//start a new data transfer connection
						if (remainingBlocks == 0) {
							//earlier connection? -> finish it up
							if (dataSrc != null) dataSrc.close();
							//NB: might close/clean-up the connection completely

							OneTransfer t = transferPlan.remove(0);
							myLogger.info("=========================");
							myLogger.info("Downloading "+t);
							dataSrc = new URL(t.URL).openStream();
							remainingBlocks = t.noOfBlocks;
						}
						--remainingBlocks;

						//calculate the expected sizes of the current block
						shortedBlockFlag[0] = x+blockSize[0] > maxX+1;
						shortedBlockFlag[1] = y+blockSize[1] > maxY+1;
						shortedBlockFlag[2] = z+blockSize[2] > maxZ+1;
						final int ex = shortedBlockFlag[0] ? shortedBlockSize[0] : blockSize[0];
						final int ey = shortedBlockFlag[1] ? shortedBlockSize[1] : blockSize[1];
						final int ez = shortedBlockFlag[2] ? shortedBlockSize[2] : blockSize[2];

						myLogger.info("block at ["+x+","+y+","+z+"] px");
						myLogger.info(" +- shorted block in x,y,z: "
								+ shortedBlockFlag[0]+","+shortedBlockFlag[1]+","+shortedBlockFlag[2]);
						myLogger.info(" +- I  expect  size: "+ex+" x "+ey+" x "+ez);

						//retrieve the block header (which contains block size)
						int readSoFar = 0;
						while (readSoFar < 12) {
							//make sure data is available, and read only afterwards
							busyWaitOrThrowOnTimeOut(dataSrc);
							readSoFar += dataSrc.read(header,readSoFar,12-readSoFar);
						}
						wrapperOfHeader.rewind();
						final int bx = wrapperOfHeader.getInt();
						final int by = wrapperOfHeader.getInt();
						final int bz = wrapperOfHeader.getInt();
						final int blockLength = bx*by*bz * th.nativeAndRealType.getBitsPerPixel()/8;

						myLogger.info(" +- block says size: "+bx+" x "+by+" x "+bz
								+ " -> "+blockLength+" Bytes");
						totalHeaders += 12;

						if (bx == -1 && by == -1 && bz == -1) {
							//server signals that this block is missing, we skip it for now...
							myLogger.info(" -> skipped");
							continue;
						}

						//check if the incoming block size fits the currently expected block size
						checkBlockSizeAndPassOrThrow(bx,ex,'x');
						checkBlockSizeAndPassOrThrow(by,ey,'y');
						checkBlockSizeAndPassOrThrow(bz,ez,'z');

						//make sure the buffer can accommodate the incoming data
						if (blockLength > pxData.length)
							pxData = new byte[blockLength];

						//(eventually) read the buffer (aka block) fully
						readSoFar = 0;
						while (readSoFar < blockLength) {
							busyWaitOrThrowOnTimeOut(dataSrc);
							readSoFar += dataSrc.read(pxData,readSoFar,blockLength-readSoFar);
						}
						myLogger.info(" +- read "+readSoFar+" Bytes");
						totalData += readSoFar;

						//copy the just-obtained buffer into the image block
						th.blockIntoImgInterval(pxData, bx*by*bz, Views.interval(img,
								new long[]{x-minX,      y-minY,      z-minZ},
								new long[]{x-minX+bx-1, y-minY+by-1, z-minZ+bz-1}));
					}
			myLogger.info("Whole transfer took "
					+TimeProfiling.seconds(TimeProfiling.tac(timeTotal))
					+" seconds.");

			outDatasetImg = new DefaultDataset(this.getContext(),
					new ImgPlus<>(img,"Retrieved image at "+timepoint+","+channel+","+angle) );
			myLogger.info("Created image: \""+outDatasetImg.getName()+"\"");
			myLogger.info("Transferred "+totalData+" Bytes ("+(totalData>>20)
					+" MB) in pixels plus "+totalHeaders+" Bytes in headers");

		} catch (NoSuchElementException e) {
			myLogger.error("Unrecognized voxel type: " + e.getMessage());
			this.cancel("Unrecognized voxel type: " + e.getMessage());
		} catch (IOException | InterruptedException | IllegalStateException e) {
			myLogger.error("Problem accessing the dataset: "+e.getMessage());
			this.cancel("Problem accessing the dataset: "+e.getMessage());
		}

		myLogger.info("DONE reading image.");
		return outDatasetImg;
	}


	public <T extends NativeType<T> & RealType<T>>
	void writeWithAType(final Dataset inDatasetImg) {
		try {
			final Imglib2Types.TypeHandler<T> th = Imglib2Types.getTypeHandler(inDatasetImg.getType());
			final Img<T> img = (Img)inDatasetImg.getImgPlus().getImg();

			//sanity check already at the client
			final Imglib2Types.TypeHandler<T> thServer = Imglib2Types.getTypeHandler(di.voxelType);
			if (!thServer.nativeAndRealType.equals(th.nativeAndRealType))
				throw new IllegalArgumentException("Connecting to a server for a type "+thServer.httpType
						+" with a Dataset of a type "+th.httpType);

			//the expected block sizes for reporting
			setupBlockSizes(th);
			setupTransferPlan(maxOneWriteTransferByteSize);
			printTransferPlan();

			//shared buffers to be re-used (to reduce calls to the operator 'new')
			byte[] pxData = new byte[0];
			final byte[] header = new byte[12];
			final ByteBuffer wrapperOfHeader = ByteBuffer.wrap(header); //convenience wrapper to write block sizes into a byte array

			long totalHeaders = 0;
			long totalData = 0;

			HttpURLConnection connection = null;
			OutputStream dataTgt = null;
			int remainingBlocks = 0;

			long timeTotal = TimeProfiling.tic();

			//iterate over the blocks and read them in into the image
			for (int z = minZ; z <= maxZ; z += blockSize[2])
				for (int y = minY; y <= maxY; y += blockSize[1])
					for (int x = minX; x <= maxX; x += blockSize[0]) {
						//start a new data transfer connection
						if (remainingBlocks == 0) {
							//earlier connection? -> finish it up
							if (connection != null) {
								myLogger.info("=== transferring starts");
								connection.getInputStream();
								myLogger.info("=== transferring ends");
								dataTgt.close(); //might close/clean-up the connection completely
							}

							OneTransfer t = transferPlan.remove(0);
							myLogger.info("=========================");
							myLogger.info("Uploading "+t);
							connection = (HttpURLConnection) new URL(t.URL).openConnection();
							connection.setRequestMethod("POST");
							connection.setRequestProperty("Content-Type","application/octet-stream"); //to prevent from 415 err code (Unsupported Media Type)
							connection.setDoOutput(true);
							connection.connect();
							dataTgt = connection.getOutputStream();
							remainingBlocks = t.noOfBlocks;
						}
						--remainingBlocks;

						//calculate the expected sizes of the current block
						shortedBlockFlag[0] = x+blockSize[0] > maxX+1;
						shortedBlockFlag[1] = y+blockSize[1] > maxY+1;
						shortedBlockFlag[2] = z+blockSize[2] > maxZ+1;
						final int ex = shortedBlockFlag[0] ? shortedBlockSize[0] : blockSize[0];
						final int ey = shortedBlockFlag[1] ? shortedBlockSize[1] : blockSize[1];
						final int ez = shortedBlockFlag[2] ? shortedBlockSize[2] : blockSize[2];
						final int blockLength = ex*ey*ez * th.nativeAndRealType.getBitsPerPixel()/8;

						myLogger.info("block at ["+x+","+y+","+z+"] px");
						myLogger.info(" +- shorted block in x,y,z: "
								+ shortedBlockFlag[0]+","+shortedBlockFlag[1]+","+shortedBlockFlag[2]);
						myLogger.info(" +- I report size: "+ex+" x "+ey+" x "+ez
								+ " -> "+blockLength+" Bytes");

						//write the block header (which contains block size)
						wrapperOfHeader.rewind();
						wrapperOfHeader.putInt(ex);
						wrapperOfHeader.putInt(ey);
						wrapperOfHeader.putInt(ez);
						try {
							dataTgt.write(header, 0, 12);
						} catch (IOException e) {
							throw new IOException("Failed writing full block header",e);
						}
						totalHeaders += 12;

						//make sure the buffer can accommodate the outgoing data
						if (blockLength > pxData.length)
							pxData = new byte[blockLength];

						//copy the current image block into the buffer
						th.imgIntervalIntoBlock( Views.interval(img,
								new long[]{x-minX,      y-minY,      z-minZ},
								new long[]{x-minX+ex-1, y-minY+ey-1, z-minZ+ez-1}),
								ex*ey*ez, pxData);

						//(eventually) write the buffer (aka block) fully
						dataTgt.write(pxData,0,blockLength);
						//dataTgt.flush(); leave this decision on the subsystems...
						myLogger.info(" +- wrote "+blockLength+" Bytes");
						totalData += blockLength;
					}
			myLogger.info("Whole transfer took "
					+TimeProfiling.seconds(TimeProfiling.tac(timeTotal))
					+" seconds.");
			myLogger.info("=== transferring "+totalData+" Bytes ("+(totalData>>20)
					+" MB) in pixels plus "+totalHeaders+" Bytes in headers");
			connection.getInputStream();
			myLogger.info("=== transferring ends");

		} catch (NoSuchElementException e) {
			myLogger.error("Unrecognized voxel type: " + e.getMessage());
			this.cancel("Unrecognized voxel type: " + e.getMessage());
		} catch (IOException | IllegalStateException e) {
			myLogger.error("Problem accessing the dataset: "+e.getMessage());
			this.cancel("Problem accessing the dataset: "+e.getMessage());
		}
		myLogger.info("DONE writing image.");
	}


	private void busyWaitOrThrowOnTimeOut(final InputStream dataSrc)
	throws IOException, InterruptedException {
		int tries = 0;
		long waitTime = 20;

		while (dataSrc.available() == 0 && tries < 10) {
			Thread.sleep(waitTime);
			waitTime += 0.84*waitTime; //...nearly doubling-waiting time
			++tries;
		}
		if (dataSrc.available() == 0)
			throw new IOException("Gave up waiting for incoming data");
	}


	// ----------------------------------------------
	/** starts DatasetServer and returns an URL on it, or null if something has failed */
	protected String requestDatasetServer() {
		myLogger.info("Going to deal with a legacy ImageJ image:");
		myLogger.info("["+minX+"-"+maxX+"] x "
				+ "["+minY+"-"+maxY+"] x "
				+ "["+minZ+"-"+maxZ+"], that is "
				+ (maxX-minX+1)+" x "+(maxY-minY+1)+" x "+(maxZ-minZ+1)+" pixels,");
		myLogger.info("  at "+timepoint+","+channel+","+angle
				+ " timepoint,channel,angle");
		myLogger.info("  at "+currentResLevel);
		myLogger.info("  at version "+versionAsStr);
		myLogger.info("from dataset "+datasetID+" from "+URL+" for "+accessRegime);

		final StringBuilder urlFirstGo = new StringBuilder();
		urlFirstGo.append("http://"+URL+"/datasets/"+datasetID+"/");
		for (int dim=0; dim < 3; ++dim)
			urlFirstGo.append(currentResLevel.resolutions.get(dim)+"/");
		urlFirstGo.append(versionAsStr+"/"+accessRegime+"?timeout="+timeout);
		myLogger.info("1: "+urlFirstGo);

		try {
			//connect to get the new URL for the blocks-server itself
			final URLConnection connection = new URL(urlFirstGo.toString()).openConnection();
			connection.getInputStream(); //this enables access to the redirected URL
			final String urlSecondGo = connection.getURL().toString();
			myLogger.info("2: "+urlSecondGo);
			return urlSecondGo;
		} catch (IOException e) {
			myLogger.error(e.getMessage());
			return null;
		}
	}
}
