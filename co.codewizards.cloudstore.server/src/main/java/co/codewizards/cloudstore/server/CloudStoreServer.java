package co.codewizards.cloudstore.server;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.appid.AppIdRegistry;
import co.codewizards.cloudstore.core.auth.BouncyCastleRegistrationUtil;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.DerbyUtil;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.MainArgsUtil;
import co.codewizards.cloudstore.local.test.config.ConfigDir;
import co.codewizards.cloudstore.local.test.config.ConfigImpl;
import co.codewizards.cloudstore.ls.server.LocalServer;
import co.codewizards.cloudstore.rest.server.CloudStoreRest;

public class CloudStoreServer implements Runnable {
	public static final String CONFIG_KEY_SECURE_PORT = "server.securePort";

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreServer.class);

	private static Class<? extends CloudStoreServer> cloudStoreServerClass = CloudStoreServer.class;

	private static final int DEFAULT_SECURE_PORT = 8443;

	private static final String CERTIFICATE_ALIAS = "CloudStoreServer";
	private static final String CERTIFICATE_COMMON_NAME = CERTIFICATE_ALIAS;

	// TODO the passwords are necessary. we get exceptions without them. so maybe we should somehow make this secure, later.
	private static final String KEY_STORE_PASSWORD_STRING = "CloudStore-key-store";
	private static final char[] KEY_STORE_PASSWORD_CHAR_ARRAY = KEY_STORE_PASSWORD_STRING.toCharArray();
	private static final String KEY_PASSWORD_STRING = "CloudStore-private-key";
	private static final char[] KEY_PASSWORD_CHAR_ARRAY = KEY_PASSWORD_STRING.toCharArray();

	private File keyStoreFile;
	private final SecureRandom random = new SecureRandom();
	private int securePort;
	private final AtomicBoolean running = new AtomicBoolean();
	private Server server;
	private CloudStoreUpdaterTimer updaterTimer;

	public static void main(String[] args) throws Exception {
		args = MainArgsUtil.extractAndApplySystemPropertiesReturnOthers(args);
		initLogging();
		try {
			createCloudStoreServer(args).run();
		} catch (final Throwable x) {
			logger.error(x.toString(), x);
			System.exit(999);
		}
	}

	public CloudStoreServer(final String... args) {
		BouncyCastleRegistrationUtil.registerBouncyCastleIfNeeded();
	}

	protected static Constructor<? extends CloudStoreServer> getCloudStoreServerConstructor() throws NoSuchMethodException, SecurityException {
		final Class<? extends CloudStoreServer> clazz = getCloudStoreServerClass();
		final Constructor<? extends CloudStoreServer> constructor = clazz.getConstructor(String[].class);
		return constructor;
	}

	protected static CloudStoreServer createCloudStoreServer(final String[] args) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final Constructor<? extends CloudStoreServer> constructor = getCloudStoreServerConstructor();
		final CloudStoreServer cloudStoreServer = constructor.newInstance(new Object[] { args });
		return cloudStoreServer;
	}

	protected static Class<? extends CloudStoreServer> getCloudStoreServerClass() {
		return cloudStoreServerClass;
	}
	protected static void setCloudStoreServerClass(final Class<? extends CloudStoreServer> cloudStoreServerClass) {
		requireNonNull(cloudStoreServerClass, "cloudStoreServerClass");
		CloudStoreServer.cloudStoreServerClass = cloudStoreServerClass;
	}

	@Override
	public void run() {
		if (!running.compareAndSet(false, true))
			throw new IllegalStateException("Server is already running!");

		LocalServer localServer = null;
		try {
			initKeyStore();
			synchronized (this) {
				localServer = createLocalServer();
				if (!localServer.start())
					localServer = null;

				server = createServer();
				server.start();

				updaterTimer = createUpdaterTimer();
				updaterTimer.start();
			}

			server.join();

		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		} finally {
			synchronized (this) {
				if (localServer != null) {
					try {
						localServer.stop();
					} catch (Exception x) {
						logger.warn("localServer.stop() failed: " + x, x);
					}
					localServer = null;
				}
				server = null;
			}

			running.set(false);
		}
	}

	protected CloudStoreUpdaterTimer createUpdaterTimer() {
		return new CloudStoreUpdaterTimer();
	}

	protected LocalServer createLocalServer() {
		return new LocalServer();
	}

	public synchronized void stop() {
		if (updaterTimer != null)
			updaterTimer.stop();

		if (server != null) {
			try {
				server.stop();
			} catch (final Exception e) {
				throw new RuntimeException();
			}
		}
	}

	public synchronized File getKeyStoreFile() {
		if (keyStoreFile == null) {
			final File sslServer = createFile(ConfigDir.getInstance().getFile(), "ssl.server");

			if (!sslServer.isDirectory())
				sslServer.mkdirs();

			if (!sslServer.isDirectory())
				throw new IllegalStateException("Could not create directory: " + sslServer);

			keyStoreFile = createFile(sslServer, "keystore");
		}
		return keyStoreFile;
	}

	public synchronized void setKeyStoreFile(final File keyStoreFile) {
		assertNotRunning();
		this.keyStoreFile = keyStoreFile;
	}

	public synchronized int getSecurePort() {
		if (securePort <= 0) {
			securePort = ConfigImpl.getInstance().getPropertyAsInt(CONFIG_KEY_SECURE_PORT, DEFAULT_SECURE_PORT);
			if (securePort < 1 || securePort > 65535) {
				logger.warn("Config key '{}' is set to the value '{}' which is out of range for a port number. Falling back to default port {}.",
						CONFIG_KEY_SECURE_PORT, securePort, DEFAULT_SECURE_PORT);
				securePort = DEFAULT_SECURE_PORT;
			}
		}
		return securePort;
	}

	public synchronized void setSecurePort(final int securePort) {
		assertNotRunning();
		this.securePort = securePort;
	}

	private void assertNotRunning() {
		if (running.get())
			throw new IllegalStateException("Server is already running.");
	}

	private void initKeyStore() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SecurityException, SignatureException, NoSuchProviderException, UnrecoverableEntryException {
		if (!getKeyStoreFile().exists()) {
			logger.info("initKeyStore: keyStoreFile='{}' does not exist!", getKeyStoreFile());
			logger.info("initKeyStore: Creating RSA key pair (this might take a while)...");
			System.out.println("**********************************************************************");
			System.out.println("There is no key, yet. Creating a new RSA key pair, now. This might");
			System.out.println("take a while (a few seconds up to a few minutes). Please be patient!");
			System.out.println("**********************************************************************");
			final long keyGenStartTimestamp = System.currentTimeMillis();
			final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, KEY_STORE_PASSWORD_CHAR_ARRAY);

			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(4096, random); // TODO make configurable
			final KeyPair pair = keyGen.generateKeyPair();

			final X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

			final long serial = new SecureRandom().nextLong();

			v3CertGen.setSerialNumber(BigInteger.valueOf(serial).abs());
			v3CertGen.setIssuerDN(new X509Principal("CN=" + CERTIFICATE_COMMON_NAME + ", OU=None, O=None, C=None"));
			v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 3)));
			v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
			v3CertGen.setSubjectDN(new X509Principal("CN=" + CERTIFICATE_COMMON_NAME + ", OU=None, O=None, C=None"));

			v3CertGen.setPublicKey(pair.getPublic());
			v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

			final X509Certificate pkCertificate = v3CertGen.generateX509Certificate(pair.getPrivate());

			final PrivateKeyEntry entry = new PrivateKeyEntry(pair.getPrivate(), new Certificate[]{ pkCertificate });
			ks.setEntry(CERTIFICATE_ALIAS, entry, new KeyStore.PasswordProtection(KEY_PASSWORD_CHAR_ARRAY));

			final OutputStream fos = castStream(getKeyStoreFile().createOutputStream());
			try {
				ks.store(fos, KEY_STORE_PASSWORD_CHAR_ARRAY);
			} finally {
				fos.close();
			}

			final long keyGenDuration = System.currentTimeMillis() - keyGenStartTimestamp;
			logger.info("initKeyStore: Creating RSA key pair took {} ms.", keyGenDuration);
			System.out.println(String.format("Generating a new RSA key pair took %s ms.", keyGenDuration));
		}

		final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		final InputStream fis = castStream(getKeyStoreFile().createInputStream());
		try {
			ks.load(fis, KEY_STORE_PASSWORD_CHAR_ARRAY);
		} finally {
			fis.close();
		}
		final X509Certificate certificate = (X509Certificate) ks.getCertificate(CERTIFICATE_ALIAS);
		final String certificateSha1 = HashUtil.sha1ForHuman(certificate.getEncoded());
		System.out.println("**********************************************************************");
		System.out.println("Server certificate fingerprint (SHA1):");
		System.out.println();
		System.out.println("    " + certificateSha1);
		System.out.println();
		System.out.println("Use this fingerprint to verify on the client-side, whether you're");
		System.out.println("really talking to this server. If the client shows you a different");
		System.out.println("value, someone is tampering with your connection!");
		System.out.println();
		System.out.println("Please keep this fingerprint at a safe place. You'll need it whenever");
		System.out.println("one of your clients connects to this server for the first time.");
		System.out.println("**********************************************************************");
		logger.info("initKeyStore: RSA fingerprint (SHA1): {}", certificateSha1);
	}

	protected Server createServer() {
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);

		final Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());

		final HttpConfiguration http_config = createHttpConfigurationForHTTP();

//        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
//        http.setPort(8080);
//        http.setIdleTimeout(30000);
//        server.addConnector(http);

		server.setHandler(createServletContextHandler());
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);

		final HttpConfiguration https_config = createHttpConfigurationForHTTPS(http_config);
		server.addConnector(createServerConnectorForHTTPS(server, https_config));

		return server;
	}

	private HttpConfiguration createHttpConfigurationForHTTP() {
		final HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		http_config.setSecurePort(getSecurePort());
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);
		return http_config;
	}

	private HttpConfiguration createHttpConfigurationForHTTPS(final HttpConfiguration httpConfigurationForHTTP) {
		final HttpConfiguration https_config = new HttpConfiguration(httpConfigurationForHTTP);
		https_config.addCustomizer(new SecureRequestCustomizer());
		return https_config;
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
		return new CloudStoreRest();
	}

	private ServerConnector createServerConnectorForHTTPS(final Server server, final HttpConfiguration httpConfigurationForHTTPS) {
		final SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(getKeyStoreFile().getPath());
		sslContextFactory.setKeyStorePassword(KEY_STORE_PASSWORD_STRING);
		sslContextFactory.setKeyManagerPassword(KEY_PASSWORD_STRING);
		sslContextFactory.setTrustStorePath(getKeyStoreFile().getPath());
		sslContextFactory.setTrustStorePassword(KEY_STORE_PASSWORD_STRING);

		sslContextFactory.setExcludeCipherSuites( // TODO make this configurable!
//				"SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
//				"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
// Using wildcards instead. This should be much safer:
				".*RC4.*",
				".*DES.*");
		//        sslContextFactory.setCertAlias(CERTIFICATE_ALIAS); // Jetty uses our certificate. We put only one single cert into the key store. Hence, we don't need this.

		final ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpConfigurationForHTTPS));
		sslConnector.setPort(getSecurePort());
//		sslConnector.setIdleTimeout(300*1000);
//		sslConnector.setStopTimeout(30*1000);
//		sslConnector.setSoLingerTime(10);
		return sslConnector;
	}

	private static void initLogging() throws IOException, JoranException {
		final File logDir = ConfigDir.getInstance().getLogDir();
		DerbyUtil.setLogFile(createFile(logDir, "derby.log"));

		final String logbackXmlName = "logback.server.xml";
		final File logbackXmlFile = createFile(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists()) {
			AppIdRegistry.getInstance().copyResourceResolvingAppId(
					CloudStoreServer.class, logbackXmlName, logbackXmlFile);
		}

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    try {
	      final JoranConfigurator configurator = new JoranConfigurator();
	      configurator.setContext(context);
	      // Call context.reset() to clear any previous configuration, e.g. default
	      // configuration. For multi-step configuration, omit calling context.reset().
	      context.reset();
	      configurator.doConfigure(logbackXmlFile.getIoFile());
	    } catch (final JoranException je) {
	    	// StatusPrinter will handle this
	    	doNothing();
	    }
	    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}
}
