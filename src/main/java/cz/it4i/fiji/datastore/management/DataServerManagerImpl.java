/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.management;

import io.quarkus.runtime.Quarkus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import cz.it4i.fiji.datastore.register_service.OperationMode;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ApplicationScoped
class DataServerManagerImpl implements DataServerManager {

	private static final String PROPERTY_DATA_STORE_TIMEOUT =
		"fiji.hpc.data_store.timeout";

	private static final int WAIT_FOR_SERVER_TIMEOUT = 200;

	private static final String APP_CLASS = "cz.it4i.fiji.datastore.App";

	private static String PROPERTY_UUID = "fiji.hpc.data_store.uuid";

	private static String PROPERTY_RESOLUTION = "fiji.hpc.data_store.resolution";

	private static String PROPERTY_VERSION = "fiji.hpc.data_store.version";

	private static String PROPERTY_MODE = "fiji.hpc.data_store.mode";

	private Queue<Process> processes = new LinkedBlockingDeque<>();

	private Long dataserverTimeout;

	@Inject
	ApplicationConfiguration applicationConfiguration;

	@Inject
	DatasetRepository datasetRepository;

	@Override
	public URL startDataServer(UUID uuid, int[] r, String version,
		OperationMode mode,
		Long timeout)
		throws IOException
	{
		Optional<Dataset> dataset = datasetRepository.findByUUID(uuid);
		if (!dataset.isPresent()) {
			throw new IllegalArgumentException("Dataset with uuid=" + uuid +
				" not exist on server.");
		}
		if (null == dataset.get().getBlockDimension(r)) {
			throw new IllegalArgumentException("Dataset with UUID=" + uuid +
				" has not resolution [" + IntStream.of(r).mapToObj(i -> "" + i)
					.collect(Collectors.joining(",")) + "]");
		}
		Integer port = findRandomOpenPortOnAllLocalInterfaces();
		ProcessBuilder pb = new ProcessBuilder().inheritIO();
		List<String> commandAsList = new LinkedList<>();
		//@formatter:off
		ListAppender<String> appender = new ListAppender<>(commandAsList)
				.append("java")
				.append("-Dquarkus.http.port=" + port)
				.append("-Dquarkus.datasource.jdbc.url=jdbc:h2:mem:myDb;create=true")
				.append("-Ddatastore.path=" + applicationConfiguration.getDatastorePath())
				.append("-D" + PROPERTY_UUID + "=" + uuid)
				.append("-D" + PROPERTY_RESOLUTION + "=" + String.join(",", Arrays.stream(r).mapToObj(i -> ""+ i).collect(Collectors.toList())))
				.append("-D" + PROPERTY_VERSION + "=" + version)
				.append("-D"+ PROPERTY_MODE +"=" + mode);
		if (timeout != null) {
			appender.append("-D" + PROPERTY_DATA_STORE_TIMEOUT + "=" + timeout);
		}
		
		String classPath = System.getProperty("java.class.path");
		if (classPath.endsWith("quarkus-run.jar")) {
			appender
			.append("-jar")
			.append(classPath);
		} else {
			appender
			.append("-cp")
			.append(classPath)
			.append(APP_CLASS);
		}
		//@formatter:on
		
		pb.command(commandAsList);
		Process process = pb.start();
		processes.add(process);
		String result = String.format("http://%s:%d/", getHostName(), port);
		log.info("waiting for server starts on {}", result);
		while (true) {
			try (Socket soc = new Socket(getHostName(), port)) {
				break;
			}
			catch (IOException e) {
				try {
					Thread.sleep(WAIT_FOR_SERVER_TIMEOUT);
				}
				catch (InterruptedException exc) {
					return null;
				}
			}
		}
		return new URL(result);
	}

	@Override
	public void stopCurrentDataServer() {
		Quarkus.asyncExit();
	}

	@Override
	public boolean check(UUID uuidTyped, String version, String mode) {
		return System.getProperty(PROPERTY_UUID, "").equals(uuidTyped.toString()) &&
			System.getProperty(PROPERTY_VERSION, "").equals(version) && System
				.getProperty(PROPERTY_MODE).equals(mode);
	}

	@Override
	public UUID getUUID() {
		String uuid = System.getProperty(PROPERTY_UUID, "");
		if (uuid.isEmpty()) {
			return null;
		}
		try {
			return UUID.fromString(uuid);
		}
		catch (IllegalArgumentException exc) {
			log.warn("uuid={} passed as property is not valid", uuid);
			return null;
		}
	}


	@Override
	public int[] getResolutionLevel() {
		String[] tokens = System.getProperty(PROPERTY_RESOLUTION).split(",");
		int[] result = new int[tokens.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = Integer.parseInt(tokens[i]);
		}
		return result;
	}

	@Override
	public String getVersion() {
		return System.getProperty(PROPERTY_VERSION, "");
	}

	@Override
	public OperationMode getMode() {
		return OperationMode.valueOf(System.getProperty(PROPERTY_MODE, ""));
	}


	/**
	 * @param obj
	 */
	public void observesApplicationScopedBeforeDestroyed(
		@Observes @BeforeDestroyed(ApplicationScoped.class) Object obj)
	{
		Process proc;
		while (null != (proc = processes.poll())) {
			proc.destroyForcibly();
		}
	}

	@Override
	public long getServerTimeout() {
		if (dataserverTimeout == null) {
			dataserverTimeout = Long.parseLong(System.getProperty(
				PROPERTY_DATA_STORE_TIMEOUT,
			"-1"));
		}
		return dataserverTimeout;
	}

	private String getHostName() throws UnknownHostException {
		String hostName = System.getProperty("quarkus.http.host", "localhost");
		if (hostName.equals("0.0.0.0")) {
			hostName = InetAddress.getLocalHost().getHostAddress();
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



	@AllArgsConstructor
	private static class ListAppender<T> {

		private Collection<T> innerCollection;

		ListAppender<T> append(T item) {
			innerCollection.add(item);
			return this;
		}
	}
}
