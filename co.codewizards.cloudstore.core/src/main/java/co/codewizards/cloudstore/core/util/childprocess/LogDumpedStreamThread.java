package co.codewizards.cloudstore.core.util.childprocess;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread used to log standard-out or standard-error from a child-process.
 * <p>
 * Besides logging, it can write all data to a {@link StringBuffer}.
 * <p>
 * An instance of this class is usally not created explicitly, but implicitly via an instance of
 * {@link DumpStreamThread}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LogDumpedStreamThread extends Thread
{
	/**
	 * UTF-8 character set name.
	 */
	private static final String CHARSET_NAME_UTF_8 = "UTF-8";

	private static final Logger logger = LoggerFactory.getLogger(LogDumpedStreamThread.class);

	/**
	 * If the first data which arrived here via {@link #write(byte[], int)} have not yet been written
	 * after this time (i.e. their age exceeds this time) in milliseconds, it is logged.
	 */
	private static final long logMaxAge = 5000L;

	/**
	 * If there was no write after this many milliseconds, the current buffer is logged.
	 */
	private static final long logAfterNoWritePeriod = 500L;

	/**
	 * If the buffer grows bigger than this size in bytes, it is logged - no matter when the last
	 * write occured.
	 */
	private static final int logWhenBufferExceedsSize = 50 * 1024; // 50 KB

	private final ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
	private volatile boolean forceInterrupt = false;
	private Long firstNonLoggedWriteTimestamp = null;
	private long lastWriteTimestamp = 0;

	private volatile StringBuffer outputStringBuffer;
	private volatile int outputStringBufferMaxLength = 1024 * 1024;

	private Logger childProcessLogger;

	public LogDumpedStreamThread(final String childProcessLoggerName) {
		this(childProcessLoggerName == null ? null : LoggerFactory.getLogger(childProcessLoggerName));
	}

	public LogDumpedStreamThread(final Logger childProcessLogger)
	{
		if (childProcessLogger == null)
			this.childProcessLogger = logger;
		else
			this.childProcessLogger = childProcessLogger;
	}

	public void write(final byte[] data, final int length)
	{
		if (data == null)
			throw new IllegalArgumentException("data == null"); //$NON-NLS-1$

		synchronized (bufferOutputStream) {
			bufferOutputStream.write(data, 0, length);
			lastWriteTimestamp = System.currentTimeMillis();
			if (firstNonLoggedWriteTimestamp == null)
				firstNonLoggedWriteTimestamp = lastWriteTimestamp;
		}
	}

	public void setOutputStringBuffer(final StringBuffer outputStringBuffer) {
		this.outputStringBuffer = outputStringBuffer;
	}
	public StringBuffer getOutputStringBuffer() {
		return outputStringBuffer;
	}
	public void setOutputStringBufferMaxLength(final int outputStringBufferMaxLength) {
		this.outputStringBufferMaxLength = outputStringBufferMaxLength;
	}
	public int getOutputStringBufferMaxLength() {
		return outputStringBufferMaxLength;
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

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				synchronized (bufferOutputStream) {
					try {
						bufferOutputStream.wait(500L);
					} catch (final InterruptedException x) {
//						doNothing(); TODO reference to the Util.doNothing class after Util moved into this util project!
					}

					processBuffer(false);
				}
			} catch (final Throwable e) {
				logger.error("run: " + e, e); //$NON-NLS-1$
			}
		}
		processBuffer(true);
	}

	public void flushBuffer()
	{
		processBuffer(true);
	}

	protected void processBuffer(final boolean force)
	{
		synchronized (bufferOutputStream) {
			if (bufferOutputStream.size() > 0) {
				final long firstNonLoggedWriteAge = firstNonLoggedWriteTimestamp == null ? 0 : System.currentTimeMillis() - firstNonLoggedWriteTimestamp;
				final long noWritePeriod = System.currentTimeMillis() - lastWriteTimestamp;
				if (force || firstNonLoggedWriteAge > logMaxAge || noWritePeriod > logAfterNoWritePeriod || bufferOutputStream.size() > logWhenBufferExceedsSize) {
					String currentBufferString;
					try {
						currentBufferString = bufferOutputStream.toString(CHARSET_NAME_UTF_8);
					} catch (final UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}

					final StringBuffer outputStringBuffer = getOutputStringBuffer();
					if (outputStringBuffer != null) {
						final int newOutputStringBufferLength = outputStringBuffer.length() + currentBufferString.length();
						if (newOutputStringBufferLength > outputStringBufferMaxLength) {
							int lastCharPositionToDelete = newOutputStringBufferLength - outputStringBufferMaxLength;
							// search for first line-break
							while (outputStringBuffer.length() > lastCharPositionToDelete && outputStringBuffer.charAt(lastCharPositionToDelete) != '\n')
								++lastCharPositionToDelete;

							lastCharPositionToDelete = Math.min(lastCharPositionToDelete, outputStringBuffer.length() - 1);
							outputStringBuffer.delete(0, lastCharPositionToDelete + 1);
						}

						outputStringBuffer.append(currentBufferString);
					}

					childProcessLogger.info(
							'\n' + prefixEveryLine(currentBufferString)
							);

					bufferOutputStream.reset();
				}
			}
		}
	}

	private String prefixEveryLine(final String s)
	{
		try {
			final StringBuilder result = new StringBuilder();
			final String prefix = "  >>> "; //$NON-NLS-1$
			final BufferedReader r = new BufferedReader(new StringReader(s));
			String line;
			while (null != (line = r.readLine()))
				result.append(prefix).append(line).append('\n');

			return result.toString();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}
}
