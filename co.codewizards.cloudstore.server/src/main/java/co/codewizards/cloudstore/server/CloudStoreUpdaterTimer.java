package co.codewizards.cloudstore.server;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.updater.CloudStoreUpdaterCore;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;

public class CloudStoreUpdaterTimer {

	/**
	 * Default value for {@link #CONFIG_KEY_TIMER_PERIOD} (15 minutes in milliseconds).
	 */
	public static final long DEFAULT_TIMER_PERIOD = 15 * 60 * 1000;

	/**
	 * Configuration property key controlling how many milliseconds the timer waits between
	 * invocations of {@link CloudStoreUpdaterCore#createUpdaterDirIfUpdateNeeded()}.
	 * <p>
	 * Important: Setting this to 5 minutes does not necessarily mean that an update will be done
	 * 5 minutes after the new version was released. There's a local cache and a new release is only detected
	 * after this cache expired -- see {@link CloudStoreUpdaterCore#CONFIG_KEY_REMOTE_VERSION_CACHE_VALIDITY_PERIOD}.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 *
	 * @see #DEFAULT_TIMER_PERIOD
	 */
	public static final String CONFIG_KEY_TIMER_PERIOD = "updater.timer.period";

	/**
	 * The timer-period for the very first check, done after {@link #start()} (30 seconds in milliseconds).
	 * <p>
	 * Important: This is also the time it takes until the "updater/"-directory from a previous
	 * update is deleted.
	 */
	private static final long ON_START_TIMER_PERIOD = 30 * 1000;

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreUpdaterTimer.class);

	private Timer timer;
	private TimerTask timerTask;

	public CloudStoreUpdaterTimer() {
	}

	public synchronized void start() {
		if (timer == null)
			schedule(true);
	}

	public synchronized void stop() {
		cancelTimerTask();
		cancelTimer();
	}

	protected synchronized void schedule(final boolean onStart) {
		cancelTimerTask();

		final long timerPeriod = getTimerPeriod();
		if (timerPeriod <= 0) {
			logger.info("schedule: timerPeriod={}. Disabling this timer!", timerPeriod);
			cancelTimer();
			return;
		}

		if (timer == null)
			timer = new Timer("CloudStoreUpdaterTimer");

		if (timerTask != null) // due to cancelTimerTask() above, this should never happen!
			throw new IllegalStateException("timerTask != null");

		timerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					CloudStoreUpdaterTimer.this.run();
					schedule(false); // reschedule, because we always schedule without period (one time execution).
				} catch (Exception x) {
					logger.error("timerTask.run: " + x, x);
				}
			}
		};

		final Date nextRun;
		if (onStart) {
			nextRun = new Date(System.currentTimeMillis() + ON_START_TIMER_PERIOD);
			logger.info("schedule: onStart=true nextRun={}", nextRun);
		}
		else {
			nextRun = new Date(System.currentTimeMillis() + timerPeriod);
			logger.info("schedule: timerPeriod={} nextRun={}", timerPeriod, nextRun);
		}
		timer.schedule(timerTask, nextRun);
	}

	protected synchronized void cancelTimerTask() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	protected synchronized void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	protected long getTimerPeriod() {
		final Config config = ConfigImpl.getInstance();
		return config.getPropertyAsLong(CONFIG_KEY_TIMER_PERIOD, DEFAULT_TIMER_PERIOD);
	}

	protected void run() {
		final boolean updateNeeded = new CloudStoreUpdaterCore().createUpdaterDirIfUpdateNeeded();

		if (updateNeeded)
			System.exit(0);
	}
}
