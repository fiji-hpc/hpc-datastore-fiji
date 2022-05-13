#@ String(persistkey="datasetserverurl") host_port
#@ String(persistkey="datasetdatasetid") dataset
#@ String(description="Use single number like 0,1,2 or keywords latest or mixedLatest") version = "latest"

#@ int(min="0") downloadFromThisChannel
#@ int(min="0") downloadThisAngle
#@ int(min="0") downloadTimePoint_FROM
#@ int(min="0") downloadTimePoint_TILL

#@ String(description="The part of file name before the time-point running numbers", default="img") fileNameBeforeTPNumber
#@ int(description="Pad running numbers with zeros up to the given width, set to 0 for no padding.", min="0", default="0") zeroPaddingWidth
#@ String(description="The part of file name right after the time-point running numbers", default=".tif") fileNameAfterTPNumber = ".tif"
#@ File(style="directory") outputDir

#@ boolean verboseDownload = false
#@ int(min="0", label="Timeout in seconds, e.g. 60") timeout

//fixup boolean: 1 -> true
if (verboseDownload == 1) { verboseDownload = "true"; }

function padding(number, width) {
	if (width > 0) {
		numStr = "00000000000000000000"+number;
		return substring(numStr, numStr.length-width);
	} else {
		return number;
	}
}

print("Downloading started....");
for (t = downloadTimePoint_FROM; t <= downloadTimePoint_TILL; t++) {
	filename = outputDir+"/"+fileNameBeforeTPNumber+padding(t,zeroPaddingWidth)+fileNameAfterTPNumber;
	print("Doing TP "+t+", that is a file: "+filename);

	run("Read full image", "url="+host_port+" datasetid="+dataset+" versionasstr="+version+" timepoint="+t+" channel="+downloadFromThisChannel+" angle="+downloadThisAngle+" resolutionlevelsasstr=[[1, 1, 1]] timeout="+(timeout*1000)+" verboselog="+verboseDownload);

	saveAs("Tiff", filename);
	close();
}
print("Downloading finished....");
