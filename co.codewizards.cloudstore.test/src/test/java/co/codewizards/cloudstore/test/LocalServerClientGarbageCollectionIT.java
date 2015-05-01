package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.junit.Test;

import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.server.LocalServer;
import co.codewizards.cloudstore.test.model.ExampleService;
import co.codewizards.cloudstore.test.model.ExampleServiceImpl;

public class LocalServerClientGarbageCollectionIT extends AbstractIT {

	private LocalServer localServer;
	private LocalServerClient client;
	private List<ObjectRef> removedObjectRefs = new ArrayList<>();

	@Override
	public void before() {
		super.before();

		new MockUp<ObjectManager>() {
			@Mock
			void remove(Invocation invocation, ObjectRef objectRef) {
				removedObjectRefs.add(objectRef);
				invocation.proceed(objectRef);
			}
		};

		localServer = new LocalServer();
		localServer.start();

		final LocalServerRestClient localServerRestClient = new LocalServerRestClient() {
		};

		client = new LocalServerClient() {
			@Override
			public LocalServerRestClient getLocalServerRestClient() {
				return localServerRestClient;
			}
		};
	}

	@Override
	public void after() {
		client.close();
		localServer.stop();

		super.after();
	}

	@Test
	public void testSimpleGarbageCollection() throws Exception {
		List<ExampleService> exampleServices = new ArrayList<>();
		String stringValue = "testGarbageCollection";

		// Test that GC was *not* (yet) done on server-side, because we still hold references on the client-side.
		removedObjectRefs.clear();

		for (int i = 0; i < 1000; ++i) {
			ExampleService exampleService = client.invokeConstructor(ExampleServiceImpl.class);
			exampleService.setStringValue(stringValue);
			exampleServices.add(exampleService);

			if (i % 100 == 0)
				System.gc();
		}
		System.gc();

		assertThat(removedObjectRefs).isEmpty();

		// Now, release the client-side references and expect garbage-collection to happen on the server-side.
		exampleServices.clear();
		for (int i = 0; i < 1000; ++i) {
			ExampleService exampleService = client.invokeConstructor(ExampleServiceImpl.class);
			exampleService.setStringValue(stringValue);
			exampleServices.add(exampleService);

			if (i % 100 == 0)
				System.gc();
		}
		System.gc();

		assertThat(removedObjectRefs.size()).isGreaterThan(200);
	}
}
