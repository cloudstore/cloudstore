package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.util.DateUtil.*;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

class DynamicX509TrustManagerCallbackSleepUntilTimeout implements DynamicX509TrustManagerCallback {
	private final long[] invocationCounter;

	private static final Logger logger = LoggerFactory.getLogger(DynamicX509TrustManagerCallbackSleepUntilTimeout.class.getName());

	private final long sleepInMsec;

	public DynamicX509TrustManagerCallbackSleepUntilTimeout(long[] invocationCounter, long sleepInMsec) {
		this.invocationCounter = invocationCounter;
		this.sleepInMsec = sleepInMsec;
		if (invocationCounter != null && invocationCounter.length != 1)
			throw new IllegalArgumentException("invocationCounter.length != 1");
	}

	@Override
	public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
		if (invocationCounter != null) {
			++invocationCounter[0];
			logger.warn("handleCheckServerTrustedCertificateException: invocationCounter={}", invocationCounter[0]);
		}

		CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
		if (invocationCounter[0] == 1) {
			try {
				logger.warn("\n####################################################\n"
						+ "###\n"
						+ "###    This thread will sleep for " + sleepInMsec + " msec (" + now() + ")\n"
						+ "###\n"
						+ "####################################################");
				// we are blocking the *first* invocation
				Thread.sleep(sleepInMsec);
			} catch (InterruptedException e) {
				logger.error("Thread.sleep interrupted!", e);
			}
		}
		result.setTrusted(true);
//		result.setPermanent(true); // default is true ;-)
		return result;
	}
}