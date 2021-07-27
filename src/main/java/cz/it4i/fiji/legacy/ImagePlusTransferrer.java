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
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import cz.it4i.fiji.legacy.util.Imglib2Types;

public class ImagePlusTransferrer extends ImagePlusDialogHandler {

	public <T extends NativeType<T> & RealType<T>> Dataset readWithAType() {
		//future return value
		Dataset outDatasetImg = null;

		try {
			final Imglib2Types.TypeHandler<T> th = Imglib2Types.getTypeHandler(di.voxelType);
			final Img<T> img = th.createPlanarImgFactory().create(maxX-minX+1,maxY-minY+1,maxZ-minZ+1);

			final InputStream dataSrc = new URL(requestDatasetServer()).openStream();
			final byte[] header = new byte[12];

			//one shared read buffer to be re-used (to reduce calls to the operator 'new')
			byte[] pxData = new byte[0];

			//the expected block sizes for sanity checking of the incoming blocks
			final int[] blockSize = currentResLevel.blockDimensions.stream().mapToInt(i->i).toArray();
			final int[] shortedBlockSize = new int[3];
			for (int d = 0; d < 3; ++d) {
				shortedBlockSize[d] = currentResLevel.dimensions[d] % blockSize[d];
				if (shortedBlockSize[d] == 0) shortedBlockSize[d] = blockSize[d];
			}
			final boolean[] shortedBlockFlag = new boolean[3];
			myLogger.info("inner block sizes: "+blockSize[0]+","+blockSize[1]+","+blockSize[2]);
			myLogger.info(" last block sizes: "+shortedBlockSize[0]+","+shortedBlockSize[1]+","+shortedBlockSize[2]);

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
						final ByteBuffer bb = ByteBuffer.wrap(header);
						final int bx = bb.getInt();
						final int by = bb.getInt();
						final int bz = bb.getInt();
						final int blockLength = bx*by*bz * th.nativeAndRealType.getBitsPerPixel()/8;

						myLogger.info(" +- block says size: "+bx+" x "+by+" x "+bz
								+ " -> "+blockLength+" Bytes");

						//check if the incoming block size fits the currently expected block size
						checkBlockSizeAndPassOrThrow(bx,ex,'x');
						checkBlockSizeAndPassOrThrow(by,ey,'y');
						checkBlockSizeAndPassOrThrow(bz,ez,'z');

						//make sure the buffer can accommodate the incoming data
						if (blockLength > pxData.length)
							pxData = new byte[blockLength];

						//(eventually) read the block fully
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

						//copy the just-stored block content into the image
						th.blockIntoImgInterval(pxData, bx*by*bz, Views.interval(img,
								new long[]{x-minX,      y-minY,      z-minZ},
								new long[]{x-minX+bx-1, y-minY+by-1, z-minZ+bz-1}));
					}

			outDatasetImg = new DefaultDataset(this.getContext(),
					new ImgPlus<T>(img,"Retrieved image at "+timepoints+","+channels+","+angles) );
			myLogger.info("Created image: \""+outDatasetImg.getName()+"\"");

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

	public <T extends NativeType<T> & RealType<T>> void writeWithAType(final Dataset inDatasetImg) {
		try {
			final Imglib2Types.TypeHandler<T> th = Imglib2Types.getTypeHandler(di.voxelType);
			final Img<T> img = th.createPlanarImgFactory().create(maxX-minX+1,maxY-minY+1,maxZ-minZ+1);

			final InputStream dataSrc = new URL(requestDatasetServer()).openStream();
			final byte[] header = new byte[12];

			//one shared read buffer to be re-used (to reduce calls to the operator 'new')
			byte[] pxData = new byte[0];

			//the expected block sizes for sanity checking of the incoming blocks
			final int[] blockSize = currentResLevel.blockDimensions.stream().mapToInt(i->i).toArray();
			final int[] shortedBlockSize = new int[3];
			for (int d = 0; d < 3; ++d) {
				shortedBlockSize[d] = currentResLevel.dimensions[d] % blockSize[d];
				if (shortedBlockSize[d] == 0) shortedBlockSize[d] = blockSize[d];
			}
			final boolean[] shortedBlockFlag = new boolean[3];
			myLogger.info("inner block sizes: "+blockSize[0]+","+blockSize[1]+","+blockSize[2]);
			myLogger.info(" last block sizes: "+shortedBlockSize[0]+","+shortedBlockSize[1]+","+shortedBlockSize[2]);

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
						final ByteBuffer bb = ByteBuffer.wrap(header);
						final int bx = bb.getInt();
						final int by = bb.getInt();
						final int bz = bb.getInt();
						final int blockLength = bx*by*bz * th.nativeAndRealType.getBitsPerPixel()/8;

						myLogger.info(" +- block says size: "+bx+" x "+by+" x "+bz
								+ " -> "+blockLength+" Bytes");

						//check if the incoming block size fits the currently expected block size
						checkBlockSizeAndPassOrThrow(bx,ex,'x');
						checkBlockSizeAndPassOrThrow(by,ey,'y');
						checkBlockSizeAndPassOrThrow(bz,ez,'z');

						//make sure the buffer can accommodate the incoming data
						if (blockLength > pxData.length)
							pxData = new byte[blockLength];

						//(eventually) read the block fully
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

						//copy the just-stored block content into the image
						th.blockIntoImgInterval(pxData, bx*by*bz, Views.interval(img,
								new long[]{x-minX,      y-minY,      z-minZ},
								new long[]{x-minX+bx-1, y-minY+by-1, z-minZ+bz-1}));
					}

/*
			outDatasetImg = new DefaultDataset(this.getContext(),
					new ImgPlus<T>(img,"Retrieved image at "+timepoints+","+channels+","+angles) );
			myLogger.info("Created image: \""+outDatasetImg.getName()+"\"");
*/

		} catch (NoSuchElementException e) {
			myLogger.error("Unrecognized voxel type: " + e.getMessage());
			this.cancel("Unrecognized voxel type: " + e.getMessage());
		} catch (IOException | InterruptedException | IllegalStateException e) {
			myLogger.error("Problem accessing the dataset: "+e.getMessage());
			this.cancel("Problem accessing the dataset: "+e.getMessage());
		}
		myLogger.info("DONE writing image.");
	}
}
