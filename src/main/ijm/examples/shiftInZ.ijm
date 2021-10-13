host_port = "alfeios.fi.muni.cz:9080"
dataset = "e078bd3c-edcf-4918-aed2-216b0ac7f7ed"

TP_from = 1
TP_till = 600

channelDownload = 1
versionDownload = 0

channelUpload = 1
versionUpload = 1

verboseDownload = "false"
verboseUpload = "false"
timeout = 30


function justSaveAsLevel(resLevelX,resLevelY,resLevelZ) {
	run("Write From Image", "url="+host_port+" datasetid="+dataset+" versionasstr="+versionUpload+" resolutionlevelsasstr=[["+resLevelX+", "+resLevelY+", "+resLevelZ+"]] minx=0 maxx=99999 miny=0 maxy=99999 minz=0 maxz=99999 timepoint="+t+" channel="+channelUpload+" angle=0 timeout="+(timeout*1000)+" verboselog="+verboseUpload+" showruncmd=false");
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


for (t = TP_from; t <= TP_till; t++) {
	print("downloading tp="+t);
	run("Read Into Image", "url="+host_port+" datasetid="+dataset+" versionasstr="+versionDownload+" resolutionlevelsasstr=[[1, 1, 1]] minx=0 maxx=99999 miny=0 maxy=99999 minz=0 maxz=99999 timepoint="+t+" channel="+channelDownload+" angle=0 timeout="+(timeout*1000)+" verboselog="+verboseDownload+" showruncmd=false");
	run("Duplicate...", "duplicate");
	mainImgID = getImageID();
	close("\\Others");

	print("moving along z..");
	setSlice(nSlices);
	run("Delete Slice");
	run("Delete Slice");
	run("Delete Slice");
	run("Delete Slice");
	run("Delete Slice");
	run("Delete Slice");

	setSlice(1);
	run("Add Slice");
	run("Add Slice");
	run("Add Slice");
	run("Add Slice");
	run("Add Slice");
	run("Add Slice");

	print("Saving resolution level 1");
	justSaveAsLevel(1,1,1);

	print("Saving resolution level 2");
	scaleAndSave(mainImgID, 2,2,1);

	print("Saving resolution level 3");
	scaleAndSave(mainImgID, 4,4,1);

	selectImage(mainImgID);
	close();
}
