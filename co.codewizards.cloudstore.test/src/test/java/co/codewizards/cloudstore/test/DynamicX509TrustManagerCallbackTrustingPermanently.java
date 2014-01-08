package co.codewizards.cloudstore.test;

import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

class DynamicX509TrustManagerCallbackTrustingPermanently implements DynamicX509TrustManagerCallback {
		private final long[] invocationCounter;

		public DynamicX509TrustManagerCallbackTrustingPermanently() {
			this(null);
		}

		public DynamicX509TrustManagerCallbackTrustingPermanently(long[] invocationCounter) {
			this.invocationCounter = invocationCounter;
			if (invocationCounter != null && invocationCounter.length != 1)
				throw new IllegalArgumentException("invocationCounter.length != 1");
		}

		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
			if (invocationCounter != null)
				++invocationCounter[0];

			CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
//			result.setPermanent(true); // default is true ;-)
			return result;
		}
	}