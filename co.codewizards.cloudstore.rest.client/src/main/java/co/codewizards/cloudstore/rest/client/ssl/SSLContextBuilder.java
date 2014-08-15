package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.util.Util.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import co.codewizards.cloudstore.core.config.ConfigDir;

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
	public void setCallback(DynamicX509TrustManagerCallback callback) {
		this.callback = callback;
	}
	public SSLContextBuilder callback(DynamicX509TrustManagerCallback callback) {
		setCallback(callback);
		return this;
	}

	public URL getRemoteURL() {
		return remoteURL;
	}
	public void setRemoteURL(URL remoteURL) {
		this.remoteURL = remoteURL;
	}
	public SSLContextBuilder remoteURL(URL remoteURL) {
		setRemoteURL(remoteURL);
		return this;
	}

	public File getTrustStoreFile() {
		return trustStoreFile;
	}
	public void setTrustStoreFile(File trustStoreFile) {
		this.trustStoreFile = trustStoreFile;
	}
	public SSLContextBuilder trustStoreFile(File trustStoreFile) {
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

	private SSLContext getSSLContext(File trustStoreFile, DynamicX509TrustManagerCallback callback) throws GeneralSecurityException {
		assertNotNull("trustStoreFile", trustStoreFile);
		assertNotNull("callback", callback);
		TrustManager[] trustManagers = new TrustManager[] {
				new DynamicX509TrustManager(trustStoreFile, callback)
		};

		// http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext
		// http://en.wikipedia.org/wiki/Secure_Sockets_Layer#Cipher
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, trustManagers, null);
		return sslContext;
	}

	private SSLContext getSSLContext(URL remoteURL, DynamicX509TrustManagerCallback callback) throws GeneralSecurityException {
		assertNotNull("remoteURL", remoteURL);
		assertNotNull("callback", callback);

		String trustStoreFileName = remoteURL.getHost();
		if (remoteURL.getPort() >= 0)
			trustStoreFileName += "_" + remoteURL.getPort();

		trustStoreFileName += ".truststore";

		File sslClient = newFile(ConfigDir.getInstance().getFile(), "ssl.client");

		if (!sslClient.isDirectory())
			sslClient.mkdirs();

		if (!sslClient.isDirectory())
			throw new IllegalStateException("Could not create directory (permissions?): " + sslClient);

		return getSSLContext(newFile(sslClient, trustStoreFileName), callback);
	}
}
