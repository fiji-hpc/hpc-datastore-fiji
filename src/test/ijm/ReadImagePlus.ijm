//example reading a portion from a larger dataset,
//min/max bounds need not be perfectly accurate, the command will round them for its use
run("Read Into Image", "url=192.168.3.130:9080 datasetid=8f0d0593-7f4a-4290-85b2-2890e57ba261 accessregime=read minx=0 maxx=120 miny=0 maxy=63 minz=0 maxz=63 timepoint=3 channel=0 angle=0 resolutionlevelsasstr=[[2, 2, 2]] versionasstr=1 timeout=30000");

//example writing full res & size another version of currently opened image (which must match the size 500x910x488 -- the size used in the target dataset)
run("Write From Image", "url=192.168.3.130:9080 datasetid=8f0d0593-7f4a-4290-85b2-2890e57ba261 accessregime=write minx=0 maxx=499 miny=0 maxy=909 minz=0 maxz=487 timepoint=5 channel=0 angle=0 resolutionlevelsasstr=[[1, 1, 1]] versionasstr=3 timeout=25000");
