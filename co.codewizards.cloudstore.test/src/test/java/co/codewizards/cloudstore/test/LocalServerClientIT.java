package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.OioFileFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.util.ReflectionUtil;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.test.model.ExampleService;
import co.codewizards.cloudstore.test.model.ExampleServiceImpl;

public class LocalServerClientIT extends AbstractIT {

//	private static LocalServer localServer; // LocalServer is already started by main server.
	private static LocalServerClient client;

	@BeforeClass
	public static void beforeLocalServerClientIT() {
//		localServer = new LocalServer();
//		localServer.start();

		final LocalServerRestClient localServerRestClient = new LocalServerRestClient() {
		};

		client = new LocalServerClient() {
			@Override
			protected LocalServerRestClient _getLocalServerRestClient() {
				return localServerRestClient;
			}
		};
	}

	@AfterClass
	public static void afterLocalServerClientIT() {
		client.close();
//		localServer.stop();
	}

	@Test
	public void invokeSimpleStaticMethod() throws Exception {
		Long remoteMillis = client.invokeStatic(System.class, "currentTimeMillis");
		long localMillis = System.currentTimeMillis();
		assertThat(remoteMillis).isNotNull();
		assertThat(localMillis - remoteMillis).isBetween(0L, 10000L);
	}

	@Test
	public void invokeDeniedMethods() throws Exception {
		try {
			client.invokeStatic(System.class, "setProperty", "key1", "value1");
			fail("Succeeded invoking a method that should be denied!");
		} catch (SecurityException x) {
			doNothing();
		}

		// This is denied by the explicit blacklist in AllowCloudStoreInvocationFilter (by default all classes in *our* package are allowed)
		try {
			client.invokeStatic(ReflectionUtil.class, "invokeStatic", System.class, "getProperty", new Object[] { "user.home" });
			fail("Succeeded invoking a method that should be denied!");
		} catch (SecurityException x) {
			doNothing();
		}
	}

	@Test
	public void invokeConstructorAndSomeMethodsViaApi() throws Exception {
		Object localRepository = client.invokeConstructor(LocalRepository.class);
		assertThat(localRepository).isInstanceOf(RemoteObjectProxy.class);

		byte[] privateKeyOutput = client.invoke(localRepository, "getPrivateKey");
		assertThat(privateKeyOutput).isNull();

		byte[] privateKeyInput = new Uid().toBytes();
		client.invoke(localRepository, "setPrivateKey", privateKeyInput);

		privateKeyOutput = client.invoke(localRepository, "getPrivateKey");
		assertThat(privateKeyOutput).isEqualTo(privateKeyInput);


		Object rootDirectoryOutput = client.invoke(localRepository, "getRoot");

		Object rootDirectoryInput = client.invokeConstructor(Directory.class);
		assertThat(rootDirectoryInput).isInstanceOf(RemoteObjectProxy.class);

		client.invoke(localRepository, "setRoot", rootDirectoryInput);
		rootDirectoryOutput = client.invoke(localRepository, "getRoot");
		assertThat(rootDirectoryOutput).isInstanceOf(RemoteObjectProxy.class);
		assertThat(rootDirectoryOutput).isSameAs(rootDirectoryInput);
	}

	@Test
	public void invokeStaticMethodAndSomeMethodsViaProxy() throws Exception {
		File localRootInput = newTestRepositoryLocalRoot("");
		assertThat(localRootInput).isNotInstanceOf(RemoteObjectProxy.class);

		File localRoot = client.invokeStatic(OioFileFactory.class, "createFile", localRootInput.getAbsolutePath());
		assertThat(localRoot).isInstanceOf(File.class);
		assertThat(localRoot.mkdir()).isTrue();

		LocalRepoManagerFactory localRepoManagerFactory = client.invokeStatic(LocalRepoManagerFactory.Helper.class, "getInstance");
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isInstanceOf(RemoteObjectProxy.class);
		Map<UUID, URL> map = localRepoManager.getRemoteRepositoryId2RemoteRootMap();
		assertThat(map).isInstanceOf(RemoteObjectProxy.class);
		localRepoManager.close();
	}

	private static class PropertyChangeListenerInvocation {
		public final PropertyChangeListener listener;
		public final PropertyChangeEvent event;

		public PropertyChangeListenerInvocation(final PropertyChangeListener listener, final PropertyChangeEvent event) {
			this.listener = listener;
			this.event = event;
		}
	}

	@Test
	public void testPropertyChangeListener() throws Exception {
		ExampleService exampleService = client.invokeConstructor(ExampleServiceImpl.class);

		final List<PropertyChangeListenerInvocation> propertyChangeListenerInvocations = new ArrayList<>();

		final boolean[] slept = new boolean[] { false };
		final boolean[] sleepEnabled = new boolean[] { true };
		PropertyChangeListener globalPropertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (sleepEnabled[0]) {
					try { Thread.sleep(300_000); } catch (InterruptedException e) { }
					slept[0] = true;
				}
				propertyChangeListenerInvocations.add(new PropertyChangeListenerInvocation(this, event));
			}
		};

		PropertyChangeListener stringValuePropertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
//				if (sleepEnabled[0])
//					try { Thread.sleep(300_000); } catch (InterruptedException e) { }

				propertyChangeListenerInvocations.add(new PropertyChangeListenerInvocation(this, event));
			}
		};

		exampleService.addPropertyChangeListener(globalPropertyChangeListener);
		exampleService.addPropertyChangeListener(ExampleService.PropertyEnum.stringValue, stringValuePropertyChangeListener);

		assertThat(slept[0]).isFalse();
		propertyChangeListenerInvocations.clear();
		exampleService.setStringValue("aaa");
		assertThat(propertyChangeListenerInvocations).hasSize(2);

		assertThat(slept[0]).isTrue();

		sleepEnabled[0] = false;

		exampleService.setStringValue("bbb");
		assertThat(propertyChangeListenerInvocations).hasSize(4);

		exampleService.setLongValue(123);
		assertThat(propertyChangeListenerInvocations).hasSize(5);

		assertThat(propertyChangeListenerInvocations.get(0).listener).isSameAs(globalPropertyChangeListener);
		assertThat(propertyChangeListenerInvocations.get(0).event.getOldValue()).isEqualTo(null);
		assertThat(propertyChangeListenerInvocations.get(0).event.getNewValue()).isEqualTo("aaa");

		assertThat(propertyChangeListenerInvocations.get(1).listener).isSameAs(stringValuePropertyChangeListener);
		assertThat(propertyChangeListenerInvocations.get(1).event.getOldValue()).isEqualTo(null);
		assertThat(propertyChangeListenerInvocations.get(1).event.getNewValue()).isEqualTo("aaa");

		assertThat(propertyChangeListenerInvocations.get(2).listener).isSameAs(globalPropertyChangeListener);
		assertThat(propertyChangeListenerInvocations.get(2).event.getOldValue()).isEqualTo("aaa");
		assertThat(propertyChangeListenerInvocations.get(2).event.getNewValue()).isEqualTo("bbb");

		assertThat(propertyChangeListenerInvocations.get(3).listener).isSameAs(stringValuePropertyChangeListener);
		assertThat(propertyChangeListenerInvocations.get(3).event.getOldValue()).isEqualTo("aaa");
		assertThat(propertyChangeListenerInvocations.get(3).event.getNewValue()).isEqualTo("bbb");

		assertThat(propertyChangeListenerInvocations.get(4).listener).isSameAs(globalPropertyChangeListener);
		assertThat(propertyChangeListenerInvocations.get(4).event.getOldValue()).isEqualTo(0L);
		assertThat(propertyChangeListenerInvocations.get(4).event.getNewValue()).isEqualTo(123L);
	}
}
