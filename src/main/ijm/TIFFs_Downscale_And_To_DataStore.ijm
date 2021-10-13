#@ String host_port
#@ String dataset
#@ int(min="0") uploadIntoThisChannel
#@ int(min="0") uploadIntoThisVersion
#@ int(min="0") TP_from
#@ int(min="0") TP_till
#@ String(description="The part of file name before the running numbers") fileNameBeforeNumber
#@ int(description="Pad running numbers with zeros up to the given width, set to 0 for no padding.", min="0") zeroPaddingWidth
#@ String(description="The part of file name right after the running numbers") fileNameAfterNumber = ".tif"
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
	run("Write From Image", "url="+host_port+" datasetid="+dataset+" versionasstr="+uploadIntoThisVersion+" resolutionlevelsasstr=[["+resLevelX+", "+resLevelY+", "+resLevelZ+"]] minx=0 maxx=99999 miny=0 maxy=99999 minz=0 maxz=99999 timepoint="+t+" channel="+uploadIntoThisChannel+" angle=0 timeout="+(timeout*1000)+" verboselog="+verboseUpload+" showruncmd=false");
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
for (t = TP_from; t <= TP_till; t++) {
	filename = inputDir+"/"+fileNameBeforeNumber+padding(t,zeroPaddingWidth)+fileNameAfterNumber;
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
