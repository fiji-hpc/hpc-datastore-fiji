package cz.it4i.fiji.legacy;

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
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import cz.it4i.fiji.legacy.util.Imglib2Types;

public class ImagePlusTransferrer extends ImagePlusDialogHandler {

	// note: this class is bound to a dialog, and a dialog is taking care
	// of one transfer; if there is another consequent transfer requested via
	// the dialog, it will happen via a separate object of this class...
	// that was to say, this class is not designed to be re-entrant
	// ('cause it needs not to be)

	// common attributes to transfers, irrespective of the transfer direction:
	final int[] blockSize = new int[3];                  //x,y,z size of a normal/inner block
	final int[] shortedBlockSize = new int[3];           //x,y,z size of a block in the diagonal corner
	final boolean[] shortedBlockFlag = new boolean[3];   //aux flag to signal if a block is inner/edge for each spatial axis

	private void setupBlockSizes() {
		for (int d = 0; d < 3; ++d) {
			blockSize[d] = currentResLevel.blockDimensions.get(d);
			shortedBlockSize[d] = currentResLevel.dimensions[d] % blockSize[d];
			if (shortedBlockSize[d] == 0) shortedBlockSize[d] = blockSize[d];
		}
		myLogger.info("inner block sizes: "+blockSize[0]+","+blockSize[1]+","+blockSize[2]);
		myLogger.info(" last block sizes: "+shortedBlockSize[0]+","+shortedBlockSize[1]+","+shortedBlockSize[2]);
	}

	protected void checkBlockSizeAndPassOrThrow(final int given, final int expected, final char axis)
	throws IllegalStateException {
		if (given != expected)
			throw new IllegalStateException("Got block of "+axis+"-size "+given+" px that does not match "
					+ "the expected block size "+expected+" px.");
	}


	public <T extends NativeType<T> & RealType<T>>
	Dataset readWithAType() {
		//future return value
		Dataset outDatasetImg = null;

		try {
			final Imglib2Types.TypeHandler<T> th = Imglib2Types.getTypeHandler(di.voxelType);
			final Img<T> img = th.createPlanarImgFactory().create(maxX-minX+1,maxY-minY+1,maxZ-minZ+1);

			final InputStream dataSrc = new URL(requestDatasetServer()).openStream();

			//shared buffers to be re-used (to reduce calls to the operator 'new')
			byte[] pxData = new byte[0];
			final byte[] header = new byte[12];
			final ByteBuffer wrapperOfHeader = ByteBuffer.wrap(header);

			//the expected block sizes for sanity checking of the incoming blocks
			setupBlockSizes();

			long totalHeaders = 0;
			long totalData = 0;

			//iterate over the blocks and read them in into the image
			for (int z = minZ; z <= maxZ; z += blockSize[2])
				for (int y = minY; y <= maxY; y += blockSize[1])
					for (int x = minX; x <= maxX; x += blockSize[0]) {
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
						if (dataSrc.read(header,0,12) != 12)
							throw new IOException("Failed reading full block header");
						wrapperOfHeader.rewind();
						final int bx = wrapperOfHeader.getInt();
						final int by = wrapperOfHeader.getInt();
						final int bz = wrapperOfHeader.getInt();
						final int blockLength = bx*by*bz * th.nativeAndRealType.getBitsPerPixel()/8;

						myLogger.info(" +- block says size: "+bx+" x "+by+" x "+bz
								+ " -> "+blockLength+" Bytes");
						totalHeaders += 12;

						//check if the incoming block size fits the currently expected block size
						checkBlockSizeAndPassOrThrow(bx,ex,'x');
						checkBlockSizeAndPassOrThrow(by,ey,'y');
						checkBlockSizeAndPassOrThrow(bz,ez,'z');

						//make sure the buffer can accommodate the incoming data
						if (blockLength > pxData.length)
							pxData = new byte[blockLength];

						//(eventually) read the buffer (aka block) fully
						int readSoFar = 0;
						while (readSoFar < blockLength) {
							//wait for data in a semi-busy wait
							int tries = 0;
							if (dataSrc.available() == 0 && tries < 10) {
								Thread.sleep(2000);
								++tries;
							}
							if (dataSrc.available() == 0)
								throw new IOException("Gave up waiting for block data after reading "
										+ readSoFar+" Bytes");

							readSoFar += dataSrc.read(pxData,readSoFar,blockLength-readSoFar);
						}
						myLogger.info(" +- read "+readSoFar+" Bytes");
						totalData += readSoFar;

						//copy the just-obtained buffer into the image block
						th.blockIntoImgInterval(pxData, bx*by*bz, Views.interval(img,
								new long[]{x-minX,      y-minY,      z-minZ},
								new long[]{x-minX+bx-1, y-minY+by-1, z-minZ+bz-1}));
					}

			outDatasetImg = new DefaultDataset(this.getContext(),
					new ImgPlus<>(img,"Retrieved image at "+timepoints+","+channels+","+angles) );
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

			final HttpURLConnection connection = (HttpURLConnection) new URL(requestDatasetServer()).openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","application/octet-stream"); //to prevent from 415 err code (Unsupported Media Type)
			connection.setDoOutput(true);
			connection.connect();
			final OutputStream dataTgt = connection.getOutputStream();

			//shared buffers to be re-used (to reduce calls to the operator 'new')
			byte[] pxData = new byte[0];
			final byte[] header = new byte[12];
			final ByteBuffer wrapperOfHeader = ByteBuffer.wrap(header); //convenience wrapper to write block sizes into a byte array

			//the expected block sizes for reporting
			setupBlockSizes();

			long totalHeaders = 0;
			long totalData = 0;

			//iterate over the blocks and read them in into the image
			for (int z = minZ; z <= maxZ; z += blockSize[2])
				for (int y = minY; y <= maxY; y += blockSize[1])
					for (int x = minX; x <= maxX; x += blockSize[0]) {
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
			myLogger.info("transferring "+totalData+" Bytes ("+(totalData>>20)
					+" MB) in pixels plus "+totalHeaders+" Bytes in headers");
			connection.getInputStream();
			myLogger.info("transferring ends");

		} catch (NoSuchElementException e) {
			myLogger.error("Unrecognized voxel type: " + e.getMessage());
			this.cancel("Unrecognized voxel type: " + e.getMessage());
		} catch (IOException | IllegalStateException e) {
			myLogger.error("Problem accessing the dataset: "+e.getMessage());
			this.cancel("Problem accessing the dataset: "+e.getMessage());
		}
		myLogger.info("DONE writing image.");
	}


	// ========= connect and request image server =========
	/** starts DatasetServer and returns an URL on it, or null if something has failed */
	protected String requestDatasetServer() {
		myLogger.info("Going to deal with a legacy ImageJ image:");
		myLogger.info("["+minX+"-"+maxX+"] x "
				+ "["+minY+"-"+maxY+"] x "
				+ "["+minZ+"-"+maxZ+"], that is "
				+ (maxX-minX+1)+" x "+(maxY-minY+1)+" x "+(maxZ-minZ+1)+" pixels,");
		myLogger.info("  at "+timepoints+","+channels+","+angles
				+ " timepoint,channel,angle");
		myLogger.info("  at "+currentResLevel);
		myLogger.info("  at version "+versionAsStr);
		myLogger.info("from dataset "+datasetID+" from "+URL+" for "+accessRegime);

		final StringBuilder urlFirstGo = new StringBuilder();
		urlFirstGo.append("http://"+URL+"/datasets/"+datasetID+"/");
		for (int dim=0; dim < 3; ++dim)
			urlFirstGo.append(currentResLevel.resolutions.get(dim)+"/");
		urlFirstGo.append(versionAsStr+"/"+accessRegime+"?timeout="+timeout);

		final StringBuilder urlSecondGo = new StringBuilder();
		try {
			//connect to get the new URL for the blocks-server itself
			final URLConnection connection = new URL(urlFirstGo.toString()).openConnection();
			connection.getInputStream(); //this enables access to the redirected URL

			urlSecondGo.append(connection.getURL());

			//iterate over the blocks and build up the request URL
			final int[] blockSize = currentResLevel.blockDimensions.stream().mapToInt(i->i).toArray();
			for (int z = minZ; z <= maxZ; z += blockSize[2])
				for (int y = minY; y <= maxY; y += blockSize[1])
					for (int x = minX; x <= maxX; x += blockSize[0]) {
						urlSecondGo.append(x/blockSize[0]+"/"
								+ y/blockSize[1]+"/"
								+ z/blockSize[2]+"/"
								+ timepoints+"/"
								+ channels+"/"
								+ angles+"/");
					}

			myLogger.info("1: "+urlFirstGo);
			myLogger.info("2: "+urlSecondGo);
		} catch (IOException e) {
			myLogger.error(e.getMessage());
			return null;
		}

		return urlSecondGo.toString();
	}
}
