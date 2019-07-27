package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static java.util.Objects.*;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.HashUtil;

class DynamicX509TrustManager implements X509TrustManager {
	private static final int LOCKFILE_TIMEOUT_MS = 1000 * 10;
	private static final char[] TRUST_STORE_PASSWORD_CHAR_ARRAY = "CloudStore".toCharArray();
	private final File trustStoreFile;
	private final DynamicX509TrustManagerCallback callback;
	private X509TrustManager trustManager;
	private final List<Certificate> tempCertList = new ArrayList<Certificate>();

	public DynamicX509TrustManager(final File trustStoreFile, final DynamicX509TrustManagerCallback callback) {
		this.trustStoreFile = requireNonNull(trustStoreFile, "trustStoreFile");
		this.callback = requireNonNull(callback, "callback");
		reloadTrustManager();
	}

	@Override
	public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
		requireNonNull(chain, "chain");
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
			final byte[] certEncoded = requireNonNull(cert, "cert").getEncoded();
			final byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(certEncoded));
			return HashUtil.encodeHexStr(hash);
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	private KeyStore readTrustStore() {
		try (final LockFile lockFile = acquireTrustStoreFileLockFile();) {
			final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

			try (final InputStream in = new BufferedInputStream(castStream(lockFile.createInputStream()))) {
				in.mark(1);
				final boolean empty = in.read() < 0;
				in.reset();

				ks.load(empty ? null : in, TRUST_STORE_PASSWORD_CHAR_ARRAY);
			}
			return ks;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	private void writeTrustStore(final KeyStore trustStore) {
		try (final LockFile lockFile = acquireTrustStoreFileLockFile();) {
			try (final OutputStream out = castStream(lockFile.createOutputStream())) {
				trustStore.store(out, TRUST_STORE_PASSWORD_CHAR_ARRAY);
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
				try (final LockFile lockFile = acquireTrustStoreFileLockFile();) {
					final Lock lock = lockFile.getLock();
					lock.lock();
					try {
						final KeyStore trustStore = readTrustStore();
						trustStore.setCertificateEntry(sha1(cert), cert);
						writeTrustStore(trustStore);
					} finally {
						lock.unlock();
					}
				}
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

	private LockFile acquireTrustStoreFileLockFile() {
		return LockFileFactory.getInstance().acquire(trustStoreFile, LOCKFILE_TIMEOUT_MS);
	}
}