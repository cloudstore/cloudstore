package co.codewizards.cloudstore.shared.progress;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Pattern;

import org.slf4j.Logger;

/**
 * A progress monitor implementation which logs to an SLF4J {@link Logger}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class LoggerProgressMonitor implements ProgressMonitor
{
	private Logger logger;

	/**
	 * Create a monitor logging to the specified logger.
	 *
	 * @param logger the logger to write to. Must not be <code>null</code>.
	 */
	public LoggerProgressMonitor(Logger logger) {
		if (logger == null)
			throw new IllegalArgumentException("logger == null");

		this.logger = logger;
	}

	/**
	 * The variable containing the task-name (i.e. what is done). This is the name which is passed to
	 * the {@link ProgressMonitor#beginTask(String, int)} method.
	 * @see #setMessage(String)
	 */
	public static final String MESSAGE_VARIABLE_NAME = "${name}";

	/**
	 * The variable containing the current percentage.
	 * @see #setMessage(String)
	 */
	public static final String MESSAGE_VARIABLE_PERCENTAGE = "${percentage}";

	private String message = MESSAGE_VARIABLE_NAME + ": " + MESSAGE_VARIABLE_PERCENTAGE;

	/**
	 * Get the log-message. For details see {@link #setMessage(String)}.
	 * @return the message to be logged.
	 * @see #setMessage(String)
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * <p>Set the message which will be written to the logger.</p>
	 * <p>
	 * Before writing to the logger, the variables contained in this message
	 * are replaced by their actual values. The following variables can be used:
	 * </p>
	 * <ul>
	 * <li>{@link #MESSAGE_VARIABLE_NAME}</li>
	 * <li>{@link #MESSAGE_VARIABLE_PERCENTAGE}</li>
	 * </ul>
	 *
	 * @param message the message to be logged.
	 * @see #getMessage()
	 */
	public void setMessage(String message) {
		if (message == null)
			throw new IllegalArgumentException("message must not be null!");

		this.message = message;
	}

	/**
	 * Get the logger to which this monitor is writing. It is the one that has been passed
	 * to {@link #LoggerProgressMonitor(Logger)} before.
	 * @return the logger.
	 */
	public Logger getLogger() {
		return logger;
	}

	private String name;

	private double internalTotalWork = Double.NaN;

	private double internalWorked = 0;

	private float percentageWorked = 0;
	private float lastLogMessage_percentageWorked = Float.MIN_VALUE;

	private long lastLogMessage_timestamp = 0;

	private long logMinPeriodMSec = 5000;

	/**
	 * Get the minimum log period in milliseconds. For details see {@link #setLogMinPeriodMSec(long)}.
	 * @return the minimum log period.
	 * @see #setLogMinPeriodMSec(long)
	 */
	public long getLogMinPeriodMSec() {
		return logMinPeriodMSec;
	}
	/**
	 * <p>
	 * Set the minimum period between log messages in milliseconds.
	 * </p>
	 * <p>
	 * In order to prevent log flooding as well as to improve performance,
	 * calling {@link #worked(int)} or {@link #internalWorked(double)} will only cause
	 * a log message to be printed, if at least one of the following criteria are met:
	 * </p>
	 * <ul>
	 * <li>
	 * The last log message happened longer ago than the time (in milliseconds) configured
	 * by the property <code>logMinPeriodMSec</code> (i.e. by this method).
	 * </li>
	 * <li>
	 * The percentage  difference to the last log message is larger than
	 * {@link #getLogMinPercentageDifference() the min-percentage-difference}.
	 * </li>
	 * <li>
	 * 100% are reached.
	 * </li>
	 * </ul>
	 * @param logMinPeriodMSec the minimum period between log messages.
	 * @see #getLogMinPeriodMSec()
	 * @see #setLogMinPercentageDifference(float)
	 */
	public void setLogMinPeriodMSec(long logMinPeriodMSec) {
		this.logMinPeriodMSec = logMinPeriodMSec;
	}

	private float logMinPercentageDifference = 5f;

	/**
	 * Get the minimum percentage difference to trigger a new log message. For details see
	 * {@link #setLogMinPercentageDifference(float)}.
	 * @return the minimum percentage difference.
	 * @see #setLogMinPercentageDifference(float)
	 */
	public float getLogMinPercentageDifference() {
		return logMinPercentageDifference;
	}
	/**
	 * <p>
	 * Set the minimum period between log messages in milliseconds.
	 * </p>
	 * <p>
	 * In order to prevent log flooding as well as to improve performance,
	 * calling {@link #worked(int)} or {@link #internalWorked(double)} will only cause
	 * a log message to be printed, if at least one of the following criteria are met:
	 * </p>
	 * <ul>
	 * <li>
	 * The last log message happened longer ago than the time (in milliseconds) configured
	 * by the property {@link #setLogMinPeriodMSec(long) logMinPeriodMSec}.
	 * </li>
	 * <li>
	 * The percentage difference to the last log message is larger than
	 * the percentage configured by the property
	 * <code>logMinPercentageDifference</code> (i.e. by this method).
	 * </li>
	 * <li>
	 * 100% are reached.
	 * </li>
	 * </ul>
	 * @param logMinPercentageDifference the minimum percentage difference between log messages.
	 * @see #getLogMinPercentageDifference()
	 * @see #setLogMinPeriodMSec(long)
	 */
	public void setLogMinPercentageDifference(float logMinPercentageDifference) {
		this.logMinPercentageDifference = logMinPercentageDifference;
	}

	private int nestedBeginTasks = 0;

	@Override
	public void beginTask(String name, int totalWork) {
		// Ignore nested begin task calls.
		if (++nestedBeginTasks > 1)
			return;

		if (name == null)
			name = "anonymous";

		if (totalWork < 0)
			totalWork = 0;

		this.name = name;
		this.internalTotalWork = totalWork;
	}

	@Override
	public void done() {
		// Ignore if more done calls than beginTask calls or if we are still
		// in some nested beginTasks
		if (nestedBeginTasks == 0 || --nestedBeginTasks > 0)
			return;

		double stillToWork = internalTotalWork - internalWorked;
		if (stillToWork > 0)
			internalWorked(stillToWork); // To do whatever still needs to be done
	}

	@Override
	public void internalWorked(double worked) {
		if (worked < 0 || worked == Double.NaN)
			return;

		if (this.internalWorked == this.internalTotalWork)
			return;

		this.internalWorked += worked;
		if (this.internalWorked > this.internalTotalWork)
			this.internalWorked = this.internalTotalWork;

		this.percentageWorked = (float) (100d * this.internalWorked / this.internalTotalWork);
		boolean doLog = false;

		// log at 100%
		if (!doLog && (this.internalWorked == this.internalTotalWork))
			doLog = true;

		// log when the percentage difference is larger than our minimum
		if (!doLog && (this.percentageWorked - lastLogMessage_percentageWorked >= logMinPercentageDifference))
			doLog = true;

		// log when the last log happened very long ago (longer than our minimum period).
		long now = System.currentTimeMillis();
		if (!doLog && (now - lastLogMessage_timestamp >= logMinPeriodMSec))
			doLog = true;

		if (doLog) {
			lastLogMessage_percentageWorked = this.percentageWorked;
			lastLogMessage_timestamp = now;

			String percentageString = PERCENTAGE_FORMAT.format(this.percentageWorked) + '%';
			String msg = message.replaceAll(Pattern.quote(MESSAGE_VARIABLE_NAME), name);
			msg = msg.replaceAll(Pattern.quote(MESSAGE_VARIABLE_PERCENTAGE), percentageString);
			switch (logLevel) {
				case trace:
					logger.trace(msg);
					break;
				case debug:
					logger.debug(msg);
					break;
				case info:
					logger.info(msg);
					break;
				case warn:
					logger.warn(msg);
					break;
				case error:
					logger.error(msg);
					break;
				default:
					throw new IllegalStateException("Unknown logLevel: " + logLevel);
			}
		}
	}

	/**
	 * The level to use for logging.
	 *
	 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
	 *
	 * @see LoggerProgressMonitor#setLogLevel(LogLevel)
	 */
	public enum LogLevel {
		trace,
		debug,
		info,
		warn,
		error
	}

	private LogLevel logLevel = LogLevel.info;

	/**
	 * Get the log-level that is used when writing to the logger.
	 * @return the log-level to be used.
	 */
	public LogLevel getLogLevel() {
		return logLevel;
	}
	/**
	 * Set the log-level to use when writing to the logger.
	 * @param logLevel the {@link LogLevel} to be used.
	 */
	public void setLogLevel(LogLevel logLevel) {
		if (logLevel == null)
			throw new IllegalArgumentException("logLevel must not be null!");

		this.logLevel = logLevel;
	}

	private static final NumberFormat PERCENTAGE_FORMAT = new DecimalFormat("0.00");

	private volatile boolean canceled = false; // better use volatile, because the canceled flag might be accessed from different threads.

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	@Override
	public void setTaskName(String name) {
		this.name = name; // TODO not sure, if this is a correct implementation
	}

	@Override
	public void subTask(String name) {
		this.name = name; // TODO not sure, if this is a correct implementation
	}

	@Override
	public void worked(int work) {
		internalWorked(work);
	}

}
