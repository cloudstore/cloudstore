package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.RemoteException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.core.util.TestException;
import co.codewizards.cloudstore.rest.client.ClientBuilderDefaultValuesDecorator;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.request.TestRequest;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextBuilder;
import co.codewizards.cloudstore.rest.server.service.TestService;

/**
 * Test asserting the certificate handling using the {@link DynamicX509TrustManagerCallback} works correctly.
 * It uses the simple {@link TestService} for this purpose and checks it completely: success + exception
 * (with {@link RemoteException} being thrown).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class CertificateHandlingAndTestServiceIT extends AbstractIT {
	private static final Logger logger = LoggerFactory.getLogger(CertificateHandlingAndTestServiceIT.class);
	private ClientBuilder clientBuilder;

	static File getRandomTrustStoreFile() throws IOException {
		final File dir = createFile(ConfigDir.getInstance().getFile(), "ssl.client");
		dir.mkdirs();
		final File trustStoreFile = createTempFile("truststore_", null, dir);
		trustStoreFile.delete(); // It must not exist (reading it fails).
		return trustStoreFile;
	}

	@Before
	public void setUp(){
		clientBuilder = new ClientBuilderDefaultValuesDecorator();
	}

	@Test
	public void testSuccessWithPermanentTrust() throws Exception {
		final File trustStoreFile = getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		final DynamicX509TrustManagerCallback callback1 = new DynamicX509TrustManagerCallbackTrustingPermanently(handleCertificateExceptionCounter);
		clientBuilder.sslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback1).build());
		CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl(), clientBuilder);
		cloudStoreRestClient.execute(new TestRequest(false));
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);

		cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl(), clientBuilder);
		final DynamicX509TrustManagerCallback callback2 = new DynamicX509TrustManagerCallback() {
			@Override
			public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
				Assert.fail("The certificate trust should have been stored permanently! But it was not.");
				return null;
			}
		};
		clientBuilder = new ClientBuilderDefaultValuesDecorator()
			.sslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback2).build());
		cloudStoreRestClient.execute(new TestRequest(false));
	}

	@Test
	public void testException() throws Exception {
		final File trustStoreFile = getRandomTrustStoreFile();
		final long[] handleCertificateExceptionCounter = new long[1];
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackTrustingPermanently(handleCertificateExceptionCounter);
		clientBuilder.sslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		final CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl(), clientBuilder);

		try {
			cloudStoreRestClient.execute(new TestRequest(true));
			Assert.fail("cloudStoreRESTClient.testException() should have thrown a RemoteException, but it did not!");
		} catch (final Exception x) {
			logger.info(x.toString(), x);
			assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
			assertThat(x).isInstanceOf(TestException.class);
			assertThat(x.getMessage()).isEqualTo("Test");

			assertThat(x.getStackTrace()).isNotNull();
			assertThat(x.getStackTrace().length).isGreaterThan(0);
			assertThat(x.getStackTrace()[0]).isNotNull();
			assertThat(x.getStackTrace()[0].getClassName()).isEqualTo("co.codewizards.cloudstore.rest.server.service.TestService");
			assertThat(x.getStackTrace()[0].getMethodName()).isEqualTo("test");
			assertThat(x.getStackTrace()[0].getLineNumber()).isEqualTo(37);

//			assertThat(x.getCause()).isNotNull();
//			assertThat(x.getCause()).isInstanceOf(RemoteException.class);
//
//			final RemoteException rx = (RemoteException) x.getCause();
//			assertThat(rx.getErrorClassName()).isEqualTo(TestException.class.getName());
//			assertThat(rx.getMessage()).isEqualTo("Test");
//			assertThat(rx.getStackTrace()).isNotNull();
//			assertThat(rx.getStackTrace().length).isGreaterThan(0);
//			assertThat(rx.getStackTrace()[0]).isNotNull();
//			assertThat(rx.getStackTrace()[0].getClassName()).isEqualTo("co.codewizards.cloudstore.rest.server.service.TestService");
		}
	}

	@Test
	public void nonTrustedCertificate() throws Exception {
		final File trustStoreFile = getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackTrustingPermanently(handleCertificateExceptionCounter) {
			@Override
			public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
				final CheckServerTrustedCertificateExceptionResult result = super.handleCheckServerTrustedCertificateException(context);
				result.setTrusted(false);
				return result;
			}
		};
		clientBuilder.sslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		final CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl(), clientBuilder);

		try {
			cloudStoreRestClient.execute(new TestRequest(false));
			Assert.fail("The certificate was trusted - but it should *not* be!!!");
		} catch (final Exception x) {
			final CertificateException certificateException = ExceptionUtil.getCause(x, CertificateException.class);
			final SSLException sslException = ExceptionUtil.getCause(x, SSLException.class);
			if (certificateException == null && sslException == null)
				throw x;
		}
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);
	}

	@Test
	public void testSuccessWithTemporaryTrust() throws Exception {
		final File trustStoreFile = getRandomTrustStoreFile();

		final long[] handleCertificateExceptionCounter = new long[1];
		final DynamicX509TrustManagerCallback callback = new DynamicX509TrustManagerCallbackTrustingTemporarily(handleCertificateExceptionCounter);
		clientBuilder.sslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());
		CloudStoreRestClient cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl(), clientBuilder);
		cloudStoreRestClient.execute(new TestRequest(false));
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(1);

		clientBuilder = new ClientBuilderDefaultValuesDecorator()
				.sslContext(SSLContextBuilder.create().trustStoreFile(trustStoreFile).callback(callback).build());

		cloudStoreRestClient = new CloudStoreRestClient(getSecureUrl(), clientBuilder);
		cloudStoreRestClient.execute(new TestRequest(false));
		assertThat(handleCertificateExceptionCounter[0]).isEqualTo(2);
	}
}
