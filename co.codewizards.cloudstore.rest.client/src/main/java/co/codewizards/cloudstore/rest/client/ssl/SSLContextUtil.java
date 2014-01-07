package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import co.codewizards.cloudstore.core.config.ConfigDir;

public final class SSLContextUtil {

	private SSLContextUtil() { }

	public static SSLContext getSSLContext(File trustStoreFile, DynamicX509TrustManagerCallback callback) throws GeneralSecurityException {
		assertNotNull("trustStoreFile", trustStoreFile);
		assertNotNull("callback", callback);
		TrustManager[] trustManagers = new TrustManager[] {
				new DynamicX509TrustManager(trustStoreFile, callback)
		};
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustManagers, null);
		return sslContext;
	}

	public static SSLContext getSSLContext(URL remoteURL, DynamicX509TrustManagerCallback callback) throws GeneralSecurityException {
		assertNotNull("remoteURL", remoteURL);
		assertNotNull("callback", callback);

		String trustStoreFileName = remoteURL.getHost();
		if (remoteURL.getPort() >= 0)
			trustStoreFileName += "_" + remoteURL.getPort();

		trustStoreFileName += ".truststore";

		File sslClient = new File(ConfigDir.getInstance().getFile(), "ssl.client");

		if (!sslClient.isDirectory())
			sslClient.mkdirs();

		if (!sslClient.isDirectory())
			throw new IllegalStateException("Could not create directory (permissions?): " + sslClient);

		return getSSLContext(new File(sslClient, trustStoreFileName), callback);
	}
}
