//make sure this function closes 'titleOfExistingInputImage', and leaves
//no other window left behind except for 'titleOfFutureOutputImage' with the result
function process2DImage(titleOfExistingInputImage, titleOfFutureOutputImage) {
	selectImage(titleOfExistingInputImage);

	// Upsample the slice for the segmentation
	run("Scale...", "x=2 y=2 width=" + 2*width + " height=" + 2*height + " interpolation=Bicubic average create title=[upsampledImg]");
	close(titleOfExistingInputImage);

	//the segmentation....
	run("DeepImageJ Run", "model=StarDist_Jul28 format=Tensorflow preprocessing=[per_sample_scale_range.ijm] postprocessing=[StarDist_Post-processing.ijm] axes=Y,X,C tile=2112,1216,1 logging=normal");
	rename("res_atFullRes");
	close("upsampledImg");

	// Downsample the segmentation
	run("Scale...", "x=0.5 y=0.5 width=" + width + " height=" + height + " interpolation=None create title="+titleOfFutureOutputImage);
	close("res_atFullRes");
}


function process3DImagePerSlices(refImage) {
	//make sure the given is the active one
	selectImage(refImage);

	//duplicate out if it just one slice to which we gonna be appending results
	run("Duplicate...", "title=refOutcome");

	selectImage(refImage);
	getDimensions(width, height, slices, channels, frames);
	print("going to process "+slices+" slices");

	for (i = 0; i < slices; i++) {
		//extract the current slice
		selectImage(refImage);
		run("Make Substack...", "slices=" + (i+1));
		rename("oneSliceSource");

		print(">>>>> "+(i+1)+". image processing now....");
		process2DImage("oneSliceSource","oneSliceRes");

		//inject the current (and processed) slide
		run("Concatenate...", "  title=refOutcome image1=refOutcome image2=oneSliceRes image3=[-- None --]");
	}

	//remove the very first slice from the outcome stack, the original "refOutcome" slice
	run("Make Substack...", "slices=2-" + (slices+1));
	close("refOutcome");
}


function process3DImagePerBlockLayers(argListForReadingDatastore, argListForWritingDatastore, zBlockSize, zImageSize) {
	noOfBlocks = Math.ceil(zImageSize/zBlockSize);

	// PARALLELIZE THIS FOR-CYCLE (a node must always process its full block!)
	// for (i = parGetRank(); i < noOfBlocks; i+=parGetSize()) {
	//
	// SERIAL VERSION:
	for (i = 0; i < noOfBlocks; i++) {

		zPosInIthLayerOfBlocks = i*zBlockSize;
		print("===== block at z="+zPosInIthLayerOfBlocks);
		//NB: just compute z-pos of _any_ pixel in this z-layer of blocks,
		//    and enjoy that the macro takes care of appropriate rounding to blocks boundaries

		run("Read Into Image",  argListForReadingDatastore+" minz="+zPosInIthLayerOfBlocks+" maxz="+zPosInIthLayerOfBlocks);
		process3DImagePerSlices( getTitle() );
		run("Write From Image", argListForWritingDatastore+" minz="+zPosInIthLayerOfBlocks+" maxz="+zPosInIthLayerOfBlocks);

		close(); //closes the result window
		close(); //closes the input window
	}
}

//process3DImagePerSlices("test")

process3DImagePerBlockLayers("url=helenos.fi.muni.cz:9080 datasetid=dba15297-37a2-4cf1-94fc-147708ae9de2 minx=0 maxx=99999 miny=0 maxy=99999 timepoint=0 channel=0 angle=0 resolutionlevelsasstr=[[1, 1, 1]] versionasstr=0      timeout=180000 verboselog=false showruncmd=false",
                             "url=helenos.fi.muni.cz:9080 datasetid=dba15297-37a2-4cf1-94fc-147708ae9de2 minx=0 maxx=99999 miny=0 maxy=99999 timepoint=0 channel=0 angle=0 resolutionlevelsasstr=[[1, 1, 1]] versionasstr=latest timeout=180000 verboselog=false showruncmd=false",
                             20, 975); //zBlockSize, totalChosenRes_zImageSizeAtTotalChosenRes
//NB: for now, it stores the output as another version of the input data
//    for later, it should store the output into another channel next to the input data
//
//NB: the plugin automatically adjusts min/max values to the appropriate correct block boundaries, here it does so for the x and y spans,
//    inside process3DImagePerBlockLayers() we use it for the z axis

print("DONE");
