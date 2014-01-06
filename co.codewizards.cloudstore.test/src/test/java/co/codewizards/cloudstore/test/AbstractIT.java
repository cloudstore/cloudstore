package co.codewizards.cloudstore.test;

import java.net.Socket;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.server.CloudStoreServer;

public abstract class AbstractIT {
	static {
		System.setProperty(ConfigDir.SYSTEM_PROPERTY, "target/.cloudstore");
	}

	protected static final SecureRandom random = new SecureRandom();
	private static CloudStoreServer cloudStoreServer;
	private static Thread cloudStoreServerThread;
	private static final Object cloudStoreServerMutex = new Object();
	private static final Timer cloudStoreServerStopTimer = new Timer("cloudStoreServerStopTimer", true);
	private static TimerTask cloudStoreServerStopTimerTask;

	private static int securePort;

	public static int getSecurePort() {
		return securePort;
	}

	@BeforeClass
	public static void beforeClass() {
		synchronized (cloudStoreServerMutex) {
			if (cloudStoreServerStopTimerTask != null) {
				cloudStoreServerStopTimerTask.cancel();
				cloudStoreServerStopTimerTask = null;
			}

			if (cloudStoreServer == null) {
				IOUtil.deleteDirectoryRecursively(ConfigDir.getInstance().getFile());

				cloudStoreServer = new CloudStoreServer();
				securePort = 1024 + 1 + random.nextInt(10240);
				cloudStoreServer.setSecurePort(securePort);
				cloudStoreServerThread = new Thread(cloudStoreServer);
				cloudStoreServerThread.setName("cloudStoreServerThread");
				cloudStoreServerThread.setDaemon(true);
				cloudStoreServerThread.start();
				waitForServerToOpenSecurePort();
			}
		}
	}

	private static void waitForServerToOpenSecurePort() {
		final long timeoutMillis = 60000L;
		long begin = System.currentTimeMillis();
		while (true) {
			try {
				Socket socket = new Socket("localhost", getSecurePort());
				socket.close();
				return; // success!
			} catch (Exception x) {
				try { Thread.sleep(1000); } catch (InterruptedException ie) { }
			}

			if (System.currentTimeMillis() - begin > timeoutMillis)
				throw new IllegalStateException("Server did not start within timeout (ms): " + timeoutMillis);
		}
	}

	@AfterClass
	public static void afterClass() {
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
