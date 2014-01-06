package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import co.codewizards.cloudstore.core.util.HashUtil;

class DynamicX509TrustManager implements X509TrustManager {
	private static final char[] TRUST_STORE_PASSWORD_CHAR_ARRAY = "CloudStore".toCharArray();
	private final File trustStoreFile;
	private final DynamicX509TrustManagerCallback callback;
	private X509TrustManager trustManager;
	private List<Certificate> tempCertList = new ArrayList<Certificate>();

	public DynamicX509TrustManager(File trustStoreFile, DynamicX509TrustManagerCallback callback) {
		this.trustStoreFile = assertNotNull("trustStoreFile", trustStoreFile);
		this.callback = assertNotNull("callback", callback);
		reloadTrustManager();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		assertNotNull("chain", chain);
		if (chain.length < 1)
			throw new IllegalArgumentException("chain is empty!");

		try {
			trustManager.checkServerTrusted(chain, authType);
//		} catch (CertificateException cx) {
		} catch (Exception cx) {
			CheckServerTrustedCertificateExceptionResult result = callback.handleCheckServerTrustedCertificateException(
					new CheckServerTrustedCertificateExceptionContext(chain, cx));
			if (result == null)
				throw new IllegalStateException("Implementation error: callback.handleCheckServerTrustedCertificateException(...) returned null! callback.class=" + callback.getClass().getName());

			if (!result.isTrusted()) {
				if (cx instanceof RuntimeException)
					throw (RuntimeException)cx;

				if (cx instanceof CertificateException)
					throw (CertificateException)cx;

				throw new RuntimeException(cx);
			}

			addServerCertAndReload(chain[0], result.isPermanent());
			trustManager.checkServerTrusted(chain, authType);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		X509Certificate[] issuers = trustManager.getAcceptedIssuers();
		return issuers;
	}

	private void reloadTrustManager() {
		try {
			KeyStore trustStore = readTrustStore();

			// add all temporary certs to KeyStore (ks)
			for (Certificate cert : tempCertList) {
				trustStore.setCertificateEntry(sha1(cert), cert);
			}

			// initialize a new TMF with the ts we just loaded
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);

			// acquire X509 trust manager from factory
			TrustManager tms[] = tmf.getTrustManagers();
			for (int i = 0; i < tms.length; i++) {
				if (tms[i] instanceof X509TrustManager) {
					trustManager = (X509TrustManager)tms[i];
					return;
				}
			}

			throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	private String sha1(Certificate cert) {
		try {
			byte[] certEncoded = assertNotNull("cert", cert).getEncoded();
			byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(certEncoded));
			return HashUtil.encodeHexStr(hash);
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	private KeyStore readTrustStore() {
		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			@SuppressWarnings("resource") // wrongly detected - there is NO leak! there is a finally block closing it!
			InputStream in = trustStoreFile.exists() ? new FileInputStream(trustStoreFile) : null;
			try {
				ks.load(in, null);
			} finally {
				if (in != null)
					in.close();
			}
			return ks;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	private void writeTrustStore(KeyStore trustStore) {
		try {
			FileOutputStream out = new FileOutputStream(trustStoreFile);
			try {
				trustStore.store(out, TRUST_STORE_PASSWORD_CHAR_ARRAY);
			} finally {
				out.close();
			}
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	private void addServerCertAndReload(Certificate cert, boolean permanent) {
		try {
			if (permanent) {
				KeyStore trustStore = readTrustStore();
				trustStore.setCertificateEntry(sha1(cert), cert);
				writeTrustStore(trustStore);
			} else {
				tempCertList.add(cert);
			}
			reloadTrustManager();
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}
}