/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.management;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import lombok.extern.log4j.Log4j2;

@Log4j2
@ApplicationScoped
public class DataServerManager {

	private static final String APP_CLASS = "cz.it4i.fiji.datastore.App";

	public URL startDataServer(UUID uuid, String version) throws IOException {
		Integer port = findRandomOpenPortOnAllLocalInterfaces();
		ProcessBuilder pb = new ProcessBuilder().inheritIO();
		pb.command("java", "-cp", System.getProperty("java.class.path"),
			"-Dquarkus.http.port=" + port, "-Dfiji.hpc.data_store.uuid=" + uuid,
			"-Dfiji.hpc.data_store.version=" + version, APP_CLASS);
		Process p = pb.start();
		String result = String.format("http://%s:%d/", getHostName(), port);
		try {
			p.waitFor();

		}
		catch (InterruptedException exc) {
			p.destroyForcibly();
		}
		return new URL(result);
	}

	private String getHostName() throws UnknownHostException {
		String hostName = System.getProperty("quarkus.http.host", "localhost");
		if (hostName.equals("0.0.0.0")) {
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		}
		return hostName;
	}

	private static Integer findRandomOpenPortOnAllLocalInterfaces()
		throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	public static void main(String[] args) throws IOException {
		new DataServerManager().startDataServer(UUID.randomUUID(), "latest");
		log.info("free port {}", findRandomOpenPortOnAllLocalInterfaces());
	}
}
