package co.codewizards.cloudstore.test;

import java.io.IOException;
import java.net.ServerSocket;
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

	/**
	 * When running tests in parallel, the beforeClass() and afterClass() seem to be invoked multiple times.
	 */
	private int testInstanceCounter;

	private int securePort;

	public int getSecurePort() {
		return securePort;
	}

	public String getSecureUrl() {
		return "https://localhost:" + getSecurePort();
	}

	/**
	 * @return <code>true</code>, if this is the first invocation. <code>false</code> afterwards.
	 */
	public boolean beforeClass() throws Exception {
		synchronized (cloudStoreServerMutex) {
			final boolean first = testInstanceCounter++ == 0;

			if (cloudStoreServerStopTimerTask != null) {
				cloudStoreServerStopTimerTask.cancel();
				cloudStoreServerStopTimerTask = null;
			}

			if (cloudStoreServer == null) {
				IOUtil.deleteDirectoryRecursively(ConfigDir.getInstance().getFile());

//				securePort = 1024 + 1 + random.nextInt(10240);
				securePort = getRandomAvailableServerPort();
				cloudStoreServer = createCloudStoreServer();
				cloudStoreServer.setSecurePort(securePort);
				cloudStoreServerThread = new Thread(cloudStoreServer);
				cloudStoreServerThread.setName("cloudStoreServerThread");
				cloudStoreServerThread.setDaemon(true);
				cloudStoreServerThread.start();
				waitForServerToOpenSecurePort();
			}

			return first;
		}
	}

	private int getRandomAvailableServerPort() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(0);
		final int port = serverSocket.getLocalPort();
		serverSocket.close();
		return port;
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

	/**
	 * @return <code>true</code>, if this is the last invocation. <code>false</code> before.
	 */
	public boolean afterClass() throws Exception {
		synchronized (cloudStoreServerMutex) {
			if (--testInstanceCounter > 0)
				return false;

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

			return true;
		}
	}

}
