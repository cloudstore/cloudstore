package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.client.util.FileLs;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class FileLsIT extends AbstractIT {

	@Override
	public void before() throws Exception {
		super.before();

		final LocalServerRestClient localServerRestClient = new LocalServerRestClient() {
		};

		final LocalServerClient client = new LocalServerClient() {
			@Override
			protected LocalServerRestClient _getLocalServerRestClient() {
				return localServerRestClient;
			}
		};

		new MockUp<LocalServerClient>() {
			@Mock
			LocalServerClient getInstance() {
				System.out.println("******************** YUHUUU *************");
				return client;
			}
		};
	}

	@Test
	public void testFileOutputStreamAndInputStream() throws Exception {
		File file = createTempFile("cloudstore-test-", ".tmp");
		assertInThisVm(file);

		IOutputStream out = FileLs.createOutputStream(file);
		out.write(new byte[] { 1, 2, 3 });
		out.close();
		assertInLocalServerVm(out);

		out = file.createOutputStream(true);
		out.write(new byte[] { 4, 5, 6, 7 });
		out.close();
		assertInThisVm(out);

		out = FileLs.createOutputStream(file, true);
		out.write(new byte[] { 8, 9 });
		out.close();
		assertInLocalServerVm(out);

		IInputStream in = file.createInputStream();
		assertThat(castStream(in)).hasSameContentAs(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
		assertInThisVm(in);

		in = FileLs.createInputStream(file);
		assertThat(castStream(in)).hasSameContentAs(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
		assertInLocalServerVm(in);
	}

	protected static void assertInThisVm(final Object object) {
		assertThat(object)
		.isNotNull()
		.isNotInstanceOf(RemoteObjectProxy.class);
	}

	protected static void assertInLocalServerVm(final Object object) {
		assertThat(object)
		.isNotNull()
		.isInstanceOf(RemoteObjectProxy.class);
	}
}
