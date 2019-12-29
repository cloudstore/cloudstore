package co.codewizards.cloudstore.ls.server;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.DebugUtil.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.BouncyCastleRegistrationUtil;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.ls.core.LocalServerPropertiesManager;
import co.codewizards.cloudstore.ls.core.LsConfig;
import co.codewizards.cloudstore.ls.rest.server.LocalServerRest;
import co.codewizards.cloudstore.ls.rest.server.auth.AuthManager;

public class LocalServer {
	public static final String CONFIG_KEY_PORT = "localServer.port";
	private static final int RANDOM_PORT = 0;
	private static final int DEFAULT_PORT = RANDOM_PORT;

	private static final Logger logger = LoggerFactory.getLogger(LocalServer.class);

	private Server server;
	private int port = -1;

	private File localServerRunningFile;
	private LockFile localServerRunningLockFile;
	private boolean localServerStopFileEnabled;
	private File localServerStopFile;
	private final Timer localServerStopFileTimer = new Timer("localServerStopFileTimer", true);
	private TimerTask localServerStopFileTimerTask;

	private static final Map<String, LocalServer> localServerRunningFile2LocalServer_running = new HashMap<>();

	public LocalServer() {
		BouncyCastleRegistrationUtil.registerBouncyCastleIfNeeded();
	}

	public File getLocalServerRunningFile() {
		if (localServerRunningFile == null) {
			try {
				localServerRunningFile = createFile(ConfigDir.getInstance().getFile(), "localServerRunning.lock").getCanonicalFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return localServerRunningFile;
	}

	public File getLocalServerStopFile() {
		if (localServerStopFile == null)
			localServerStopFile = createFile(ConfigDir.getInstance().getFile(), "localServerRunning.deleteToStop");

		return localServerStopFile;
	}

	public boolean isLocalServerStopFileEnabled() {
		return localServerStopFileEnabled;
	}
	public void setLocalServerStopFileEnabled(boolean localServerStopFileEnabled) {
		this.localServerStopFileEnabled = localServerStopFileEnabled;
	}

	/**
	 * Starts this instance of {@code LocalServer}, if no other instance is running on this computer.
	 * @return <code>true</code>, if neither this nor any other {@code LocalServer} is running on this computer, yet, and this
	 * instance could thus be started. <code>false</code>, if this instance or another instance was already started before.
	 * @throws RuntimeException in case starting the server fails for an unexpected reason.
	 */
	public boolean start() {
		logMemoryStats(logger);
		if (! LsConfig.isLocalServerEnabled())
			return false;

		LockFile _localServerRunningLockFile = null;
		try {
			final Server s;
			synchronized (localServerRunningFile2LocalServer_running) {
				final File localServerRunningFile = getLocalServerRunningFile();
				final String localServerRunningFilePath = localServerRunningFile.getPath();

				try {
					_localServerRunningLockFile = LockFileFactory.getInstance().acquire(localServerRunningFile, 5000);
				} catch (TimeoutException x) {
					return false;
				}

				if (localServerRunningFile2LocalServer_running.containsKey(localServerRunningFilePath))
					return false;

				// We now hold both the computer-wide LockFile and the JVM-wide synchronization, hence it's safe to write all the fields.
				localServerRunningLockFile = _localServerRunningLockFile;
				server = s = createServer();
				createLocalServerStopFileTimerTask();

				localServerRunningFile2LocalServer_running.put(localServerRunningFilePath, this);

				// Then we hook the lifecycle-listener in order to transfer the locking responsibility out of this method.
				s.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
					@Override
					public void lifeCycleFailure(LifeCycle event, Throwable cause) {
						onStopOrFailure();
					}
					@Override
					public void lifeCycleStopped(LifeCycle event) {
						onStopOrFailure();
					}
				});

				// The listener is hooked and thus this method's finally block is not responsible for unlocking, anymore!
				// onStopOrFailure() now has the duty of unlocking instead!
				_localServerRunningLockFile = null;
			}

			// Start outside of synchronized block to make sure, any listeners don't get stuck in a deadlock.
			s.start();

			writeLocalServerProperties();

//			waitForServerToGetReady(); // seems not to be necessary => start() seems to block until the REST app is ready => commented out.

			return true;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		} finally {
			if (_localServerRunningLockFile != null)
				_localServerRunningLockFile.release();
		}
	}

	private void createLocalServerStopFileTimerTask() {
		if (! isLocalServerStopFileEnabled())
			return;

		final File localServerStopFile = getLocalServerStopFile();
		try {
			localServerStopFile.createNewFile();

			if (! localServerStopFile.exists())
				throw new IOException("File not created!");

		} catch (Exception e) {
			throw new RuntimeException("Failed to create file: " + localServerStopFile);
		}

		synchronized (localServerStopFileTimer) {
			cancelLocalServerStopFileTimerTask();

			localServerStopFileTimerTask = new TimerTask() {
				@Override
				public void run() {
					if (localServerStopFile.exists()) {
						logger.debug("localServerStopFileTimerTask.run: file '{}' exists => nothing to do.", localServerStopFile);
						return;
					}
					logger.info("localServerStopFileTimerTask.run: file '{}' does not exist => stopping server!", localServerStopFile);

					stop();
					System.exit(0);
				}
			};

			final long period = 5000L;
			localServerStopFileTimer.schedule(localServerStopFileTimerTask, period, period);
		}
	}

	private void cancelLocalServerStopFileTimerTask() {
		synchronized (localServerStopFileTimer) {
			if (localServerStopFileTimerTask != null) {
				localServerStopFileTimerTask.cancel();
				localServerStopFileTimerTask = null;
			}
		}
	}

//	private void waitForServerToGetReady() throws TimeoutException { // seems not to be necessary => start() seems to block until the REST app is ready => commented out.
//		final int localPort = getLocalPort();
//		if (localPort < 0)
//			return;
//
//		final long timeoutMillis = 60000L;
//		final long begin = System.currentTimeMillis();
//		while (true) {
//			try {
//				final Socket socket = new Socket("127.0.0.1", localPort);
//				socket.close();
//				return; // success!
//			} catch (final Exception x) {
//				try { Thread.sleep(1000); } catch (final InterruptedException ie) { }
//			}
//
//			if (System.currentTimeMillis() - begin > timeoutMillis)
//				throw new TimeoutException("Server did not start within timeout (ms): " + timeoutMillis);
//		}
//	}

	private void onStopOrFailure() {
		cancelLocalServerStopFileTimerTask();

		synchronized (localServerRunningFile2LocalServer_running) {
			final File localServerRunningFile = getLocalServerRunningFile();
			final String localServerRunningFilePath = localServerRunningFile.getPath();

			if (localServerRunningFile2LocalServer_running.get(localServerRunningFilePath) == this) {
				localServerRunningFile2LocalServer_running.remove(localServerRunningFilePath);
				server = null;
			}

			final File localServerStopFile = getLocalServerStopFile();
			if (localServerStopFile.exists()) {
				localServerStopFile.delete();
				if (localServerStopFile.exists())
					logger.warn("onStopOrFailure: Failed to delete file: {}", localServerStopFile);
				else
					logger.info("onStopOrFailure: Successfully deleted file: {}", localServerStopFile);
			}
			else
				logger.info("onStopOrFailure: File did not exist (could not delete): {}", localServerStopFile);

			if (localServerRunningLockFile != null) {
				localServerRunningLockFile.release();
				localServerRunningLockFile = null;
			}
		}
	}

	private void writeLocalServerProperties() throws IOException {
		final int localPort = getLocalPort();
		if (localPort < 0)
			return;

		final LocalServerPropertiesManager localServerPropertiesManager = LocalServerPropertiesManager.getInstance();
		localServerPropertiesManager.setPort(localPort);
		localServerPropertiesManager.setPassword(new String(AuthManager.getInstance().getCurrentPassword()));
		localServerPropertiesManager.writeLocalServerProperties();
	}

	public void stop() {
		final Server s = getServer();
		if (s != null) {
			try {
				s.stop();
			} catch (final Exception e) {
				throw new RuntimeException();
			}
		}
	}

	public Server getServer() {
		synchronized (localServerRunningFile2LocalServer_running) {
			return server;
		}
	}

	public int getLocalPort() {
		final Server server = getServer();
		if (server == null)
			return -1;

		final Connector[] connectors = server.getConnectors();
		if (connectors.length != 1)
			throw new IllegalStateException("connectors.length != 1");

		return ((ServerConnector) connectors[0]).getLocalPort();
	}

	public int getPort() {
		synchronized (localServerRunningFile2LocalServer_running) {
			if (port < 0) {
				port = ConfigImpl.getInstance().getPropertyAsInt(CONFIG_KEY_PORT, DEFAULT_PORT);
				if (port < 0 || port > 65535) {
					logger.warn("Config key '{}' is set to the value '{}' which is out of range for a port number. Falling back to default port {} ({} meaning a random port).",
							CONFIG_KEY_PORT, port, DEFAULT_PORT, RANDOM_PORT);
					port = DEFAULT_PORT;
				}
			}
			return port;
		}
	}

	public void setPort(final int port) {
		synchronized (localServerRunningFile2LocalServer_running) {
			assertNotRunning();
			this.port = port;
		}
	}

	private boolean isRunning() {
		return getServer() != null;
	}

	private void assertNotRunning() {
		if (isRunning())
			throw new IllegalStateException("Server is already running.");
	}

	private Server createServer() {
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);

		final Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());

		final ServerConnector http = createHttpServerConnector(server);
        server.addConnector(http);

		server.setHandler(createServletContextHandler());
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);

		return server;
	}

	private ServerConnector createHttpServerConnector(Server server) {
		final HttpConfiguration http_config = createHttpConfigurationForHTTP();

        final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setHost("127.0.0.1");
        http.setPort(getPort());
        http.setIdleTimeout(30000);

        return http;
	}

	private HttpConfiguration createHttpConfigurationForHTTP() {
		final HttpConfiguration http_config = new HttpConfiguration();
//		http_config.setSecureScheme("https");
//		http_config.setSecurePort(getSecurePort());
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);
		return http_config;
	}

	private ServletContextHandler createServletContextHandler() {
		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		final ServletContainer servletContainer = new ServletContainer(requireNonNull(createResourceConfig(), "createResourceConfig()"));
		context.addServlet(new ServletHolder(servletContainer), "/*");
//		context.addFilter(GzipFilter.class, "/*", EnumSet.allOf(DispatcherType.class)); // Does not work :-( Using GZip...Interceptor instead ;-)
		return context;
	}

	/**
	 * Creates the actual REST application.
	 * @return the actual REST application. Must not be <code>null</code>.
	 */
	protected ResourceConfig createResourceConfig() {
		return new LocalServerRest();
	}
}
