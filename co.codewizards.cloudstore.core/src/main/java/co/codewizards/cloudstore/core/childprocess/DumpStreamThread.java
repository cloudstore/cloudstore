package co.codewizards.cloudstore.core.childprocess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class DumpStreamThread extends Thread
{
	private final Logger logger = LoggerFactory.getLogger(DumpStreamThread.class);
	private InputStream inputStream;
	private OutputStream outputStream;
	private volatile boolean ignoreErrors = false;
	private volatile boolean forceInterrupt = false;
	private LogDumpedStreamThread logDumpedStreamThread;

	public void setIgnoreErrors(boolean ignoreErrors) {
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

	public DumpStreamThread(InputStream inputStream, OutputStream outputStream, String childProcessLoggerName) throws IOException
	{
		if (inputStream == null)
			throw new IllegalArgumentException("inputStream == null"); //$NON-NLS-1$
		if (outputStream == null)
			throw new IllegalArgumentException("outputStream == null"); //$NON-NLS-1$

		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.logDumpedStreamThread = new LogDumpedStreamThread(childProcessLoggerName);
	}

	@Override
	public synchronized void start() {
		logDumpedStreamThread.start();
		super.start();
	}

	@Override
	public void run() {
		try {
			final byte[] buffer = new byte[10240];
			while (!isInterrupted()) {
				try {
					int bytesRead = inputStream.read(buffer);
					if (bytesRead > 0) {
						outputStream.write(buffer, 0, bytesRead);
						logDumpedStreamThread.write(buffer, bytesRead);
					}
				} catch (Throwable e) {
					if (!ignoreErrors)
						logger.error("run: " + e, e); //$NON-NLS-1$
					else
						logger.info("run: " + e); //$NON-NLS-1$

					return;
				}
			}
		} finally {
			logDumpedStreamThread.interrupt();
			try {
				outputStream.close();
			} catch (IOException e) {
				logger.warn("run: outputStream.close() failed: " + e, e); //$NON-NLS-1$
			}
		}
	}

	public void setOutputStringBuffer(StringBuffer outputStringBuffer) {
		logDumpedStreamThread.setOutputStringBuffer(outputStringBuffer);
	}
	public StringBuffer getOutputStringBuffer() {
		return logDumpedStreamThread.getOutputStringBuffer();
	}
	public void setOutputStringBufferMaxLength(int outputStringBufferMaxLength) {
		logDumpedStreamThread.setOutputStringBufferMaxLength(outputStringBufferMaxLength);
	}
	public int getOutputStringBufferMaxLength() {
		return logDumpedStreamThread.getOutputStringBufferMaxLength();
	}
	public void flushBuffer() {
		logDumpedStreamThread.flushBuffer();
	}
}