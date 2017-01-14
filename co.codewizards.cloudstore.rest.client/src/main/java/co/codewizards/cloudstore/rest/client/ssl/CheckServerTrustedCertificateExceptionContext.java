package co.codewizards.cloudstore.rest.client.ssl;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.cert.X509Certificate;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class CheckServerTrustedCertificateExceptionContext {

	private X509Certificate[] certificateChain;
	private Throwable error;

	protected CheckServerTrustedCertificateExceptionContext(X509Certificate[] certificateChain, Throwable certificateException) {
		this.certificateChain = AssertUtil.assertNotNull(certificateChain, "certificateChain");
		this.error = AssertUtil.assertNotNull(certificateException, "error");

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
