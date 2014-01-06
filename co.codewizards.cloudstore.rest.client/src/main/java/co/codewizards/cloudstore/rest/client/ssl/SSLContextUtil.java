package co.codewizards.cloudstore.rest.client.ssl;

import java.io.File;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public final class SSLContextUtil {

	private SSLContextUtil() { }

	public static SSLContext getSSLContext(File trustStoreFile, DynamicX509TrustManagerCallback callback) throws Exception {
		TrustManager[] trustManagers = new TrustManager[] {
				new DynamicX509TrustManager(trustStoreFile, callback)
		};
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustManagers, null);
		return sslContext;
	}
}
