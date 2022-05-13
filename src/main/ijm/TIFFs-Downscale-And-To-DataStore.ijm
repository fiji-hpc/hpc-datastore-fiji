#@ String(persistkey="datasetserverurl") host_port
#@ String(persistkey="datasetdatasetid") dataset
#@ String(description="Use single number like 0,1,2 or keywords latest or new") version = "latest"

#@ int(min="0") uploadIntoThisChannel
#@ int(min="0") uploadIntoThisAngle
#@ int(min="0") uploadTimePoint_FROM
#@ int(min="0") uploadTimePoint_TILL

#@ String(description="The part of file name before the time-point running numbers", default="img") fileNameBeforeTPNumber
#@ int(description="Pad running numbers with zeros up to the given width, set to 0 for no padding.", min="0", default="0") zeroPaddingWidth
#@ String(description="The part of file name right after the time-point running numbers", default=".tif") fileNameAfterTPNumber = ".tif"
#@ File(style="directory", description="Folder with the above defined files") inputDir

#@ boolean verboseUpload = false
#@ int(min="0", label="Timeout in seconds, e.g. 60") timeout

//fixup boolean: 1 -> true
if (verboseUpload == 1) { verboseUpload = "true"; }

function padding(number, width) {
	if (width > 0) {
		numStr = "00000000000000000000"+number;
		return substring(numStr, numStr.length-width);
	} else {
		return number;
	}
}

function justSaveAsLevel(resLevelX,resLevelY,resLevelZ) {
	run("Write full image", "url="+host_port+" datasetid="+dataset+" timepoint="+t+" channel="+uploadIntoThisChannel+" angle="+uploadIntoThisAngle+" resolutionlevelsasstr=[["+resLevelX+", "+resLevelY+", "+resLevelZ+"]] versionasstr="+version+" timeout="+(timeout*1000)+" verboselog="+verboseUpload);
}

function scaleAndSave(refMainImg, resLevelX,resLevelY,resLevelZ) {
	xFac = 1/resLevelX;
	yFac = 1/resLevelY;
	zFac = 1/resLevelZ;
	selectImage(refMainImg);
	run("Scale...", "x="+xFac+" y="+yFac+" z="+zFac+" interpolation=None process create");
	justSaveAsLevel(resLevelX,resLevelY,resLevelZ);
	close();
	//NB: close() is counterpart to "create" in the run() above
}

print("Uploading started....");
for (t = uploadTimePoint_FROM; t <= uploadTimePoint_TILL; t++) {
	filename = inputDir+"/"+fileNameBeforeTPNumber+padding(t,zeroPaddingWidth)+fileNameAfterTPNumber;
	print("Doing TP "+t+", that is a file: "+filename);

	open(filename);
	mainImgID = getImageID();

	print("Saving resolution level 1");
	justSaveAsLevel(1,1,1);

	print("Saving resolution level 2");
	scaleAndSave(mainImgID, 2,2,1);

	print("Saving resolution level 3");
	scaleAndSave(mainImgID, 4,4,1);

	selectImage(mainImgID);
	close();
}
print("Uploading finished....");
