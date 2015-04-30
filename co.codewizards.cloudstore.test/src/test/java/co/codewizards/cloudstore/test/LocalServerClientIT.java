package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.OioFileFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.remoteobject.RemoteObject;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.server.LocalServer;

public class LocalServerClientIT extends AbstractIT {

	private static LocalServer localServer;
	private LocalServerClient client;

	@BeforeClass
	public static void beforeLocalServerClientIT() {
		localServer = new LocalServer();
		localServer.start();
	}

	@Override
	public void before() {
		super.before();
		client = new LocalServerClient() {
			@Override
			protected LocalServerRestClient getLocalServerRestClient() {
				return new LocalServerRestClient() {
				};
			}
		};
	}

	@AfterClass
	public static void afterLocalServerClientIT() {
		localServer.stop();
	}

	@Test
	public void invokeSimpleStaticMethod() throws Exception {
		Long remoteMillis = client.invokeStatic(System.class, "currentTimeMillis");
		long localMillis = System.currentTimeMillis();
		assertThat(remoteMillis).isNotNull();
		assertThat(localMillis - remoteMillis).isBetween(0L, 10000L);
	}

	@Test
	public void invokeConstructorAndSomeMethodsViaApi() throws Exception {
		Object localRepository = client.invokeConstructor(LocalRepository.class);
		assertThat(localRepository).isInstanceOf(RemoteObject.class);

		byte[] privateKeyOutput = client.invoke(localRepository, "getPrivateKey");
		assertThat(privateKeyOutput).isNull();

		byte[] privateKeyInput = new Uid().toBytes();
		client.invoke(localRepository, "setPrivateKey", privateKeyInput);

		privateKeyOutput = client.invoke(localRepository, "getPrivateKey");
		assertThat(privateKeyOutput).isEqualTo(privateKeyInput);


		Object rootDirectoryOutput = client.invoke(localRepository, "getRoot");

		Object rootDirectoryInput = client.invokeConstructor(Directory.class);
		assertThat(rootDirectoryInput).isInstanceOf(RemoteObject.class);

		client.invoke(localRepository, "setRoot", rootDirectoryInput);
		rootDirectoryOutput = client.invoke(localRepository, "getRoot");
		assertThat(rootDirectoryOutput).isInstanceOf(RemoteObject.class);
		assertThat(rootDirectoryOutput).isSameAs(rootDirectoryInput);
	}

	@Test
	public void invokeStaticMethodAndSomeMethodsViaProxy() throws Exception {
		File localRootInput = newTestRepositoryLocalRoot("");
		assertThat(localRootInput).isNotInstanceOf(RemoteObject.class);

		File localRoot = client.invokeStatic(OioFileFactory.class, "createFile", localRootInput.getAbsolutePath());
		assertThat(localRoot).isInstanceOf(RemoteObject.class);
		assertThat(localRoot.mkdir()).isTrue();

		LocalRepoManagerFactory localRepoManagerFactory = client.invokeStatic(LocalRepoManagerFactory.Helper.class, "getInstance");
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isInstanceOf(RemoteObject.class);
		localRepoManager.close();
	}
}
