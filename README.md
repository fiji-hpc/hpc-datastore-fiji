#HPC datastore
[Google docs description](https://docs.google.com/document/d/1ZeLc83dyNE9USBuvSCLEVGK-zQzUKFb7VGhOlVIRBvU/edit)

# Start server
Run class _cz.it4i.fiji.datastore.App_. It accept these properties (passed as -D<property.name>)
- datastore.path
- quarkus.http.port
- quarkus.http.host

or run start-server <datastore.path> <hostname> <portnumber>

# Run client from Fiji
Add an update site https://sites.imagej.net/Imglib2-labkit-cluster/
Run Plugins>BigDataViewer>Export SPIM data as remote XML/N5

