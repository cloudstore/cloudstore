package co.codewizards.cloudstore.client.ssl;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CheckServerTrustedCertificateExceptionContext {

	private X509Certificate[] certificateChain;
	private CertificateException certificateException;

	protected CheckServerTrustedCertificateExceptionContext(X509Certificate[] certificateChain, CertificateException certificateException) {
		this.certificateChain = assertNotNull("certificateChain", certificateChain);
		this.certificateException = assertNotNull("certificateException", certificateException);

		if (certificateChain.length < 1)
			throw new IllegalArgumentException("certificateChain is empty!");
	}

	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}

	public CertificateException getCertificateException() {
		return certificateException;
	}
}
