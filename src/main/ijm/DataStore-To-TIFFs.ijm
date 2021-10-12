#@ String host_port
#@ String dataset
#@ int(min="0") downloadFromThisChannel
#@ int(min="0") TP_from
#@ int(min="0") TP_till
#@ String(description="The part of file name before the running numbers") fileNameBeforeNumber
#@ int(description="Pad running numbers with zeros up to the given width, set to 0 for no padding.", min="0") zeroPaddingWidth
#@ String(description="The part of file name right after the running numbers") fileNameAfterNumber = ".tif"
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
for (t = TP_from; t <= TP_till; t++) {
	filename = outputDir+"/"+fileNameBeforeNumber+padding(t,zeroPaddingWidth)+fileNameAfterNumber;
	print("Doing TP "+t+", that is a file: "+filename);

	run("Read Into Image", "url="+host_port+" datasetid="+dataset+" versionasstr=0 resolutionlevelsasstr=[[1, 1, 1]] minx=0 maxx=99999 miny=0 maxy=99999 minz=0 maxz=99999 timepoint="+t+" channel="+downloadFromThisChannel+" angle=0 timeout="+(timeout*1000)+" verboselog="+verboseDownload+" showruncmd=false");
	//run("Read Full Image", "url="+host_port+" datasetid="+dataset+" timepoint="+t+" channel=0 angle=0 resolutionlevelsasstr=[[1, 1, 1]] versionasstr=0 timeout=120000 verboselog="+verboseDownload);

	saveAs("Tiff", filename);
	close();
}
print("Downloading finished....");
