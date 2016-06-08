package org.toilelibre.libe.bot;

import java.util.Map;

import it.sauronsoftware.cron4j.Scheduler;

class CronStuff {

	static void startScheduler() {
		final Scheduler scheduler = new Scheduler();
		for (final Map.Entry<String, Runnable> taskEntry : Params.TASKS.entrySet()) {
			scheduler.schedule(taskEntry.getKey(), taskEntry.getValue());
		}
		scheduler.start();
	}

}
