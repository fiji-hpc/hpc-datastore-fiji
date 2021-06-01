/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.timout_shutdown;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import cz.it4i.fiji.datastore.management.DataServerManager;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class TimeoutTimer {

	@Inject
	DataServerManager dataServerManager;

	private Timer timer;

	private TimerTask task;

	private Instant nextTimeout;

	@PostConstruct
	public void init() {
		timer = new Timer();
		log.info("Init timeout timer.");
		scheduleTimer();
	}

	// TODO - make reschedule asynchronous or use set next timeout
	public void scheduleTimer() {
		long timeout = dataServerManager.getServerTimeout();
		if (timeout <= 0) {
			return;
		}
		nextTimeout = Instant.now().plus(timeout, ChronoUnit.MILLIS);
		createTimer();
	}

	private synchronized void createTimer() {
		if (task == null) {
			task = new MyTimerTask(this::timeout);
			timer.schedule(task, Date.from(nextTimeout));

		}
	}

	synchronized private void timeout() {
		Instant currentTime = Instant.now();
		task = null;
		if (currentTime.isBefore(nextTimeout)) {
			createTimer();
		}
		else {
			dataServerManager.stopCurrentDataServer();
		}
	}

	@AllArgsConstructor
	private static class MyTimerTask extends TimerTask {

		private final Runnable runnable;

		@Override
		public void run() {
			runnable.run();
		}

	}
}
