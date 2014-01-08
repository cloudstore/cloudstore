package co.codewizards.cloudstore.test;

import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;

class DynamicX509TrustManagerCallbackTrustingTemporarily extends DynamicX509TrustManagerCallbackTrustingPermanently {

	public DynamicX509TrustManagerCallbackTrustingTemporarily() { }

	public DynamicX509TrustManagerCallbackTrustingTemporarily(long[] invocationCounter) {
		super(invocationCounter);
	}

	@Override
	public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
		CheckServerTrustedCertificateExceptionResult result = super.handleCheckServerTrustedCertificateException(context);
		result.setPermanent(false);
		return result;
	}
}