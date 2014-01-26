package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.core.util.TestException;
import co.codewizards.cloudstore.rest.client.CloudStoreRESTClient;
import co.codewizards.cloudstore.rest.client.RemoteException;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextUtil;
import co.codewizards.cloudstore.rest.server.service.TestService;

/**
 * Test asserting the certificate handling using the {@link DynamicX509TrustManagerCallback} works correctly.
 * It uses the simple {@link TestService} for this purpose and checks it completely: success + exception
 * (with {@link RemoteException} being thrown).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class CertificateHandlingAndTestServiceIT extends AbstractIT {
	private static final Logger logger = LoggerFactory.getLogger(CertificateHandlingAndTestServiceIT.class);

	private File getRandomTrustStoreFile() throws IOException {
		File dir = new File(ConfigDir.getInstance().getFile(), "ssl.client");
		dir.mkdirs();
		File trustStoreFile = File.createTempFile("truststore_", null, dir);
		trustStoreFile.delete(); // It must not exist (reading it fails).
		return trustStoreFile;
	}

	@Test
	public void testSuccessWithPermanentTrust() throws Exception {
		File trustStoreFile = getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRESTClient cloudStoreRESTClient = new CloudStoreRESTClient("https://localhost:" + getSecurePort());
		DynamicX509TrustManagerCallback callback1 = new DynamicX509TrustManagerCallbackTrustingPermanently(handleCertificateExceptionCounter);
		cloudStoreRESTClient.setSslContext(SSLContextUtil.getSSLContext(trustStoreFile, callback1));
		cloudStoreRESTClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		cloudStoreRESTClient.testSuccess();
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);

		cloudStoreRESTClient = new CloudStoreRESTClient("https://localhost:" + getSecurePort());
		DynamicX509TrustManagerCallback callback2 = new DynamicX509TrustManagerCallback() {
			@Override
			public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
				Assert.fail("The certificate trust should have been stored permanently! But it was not.");
				return null;
			}
		};
		cloudStoreRESTClient.setSslContext(SSLContextUtil.getSSLContext(trustStoreFile, callback2));
		cloudStoreRESTClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		cloudStoreRESTClient.testSuccess();
	}

	@Test
	public void testException() throws Exception {
		File trustStoreFile = getRandomTrustStoreFile();
		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRESTClient cloudStoreRESTClient = new CloudStoreRESTClient("https://localhost:" + getSecurePort());
		DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackTrustingPermanently(handleCertificateExceptionCounter);
		cloudStoreRESTClient.setSslContext(SSLContextUtil.getSSLContext(trustStoreFile, callback));
		cloudStoreRESTClient.setHostnameVerifier(new HostnameVerifierAllowingAll());

		try {
			cloudStoreRESTClient.testException();
			Assert.fail("cloudStoreRESTClient.testException() should have thrown a RemoteException, but it did not!");
		} catch (Exception x) {
			logger.info(x.toString(), x);
			assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
			assertThat(x).isInstanceOf(TestException.class);
			assertThat(x.getMessage()).isEqualTo("Test");

			assertThat(x.getCause()).isNotNull();
			assertThat(x.getCause()).isInstanceOf(RemoteException.class);

			RemoteException rx = (RemoteException) x.getCause();
			assertThat(rx.getErrorClassName()).isEqualTo(TestException.class.getName());
			assertThat(rx.getMessage()).isEqualTo("Test");
			assertThat(rx.getStackTrace()).isNotNull();
			assertThat(rx.getStackTrace().length).isGreaterThan(0);
			assertThat(rx.getStackTrace()[0]).isNotNull();
			assertThat(rx.getStackTrace()[0].getClassName()).isEqualTo("co.codewizards.cloudstore.rest.server.service.TestService");
		}
	}

	@Test
	public void nonTrustedCertificate() throws Exception {
		File trustStoreFile = getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRESTClient cloudStoreRESTClient = new CloudStoreRESTClient("https://localhost:" + getSecurePort());
		DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackTrustingPermanently(handleCertificateExceptionCounter) {
			@Override
			public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
				CheckServerTrustedCertificateExceptionResult result = super.handleCheckServerTrustedCertificateException(context);
				result.setTrusted(false);
				return result;
			}
		};
		cloudStoreRESTClient.setSslContext(SSLContextUtil.getSSLContext(trustStoreFile, callback));
		cloudStoreRESTClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		try {
			cloudStoreRESTClient.testSuccess();
			Assert.fail("The certificate was trusted - but it should *not* be!!!");
		} catch (Exception x) {
			CertificateException certificateException = ExceptionUtil.getCause(x, CertificateException.class);
			SSLException sslException = ExceptionUtil.getCause(x, SSLException.class);
			if (certificateException == null && sslException == null)
				throw x;
		}
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
	}

	@Test
	public void testSuccessWithTemporaryTrust() throws Exception {
		File trustStoreFile = getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		CloudStoreRESTClient cloudStoreRESTClient = new CloudStoreRESTClient("https://localhost:" + getSecurePort());
		DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackTrustingTemporarily(handleCertificateExceptionCounter);
		cloudStoreRESTClient.setSslContext(SSLContextUtil.getSSLContext(trustStoreFile, callback));
		cloudStoreRESTClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		cloudStoreRESTClient.testSuccess();
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);

		cloudStoreRESTClient = new CloudStoreRESTClient("https://localhost:" + getSecurePort());
		cloudStoreRESTClient.setSslContext(SSLContextUtil.getSSLContext(trustStoreFile, callback));
		cloudStoreRESTClient.setHostnameVerifier(new HostnameVerifierAllowingAll());
		cloudStoreRESTClient.testSuccess();
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(2);
	}
}
