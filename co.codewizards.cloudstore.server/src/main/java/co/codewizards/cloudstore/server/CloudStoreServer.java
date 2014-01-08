package co.codewizards.cloudstore.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.rest.server.CloudStoreREST;

public class CloudStoreServer implements Runnable {
	public static final String SYSTEM_PROPERTY_SECURE_PORT = "cloudstore.securePort";

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreServer.class);

	private static final int DEFAULT_SECURE_PORT = 8443;

	private static final String CERTIFICATE_ALIAS = "CloudStoreServer";
	private static final String CERTIFICATE_COMMON_NAME = CERTIFICATE_ALIAS;

	// TODO the passwords are necessary. we get exceptions without them. so maybe we should somehow make this secure, later.
	private static final String KEY_STORE_PASSWORD_STRING = "CloudStore-key-store";
	private static final char[] KEY_STORE_PASSWORD_CHAR_ARRAY = KEY_STORE_PASSWORD_STRING.toCharArray();
	private static final String KEY_PASSWORD_STRING = "CloudStore-private-key";
	private static final char[] KEY_PASSWORD_CHAR_ARRAY = KEY_PASSWORD_STRING.toCharArray();

	private File keyStoreFile;
	private SecureRandom random = new SecureRandom();
	private int securePort;
	private final AtomicBoolean running = new AtomicBoolean();
	private Server server;

	public static void main(String[] args) {
		new CloudStoreServer().run();
	}

	public CloudStoreServer() {
		Provider provider = Security.getProvider("BC");
		if (provider == null)
			Security.addProvider(new BouncyCastleProvider());

		provider = Security.getProvider("BC");
		if (provider == null)
			throw new IllegalStateException("Registration of BouncyCastleProvider failed!");
	}

	@Override
	public void run() {
		if (!running.compareAndSet(false, true))
			throw new IllegalStateException("Server is already running!");

		try {
			initKeyStore();
			synchronized (this) {
				server = createServer();
				server.start();
			}

			server.join();

			synchronized (this) {
				server = null;
			}
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		} finally {
			running.set(false);
		}
	}

	public synchronized void stop() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				throw new RuntimeException();
			}
		}
	}

	public synchronized File getKeyStoreFile() {
		if (keyStoreFile == null) {
			File sslServer = new File(ConfigDir.getInstance().getFile(), "ssl.server");

			if (!sslServer.isDirectory())
				sslServer.mkdirs();

			if (!sslServer.isDirectory())
				throw new IllegalStateException("Could not create directory: " + sslServer);

			keyStoreFile = new File(sslServer, "keystore");
		}
		return keyStoreFile;
	}

	public synchronized void setKeyStoreFile(File keyStoreFile) {
		assertNotRunning();
		this.keyStoreFile = keyStoreFile;
	}

	public synchronized int getSecurePort() {
		if (securePort <= 0) {
			String value = System.getProperty(SYSTEM_PROPERTY_SECURE_PORT);
			if (value == null) {
				logger.debug("System property '{}' not set. Listening on default port {}.", SYSTEM_PROPERTY_SECURE_PORT, DEFAULT_SECURE_PORT);
				securePort = DEFAULT_SECURE_PORT;
			}
			else {
				try {
					securePort = Integer.valueOf(value);
				} catch (NumberFormatException x) {
					logger.warn("System property '{}' is set to the value '{}' which is not an integer. Falling back to default port {}.",
							SYSTEM_PROPERTY_SECURE_PORT, value, DEFAULT_SECURE_PORT);
					securePort = DEFAULT_SECURE_PORT;
				}
				if (securePort < 1 || securePort > 65535) {
					logger.warn("System property '{}' is set to the value '{}' which is out of range. Falling back to default port {}.",
							SYSTEM_PROPERTY_SECURE_PORT, securePort, DEFAULT_SECURE_PORT);
					securePort = DEFAULT_SECURE_PORT;
				}
			}
		}
		return securePort;
	}

	public synchronized void setSecurePort(int securePort) {
		assertNotRunning();
		this.securePort = securePort;
	}

	private void assertNotRunning() {
		if (running.get())
			throw new IllegalStateException("Server is already running.");
	}

	private void initKeyStore() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SecurityException, SignatureException, NoSuchProviderException {
		if (getKeyStoreFile().exists()) {
			return;
		}

		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, KEY_STORE_PASSWORD_CHAR_ARRAY); // TODO check if we can omit this password. makes no sense for security, anyway.

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(4096, random); // TODO make configurable
		KeyPair pair = keyGen.generateKeyPair();

		X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

		long serial = new SecureRandom().nextLong();

		v3CertGen.setSerialNumber(BigInteger.valueOf(serial).abs());
		v3CertGen.setIssuerDN(new X509Principal("CN=" + CERTIFICATE_COMMON_NAME + ", OU=None, O=None, C=None"));
		v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 3)));
		v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
		v3CertGen.setSubjectDN(new X509Principal("CN=" + CERTIFICATE_COMMON_NAME + ", OU=None, O=None, C=None"));

		v3CertGen.setPublicKey(pair.getPublic());
		v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

		X509Certificate pkCertificate = v3CertGen.generateX509Certificate(pair.getPrivate());

		PrivateKeyEntry entry = new PrivateKeyEntry(pair.getPrivate(), new Certificate[]{ pkCertificate });
		ks.setEntry(CERTIFICATE_ALIAS, entry, new KeyStore.PasswordProtection(KEY_PASSWORD_CHAR_ARRAY));

		FileOutputStream fos = new FileOutputStream(getKeyStoreFile());
		try {
			ks.store(fos, KEY_STORE_PASSWORD_CHAR_ARRAY);
		} finally {
			fos.close();
		}
	}

	private Server createServer() {
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);

		Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());

		HttpConfiguration http_config = createHttpConfigurationForHTTP();

//        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
//        http.setPort(8080);
//        http.setIdleTimeout(30000);
//        server.addConnector(http);

		server.setHandler(createServletContextHandler());
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);

		HttpConfiguration https_config = createHttpConfigurationForHTTPS(http_config);
		server.addConnector(createServerConnectorForHTTPS(server, https_config));

		return server;
	}

	private HttpConfiguration createHttpConfigurationForHTTP() {
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		http_config.setSecurePort(getSecurePort());
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);
		return http_config;
	}

	private HttpConfiguration createHttpConfigurationForHTTPS(HttpConfiguration httpConfigurationForHTTP) {
		HttpConfiguration https_config = new HttpConfiguration(httpConfigurationForHTTP);
		https_config.addCustomizer(new SecureRequestCustomizer());
		return https_config;
	}

	private ServletContextHandler createServletContextHandler() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		ServletContainer servletContainer = new ServletContainer(new CloudStoreREST());
//		com.sun.jersey.spi.container.servlet.ServletContainer servletContainer = new com.sun.jersey.spi.container.servlet.ServletContainer(CloudStoreREST.class);
		context.addServlet(new ServletHolder(servletContainer), "/*");
		return context;
	}

	private ServerConnector createServerConnectorForHTTPS(Server server, HttpConfiguration httpConfigurationForHTTPS) {
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(getKeyStoreFile().getPath());
		sslContextFactory.setKeyStorePassword(KEY_STORE_PASSWORD_STRING);
		sslContextFactory.setKeyManagerPassword(KEY_PASSWORD_STRING);
		sslContextFactory.setTrustStorePath(getKeyStoreFile().getPath());
		sslContextFactory.setTrustStorePassword(KEY_STORE_PASSWORD_STRING);
		sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", ".*RC4.*");
		//        sslContextFactory.setCertAlias(CERTIFICATE_ALIAS); // Jetty uses our certificate. We put only one single cert into the key store. Hence, we don't need this.

		ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpConfigurationForHTTPS));
		sslConnector.setPort(getSecurePort());
		return sslConnector;
	}
}
