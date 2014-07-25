package co.codewizards.cloudstore.test;

import java.net.Socket;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.server.CloudStoreServer;

public class CloudStoreServerTestSupport {

	private static final SecureRandom random = new SecureRandom();
	private static final AtomicInteger cloudStoreServerStopTimerIndex = new AtomicInteger();
	private CloudStoreServer cloudStoreServer;
	private Thread cloudStoreServerThread;
	private final Object cloudStoreServerMutex = new Object();
	private final Timer cloudStoreServerStopTimer = new Timer("cloudStoreServerStopTimer-" + cloudStoreServerStopTimerIndex.incrementAndGet(), true);
	private TimerTask cloudStoreServerStopTimerTask;

	private int securePort;

	public int getSecurePort() {
		return securePort;
	}

	public String getSecureUrl() {
		return "https://localhost:" + getSecurePort();
	}

	public void beforeClass() {
		synchronized (cloudStoreServerMutex) {
			if (cloudStoreServerStopTimerTask != null) {
				cloudStoreServerStopTimerTask.cancel();
				cloudStoreServerStopTimerTask = null;
			}

			if (cloudStoreServer == null) {
				IOUtil.deleteDirectoryRecursively(ConfigDir.getInstance().getFile());

				securePort = 1024 + 1 + random.nextInt(10240);
				cloudStoreServer = createCloudStoreServer();
				cloudStoreServer.setSecurePort(securePort);
				cloudStoreServerThread = new Thread(cloudStoreServer);
				cloudStoreServerThread.setName("cloudStoreServerThread");
				cloudStoreServerThread.setDaemon(true);
				cloudStoreServerThread.start();
				waitForServerToOpenSecurePort();
			}
		}
	}

	protected CloudStoreServer createCloudStoreServer() {
		return new CloudStoreServer();
	}

	private void waitForServerToOpenSecurePort() {
		final long timeoutMillis = 60000L;
		final long begin = System.currentTimeMillis();
		while (true) {
			try {
				final Socket socket = new Socket("localhost", getSecurePort());
				socket.close();
				return; // success!
			} catch (final Exception x) {
				try { Thread.sleep(1000); } catch (final InterruptedException ie) { }
			}

			if (System.currentTimeMillis() - begin > timeoutMillis)
				throw new IllegalStateException("Server did not start within timeout (ms): " + timeoutMillis);
		}
	}

	public void afterClass() {
		synchronized (cloudStoreServerMutex) {
			if (cloudStoreServerStopTimerTask == null) {
				cloudStoreServerStopTimerTask = new TimerTask() {
					@Override
					public void run() {
						synchronized (cloudStoreServerMutex) {
							if (cloudStoreServer != null) {
								cloudStoreServer.stop();
								cloudStoreServer = null;
								cloudStoreServerStopTimerTask = null;
							}
						}
					}
				};
				cloudStoreServerStopTimer.schedule(cloudStoreServerStopTimerTask, 60000L);
			}
		}
	}

}
