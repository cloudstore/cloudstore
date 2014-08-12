package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.SocketException;

import javax.ws.rs.ProcessingException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.command.TestCommand;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextBuilder;

/**
 * Caution: These are long running tests!
 * <p>
 * These tests ensure the timeout and idle times of the current configuration.
 * Also they check for certificate validation after a longer break.
 *
 * @author Sebastian Schefczyk
 */
public class CertificateHandlingAndTestServiceLRT extends AbstractIT {

	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateHandlingAndTestServiceLRT.class);

	@Test
	public void oneTimeoutPeriodSleeping_31() throws Exception {
		final File trustStoreFile = CertificateHandlingAndTestServiceIT.getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl());
		final long sleepInMsec = 1000 * (30 + 1);
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackSleepUntilTimeout(
				handleCertificateExceptionCounter, sleepInMsec);
		cloudStoreRestClient.setSslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		cloudStoreRestClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(0);

		cloudStoreRestClient.execute(new TestCommand(false));
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
	}

	@Test
	public void oneTimeoutPeriodSleeping_61() throws Exception {
		final File trustStoreFile = CertificateHandlingAndTestServiceIT.getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl());
		final long sleepInMsec = 1000 * (60 + 1);
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackSleepUntilTimeout(
				handleCertificateExceptionCounter, sleepInMsec);
		cloudStoreRestClient.setSslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		cloudStoreRestClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(0);

		cloudStoreRestClient.execute(new TestCommand(false));
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
	}

	@Test
	public void almostTimedOut_5min() throws Exception {
		final File trustStoreFile = CertificateHandlingAndTestServiceIT.getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl());
		final long sleepInMsec = 1000 * (60 * 5 - 3);
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackSleepUntilTimeout(
				handleCertificateExceptionCounter, sleepInMsec);
		cloudStoreRestClient.setSslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		cloudStoreRestClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(0);
		cloudStoreRestClient.execute(new TestCommand(false));
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
	}

	@Ignore("This test is ignored, as this states a strange but good situation: Its currently working *longer* than "
			+ "expected, without any longer timeouts on server-side.")
	@Test
	public void forcingTimeout_5min() throws Exception {
		final File trustStoreFile = CertificateHandlingAndTestServiceIT.getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl());
		final long sleepInMsec = 1000 * (300 + 1);
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackSleepUntilTimeout(
				handleCertificateExceptionCounter, sleepInMsec);
		cloudStoreRestClient.setSslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		cloudStoreRestClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(0);
		try {
			cloudStoreRestClient.execute(new TestCommand(false));
			Assert.fail("Should throw exception!");
		} catch (Exception x) {
			LOGGER.info(x.toString(), x);
			assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
			assertThat(x).isInstanceOf(ProcessingException.class);
			final SocketException socketException = ExceptionUtil.getCause(x, SocketException.class);
			assertThat(socketException).isNotNull();
		}
	}

}