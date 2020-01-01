package co.codewizards.cloudstore.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.server.CloudStoreServer;

public class CloudStoreServerTestSupport {

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreServerTestSupport.class);

	public final Uid instanceId = new Uid(); 
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
			logger.debug("[{}].beforeClass: entered. testInstanceCounter={}", instanceId, testInstanceCounter);

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
//		final ServerSocket serverSocket = new ServerSocket(0);
//		final int port = serverSocket.getLocalPort();
//		serverSocket.close();
//		return port;
		
		for (int tryCount = 0; tryCount < 100; ++tryCount) {
			int port = 1024 + 1 + random.nextInt(3 * 10240);
			logger.debug("[{}].getRandomAvailableServerPort: port={}: Trying to connect...", instanceId, port);
			try {
				final Socket socket = new Socket("localhost", port);
				socket.close();
				logger.warn("[{}].getRandomAvailableServerPort: port={}: Connected => Port already in use!", instanceId, port);
			} catch (final Exception x) {
				// fine -- nothing is listening on this port, yet. Likely available for us.
				logger.debug("[{}].getRandomAvailableServerPort: port={}: Connection attempt failed => Port available.", instanceId, port);
				return port;
			}
		}
		throw new IllegalStateException("Could not find any available server-port!");
	}

	protected CloudStoreServer createCloudStoreServer() {
		return new CloudStoreServer();
	}

	private void waitForServerToOpenSecurePort() {
		logger.debug("[{}].waitForServerToOpenSecurePort: securePort={}: entered.", instanceId, getSecurePort());
		final long timeoutMillis = 3 * 60_000L;
		final long begin = System.currentTimeMillis();
		while (true) {
			try {
				final Socket socket = new Socket("localhost", getSecurePort());
				socket.close();
				logger.info("[{}].waitForServerToOpenSecurePort: securePort={}: successfully connected.", instanceId, getSecurePort());
				return; // success!
			} catch (final Exception x) {
				logger.debug("waitForServerToOpenSecurePort: securePort={}: failed => sleeping + retrying...", getSecurePort());
				try { Thread.sleep(1000); } catch (final InterruptedException ie) { }
			}

			if (System.currentTimeMillis() - begin > timeoutMillis) {
				String message = String.format("Server on securePort=%s did not start within timeout=%s ms!", getSecurePort(), timeoutMillis);
				logger.error("[{}].waitForServerToOpenSecurePort: {}", instanceId, message);
				throw new IllegalStateException(message);
			}
		}
	}

	/**
	 * @return <code>true</code>, if this is the last invocation. <code>false</code> before.
	 */
	public boolean afterClass() throws Exception {
		synchronized (cloudStoreServerMutex) {
			logger.debug("[{}].afterClass: entered. testInstanceCounter={}", instanceId, testInstanceCounter);
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
