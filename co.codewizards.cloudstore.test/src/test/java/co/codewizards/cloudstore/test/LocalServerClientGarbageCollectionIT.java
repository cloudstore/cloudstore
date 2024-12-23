package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.test.model.ExampleService;
import co.codewizards.cloudstore.test.model.ExampleServiceImpl;
import co.codewizards.cloudstore.test.model.ExampleServiceRegistry;
import co.codewizards.cloudstore.test.model.ExampleServiceRegistryImpl;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

//@RunWith(JMockit.class)
public class LocalServerClientGarbageCollectionIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(LocalServerClientGarbageCollectionIT.class);

	private final List<MockUp<?>> mockUps = new ArrayList<>();
//	private LocalServer localServer; // LocalServer is already started by main server.
	private LocalServerClient client;
	private List<ObjectRef> removedObjectRefs;

	private final List<Exception> errors = Collections.synchronizedList(new ArrayList<Exception>());

	@Override
	public void before() throws Exception {
		super.before();
		errors.clear();

		removedObjectRefs = new ArrayList<>();

		mockUps.add(new MockUp<ObjectManager>() {
			@Mock
			void remove(Invocation invocation, ObjectRef objectRef) {
				removedObjectRefs.add(objectRef);
				invocation.proceed(objectRef);
			}
		});

		final LocalServerRestClient localServerRestClient = new LocalServerRestClient() {
		};

		client = new LocalServerClient() {
			@Override
			public LocalServerRestClient _getLocalServerRestClient() {
				return localServerRestClient;
			}
		};
	}

	@SuppressWarnings("deprecation")
	@Override
	public void after() throws Exception {
		if (client != null) {
			client.close();
			client = null;
		}

//		for (MockUp<?> mockUp : mockUps)
//			mockUp.tearDown(); // seems not to be needed, anymore

		mockUps.clear();

		ObjectManager.clearObjectManagers();

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

			if (i % 100 == 0) {
				System.gc();
				Thread.sleep(5000L);
			}
		}
		System.gc();
		Thread.sleep(5000L);

		assertThat(removedObjectRefs).isEmpty();

		// Now, release the client-side references and expect garbage-collection to happen on the server-side.
		exampleServices.clear();
		for (int i = 0; i < 1000; ++i) {
			ExampleService exampleService = client.invokeConstructor(ExampleServiceImpl.class);
			exampleService.setStringValue(stringValue);
			exampleServices.add(exampleService);

			if (i % 100 == 0) {
				System.gc();
				Thread.sleep(5000L);
			}
		}
		System.gc();
		Thread.sleep(5000L);

		assertThat(removedObjectRefs.size()).isGreaterThan(200);
	}

	@Test
	public void testMultiThreadGarbageCollection() throws Exception {
		final ExampleServiceRegistry registry = client.invokeStatic(ExampleServiceRegistryImpl.class, "getInstance");
		final long startTimestamp = System.currentTimeMillis();

		final List<Thread> threads = new ArrayList<Thread>();

		for (int i = 0; i < 3; ++i) {
			Thread t = new Thread() {
				@Override
				public void run() {
					while (System.currentTimeMillis() - startTimestamp < 30000L && errors.isEmpty()) {
						try {
							ExampleService exampleService = registry.getExampleServiceOrCreate(random.nextInt(5));
							exampleService.setStringValue("bla");
							try { Thread.sleep(100); } catch (InterruptedException e) { }
							exampleService.setStringValue("blubb");
							exampleService = null;

							if (random.nextInt(100) < 30)
								System.gc();
						} catch (Exception x) {
							logger.error(x.toString(), x);
							errors.add(x);
						}
					}
				}
			};
			threads.add(t);
			t.start();
		}

		for (Thread thread : threads)
			thread.join();

		if (!errors.isEmpty())
			throw errors.get(0);
	}
}
