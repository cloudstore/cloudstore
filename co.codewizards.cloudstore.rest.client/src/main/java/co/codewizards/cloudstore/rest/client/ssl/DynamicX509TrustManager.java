package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

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
	private final List<Certificate> tempCertList = new ArrayList<Certificate>();

	public DynamicX509TrustManager(final File trustStoreFile, final DynamicX509TrustManagerCallback callback) {
		this.trustStoreFile = assertNotNull("trustStoreFile", trustStoreFile);
		this.callback = assertNotNull("callback", callback);
		reloadTrustManager();
	}

	@Override
	public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
		assertNotNull("chain", chain);
		if (chain.length < 1)
			throw new IllegalArgumentException("chain is empty!");

		try {
			trustManager.checkServerTrusted(chain, authType);
		} catch (final Exception cx) {
			final CheckServerTrustedCertificateExceptionResult result = callback.handleCheckServerTrustedCertificateException(
					new CheckServerTrustedCertificateExceptionContext(chain, cx));
			if (result == null)
				throw new IllegalStateException("Implementation error: callback.handleCheckServerTrustedCertificateException(...) returned null! callback.class=" + callback.getClass().getName());

			if (!result.isTrusted())
				throw new CallbackDeniedTrustException(cx);

			addServerCertAndReload(chain[0], result.isPermanent());
			trustManager.checkServerTrusted(chain, authType);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		final X509Certificate[] issuers = trustManager.getAcceptedIssuers();
		return issuers;
	}

	private void reloadTrustManager() {
		try {
			final KeyStore trustStore = readTrustStore();

			// add all temporary certs to KeyStore (ks)
			for (final Certificate cert : tempCertList) {
				trustStore.setCertificateEntry(sha1(cert), cert);
			}

			// initialize a new TMF with the ts we just loaded
			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);

			// acquire X509 trust manager from factory
			final TrustManager tms[] = tmf.getTrustManagers();
			for (int i = 0; i < tms.length; i++) {
				if (tms[i] instanceof X509TrustManager) {
					trustManager = (X509TrustManager)tms[i];
					return;
				}
			}

			throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	private String sha1(final Certificate cert) {
		try {
			final byte[] certEncoded = assertNotNull("cert", cert).getEncoded();
			final byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(certEncoded));
			return HashUtil.encodeHexStr(hash);
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	private KeyStore readTrustStore() {
		try {
			final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			final InputStream in = trustStoreFile.exists() && trustStoreFile.length() > 0 ? new FileInputStream(trustStoreFile) : null;
			try {
				ks.load(in, null);
			} finally {
				if (in != null)
					in.close();
			}
			return ks;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	private void writeTrustStore(final KeyStore trustStore) {
		try {
			final File tmpFile = new File(trustStoreFile.getParentFile(), trustStoreFile.getName() + ".new");
			try {
				final FileOutputStream out = new FileOutputStream(tmpFile);
				try {
					trustStore.store(out, TRUST_STORE_PASSWORD_CHAR_ARRAY);
				} finally {
					out.close();
				}
				trustStoreFile.delete();
				tmpFile.renameTo(trustStoreFile);
			} finally {
				tmpFile.delete();
			}
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	private void addServerCertAndReload(final Certificate cert, final boolean permanent) {
		try {
			if (permanent) {
				final KeyStore trustStore = readTrustStore();
				trustStore.setCertificateEntry(sha1(cert), cert);
				writeTrustStore(trustStore);
			} else {
				tempCertList.add(cert);
			}
			reloadTrustManager();
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}
}