# Start server
Run class _cz.it4i.fiji.datastore.App_. It accept these properties (passed as -D<property.name>)
- datastore.path
- dataset.version - actually supported version of dataset e.g. "latest"
- quarkus.http.port
- quarkus.http.host

or run start-server <datastore.path> <hostname> <portnumber>

# Run client from Fiji
Add an update site https://sites.imagej.net/Imglib2-labkit-cluster/
Run Plugins>BigDataViewer>Export SPIM data as remote XML/N5
