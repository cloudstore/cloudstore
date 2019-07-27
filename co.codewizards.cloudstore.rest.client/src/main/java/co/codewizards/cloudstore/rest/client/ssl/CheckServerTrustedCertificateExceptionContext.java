package co.codewizards.cloudstore.rest.client.ssl;

import static java.util.Objects.*;

import java.security.cert.X509Certificate;

public class CheckServerTrustedCertificateExceptionContext {

	private X509Certificate[] certificateChain;
	private Throwable error;

	protected CheckServerTrustedCertificateExceptionContext(X509Certificate[] certificateChain, Throwable certificateException) {
		this.certificateChain = requireNonNull(certificateChain, "certificateChain");
		this.error = requireNonNull(certificateException, "error");

		if (certificateChain.length < 1)
			throw new IllegalArgumentException("certificateChain is empty!");
	}

	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}

	public Throwable getError() {
		return error;
	}
}
