package co.codewizards.cloudstore.core.util.childprocess;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread continuously reading from an {@link InputStream} and writing everything to an {@link OutputStream}.
 * <p>
 * While this functionality is useful for multiple purposes, we use {@code DumpStreamThread} primarily for
 * child processes. When starting another process (e.g. via the
 * {@link java.lang.ProcessBuilder ProcessBuilder}), it is necessary to read the process' output. Without
 * doing this, the child process might block forever - especially on Windows (having a very limited
 * standard-output-buffer), this is a well-known problem.
 * <p>
 * Since the main thread (invoking the child process) usually blocks and waits for the child process to
 * return (i.e. exit), dumping its standard-out and standard-error to a buffer or a log file is done on a
 * separate thread.
 * <p>
 * Please note that {@code DumpStreamThread} can automatically write everything to the log. This is done via
 * an instance of {@link LogDumpedStreamThread}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class DumpStreamThread extends Thread
{
	private final Logger logger = LoggerFactory.getLogger(DumpStreamThread.class);
	private final InputStream inputStream;
	private final OutputStream outputStream;
	private volatile boolean ignoreErrors = false;
	private volatile boolean forceInterrupt = false;
	private final LogDumpedStreamThread logDumpedStreamThread;

	public void setIgnoreErrors(final boolean ignoreErrors) {
		this.ignoreErrors = ignoreErrors;
	}

	@Override
	public void interrupt() {
		forceInterrupt = true;
		super.interrupt();
	}

	@Override
	public boolean isInterrupted() {
		return forceInterrupt || super.isInterrupted();
	}

	/**
	 * Creates an instance of {@code DumpStreamThread}.
	 * @param inputStream the stream to read from. Must not be <code>null</code>.
	 * @param outputStream the stream to write to. Must not be <code>null</code>.
	 * @param childProcessLoggerName the name of the logger. May be <code>null</code> if logging is not
	 * desired. In case of <code>null</code>, logging is completely disabled.
	 */
	public DumpStreamThread(final InputStream inputStream, final OutputStream outputStream, final String childProcessLoggerName) {
		this(inputStream, outputStream, null, childProcessLoggerName);
	}

	/**
	 * Creates an instance of {@code DumpStreamThread}.
	 * @param inputStream the stream to read from. Must not be <code>null</code>.
	 * @param outputStream the stream to write to. Must not be <code>null</code>.
	 * @param childProcessLogger the logger. May be <code>null</code> if logging is not desired.
	 */
	public DumpStreamThread(final InputStream inputStream, final OutputStream outputStream, final Logger childProcessLogger) {
		this(inputStream, outputStream, childProcessLogger, null);
	}

	private DumpStreamThread(final InputStream inputStream, final OutputStream outputStream,
			final Logger childProcessLogger, final String childProcessLoggerName)
	{
		assertNotNull("inputStream", inputStream);
		assertNotNull("outputStream", outputStream);

		this.inputStream = inputStream;
		this.outputStream = outputStream;
		if (childProcessLogger != null)
			this.logDumpedStreamThread = new LogDumpedStreamThread(childProcessLogger);
		else
			this.logDumpedStreamThread = childProcessLoggerName == null ? null : new LogDumpedStreamThread(childProcessLoggerName);
	}

	@Override
	public synchronized void start() {
		if (logDumpedStreamThread != null)
			logDumpedStreamThread.start();

		super.start();
	}

	@Override
	public void run() {
		try {
			final byte[] buffer = new byte[10240];
			while (!isInterrupted()) {
				try {
					final int bytesRead = inputStream.read(buffer);
					if (bytesRead > 0) {
						outputStream.write(buffer, 0, bytesRead);

						if (logDumpedStreamThread != null)
							logDumpedStreamThread.write(buffer, bytesRead);
					}
				} catch (final Throwable e) {
					if (!ignoreErrors)
						logger.error("run: " + e, e); //$NON-NLS-1$
					else
						logger.info("run: " + e); //$NON-NLS-1$

					return;
				}
			}
		} finally {
			if (logDumpedStreamThread != null)
				logDumpedStreamThread.interrupt();

			try {
				outputStream.close();
			} catch (final IOException e) {
				logger.warn("run: outputStream.close() failed: " + e, e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Sets a {@link StringBuffer} for capturing all output.
	 * <p>
	 * Please note, that only data read from the stream after this was set is captured. You normally want to
	 * set this {@code StringBuffer}.
	 * <p>
	 * This feature is only available, if logging is enabled.
	 * @param outputStringBuffer the {@link StringBuffer} used for capturing. May be <code>null</code>.
	 */
	public void setOutputStringBuffer(final StringBuffer outputStringBuffer) {
		if (logDumpedStreamThread == null)
			throw new IllegalStateException("Not supported, if logging is disabled!");

		logDumpedStreamThread.setOutputStringBuffer(outputStringBuffer);
	}
	public StringBuffer getOutputStringBuffer() {
		if (logDumpedStreamThread == null)
			throw new IllegalStateException("Not supported, if logging is disabled!");

		return logDumpedStreamThread.getOutputStringBuffer();
	}
	public void setOutputStringBufferMaxLength(final int outputStringBufferMaxLength) {
		if (logDumpedStreamThread == null)
			throw new IllegalStateException("Not supported, if logging is disabled!");

		logDumpedStreamThread.setOutputStringBufferMaxLength(outputStringBufferMaxLength);
	}
	public int getOutputStringBufferMaxLength() {
		if (logDumpedStreamThread == null)
			throw new IllegalStateException("Not supported, if logging is disabled!");

		return logDumpedStreamThread.getOutputStringBufferMaxLength();
	}

	public void flushBuffer() {
		if (logDumpedStreamThread != null)
			logDumpedStreamThread.flushBuffer();
	}
}