package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;

public final class SSLContextBuilder {

	private URL remoteURL;
	private File trustStoreFile;
	private DynamicX509TrustManagerCallback callback;

	private SSLContextBuilder() { }

	public static SSLContextBuilder create() {
		return new SSLContextBuilder();
	}

	public DynamicX509TrustManagerCallback getCallback() {
		return callback;
	}
	public void setCallback(final DynamicX509TrustManagerCallback callback) {
		this.callback = callback;
	}
	public SSLContextBuilder callback(final DynamicX509TrustManagerCallback callback) {
		setCallback(callback);
		return this;
	}

	public URL getRemoteURL() {
		return remoteURL;
	}
	public void setRemoteURL(final URL remoteURL) {
		this.remoteURL = remoteURL;
	}
	public SSLContextBuilder remoteURL(final URL remoteURL) {
		setRemoteURL(remoteURL);
		return this;
	}

	public File getTrustStoreFile() {
		return trustStoreFile;
	}
	public void setTrustStoreFile(final File trustStoreFile) {
		this.trustStoreFile = trustStoreFile;
	}
	public SSLContextBuilder trustStoreFile(final File trustStoreFile) {
		setTrustStoreFile(trustStoreFile);
		return this;
	}

	public SSLContext build() throws GeneralSecurityException {
		final File trustStoreFile = getTrustStoreFile();
		if (trustStoreFile != null) {
			if (getRemoteURL() != null)
				throw new IllegalStateException("remoteURL and trustStoreFile are both set! Only one of these should be set!");

			return getSSLContext(trustStoreFile, getCallback());
		}
		else
			return getSSLContext(getRemoteURL(), getCallback());
	}

	private SSLContext getSSLContext(final File trustStoreFile, final DynamicX509TrustManagerCallback callback) throws GeneralSecurityException {
		requireNonNull(trustStoreFile, "trustStoreFile");
		requireNonNull(callback, "callback");
		final TrustManager[] trustManagers = new TrustManager[] {
				new DynamicX509TrustManager(trustStoreFile, callback)
		};

		// http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext
		// http://en.wikipedia.org/wiki/Secure_Sockets_Layer#Cipher
		final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, trustManagers, null);
		return sslContext;
	}

	private SSLContext getSSLContext(final URL remoteURL, final DynamicX509TrustManagerCallback callback) throws GeneralSecurityException {
		requireNonNull(remoteURL, "remoteURL");
		requireNonNull(callback, "callback");

		String trustStoreFileName = remoteURL.getHost();
		if (remoteURL.getPort() >= 0)
			trustStoreFileName += "_" + remoteURL.getPort();

		trustStoreFileName += ".truststore";

		final File sslClient = createFile(ConfigDir.getInstance().getFile(), "ssl.client");

		if (!sslClient.isDirectory())
			sslClient.mkdirs();

		if (!sslClient.isDirectory())
			throw new IllegalStateException("Could not create directory (permissions?): " + sslClient);

		return getSSLContext(createFile(sslClient, trustStoreFileName), callback);
	}
}
