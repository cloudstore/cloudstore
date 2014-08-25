package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Proxy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.repo.local.FileAlreadyRepositoryException;
import co.codewizards.cloudstore.core.repo.local.FileNoDirectoryException;
import co.codewizards.cloudstore.core.repo.local.FileNoRepositoryException;
import co.codewizards.cloudstore.core.repo.local.FileNotFoundException;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.oio.api.File;

public class LocalRepoManagerFactoryTest extends AbstractTest {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, "0");
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS);
	}

	@Test
	public void createLocalRepoManagerForExistingNonRepoDirectory() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(createFile(createFile(localRoot, "bla"), ".."));
		assertThat(localRepoManager2).isNotNull();
		assertThat(localRepoManager2).isNotSameAs(localRepoManager);

		assertThat(Proxy.isProxyClass(localRepoManager.getClass())).isTrue();
		assertThat(Proxy.isProxyClass(localRepoManager2.getClass())).isTrue();

		final LocalRepoManagerInvocationHandler invocationHandler = (LocalRepoManagerInvocationHandler) Proxy.getInvocationHandler(localRepoManager);
		final LocalRepoManagerInvocationHandler invocationHandler2 = (LocalRepoManagerInvocationHandler) Proxy.getInvocationHandler(localRepoManager2);
		assertThat(invocationHandler).isNotSameAs(invocationHandler2);
		assertThat(invocationHandler.localRepoManagerImpl).isSameAs(invocationHandler2.localRepoManagerImpl);

		localRepoManager.close();
		localRepoManager2.close();
	}

	@Test
	public void getLocalRepoManagerForExistingRepository() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		localRepoManager.close();

		final LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(createFile(createFile(localRoot, "bla"), ".."));
		assertThat(localRepoManager2).isNotNull();
		assertThat(localRepoManager2).isNotSameAs(localRepoManager);

		localRepoManager2.close();
	}

	@Test(expected=FileNotFoundException.class)
	public void getLocalRepoManagerForNonExistingDirectory() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
	}

	@Test(expected=FileNoDirectoryException.class)
	public void getLocalRepoManagerForExistingNonDirectoryFile() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		final File localRootParent = localRoot.getParentFile();

		localRootParent.mkdirs();
		assertThat(localRootParent.isDirectory()).isTrue();

		localRoot.createNewFile();
		assertThat(localRoot.isFile()).isTrue();

		localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
	}

	@Test(expected=FileNoRepositoryException.class)
	public void getLocalRepoManagerForExistingNonRepoDirectory() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		localRepoManager.close();
	}

	@Test(expected=FileNotFoundException.class)
	public void createLocalRepoManagerForNonExistingDirectory() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
	}

	@Test(expected=FileAlreadyRepositoryException.class)
	public void createLocalRepoManagerForRepoDirectory() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		localRepoManager.close();

		localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
	}

	/**
	 * Expects the same behaviour as {@link #createLocalRepoManagerForRepoDirectory()}
	 */
	@Test(expected=FileAlreadyRepositoryException.class)
	public void createLocalRepoManagerForRepoDirectoryWithClose() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		localRepoManager.close();
		localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
	}

	@Test(expected=FileAlreadyRepositoryException.class)
	public void createLocalRepoManagerForNonRepoDirInsideRepoDirectory() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);

		assertThat(localRepoManager.getLocalRoot()).isEqualTo(localRoot.getCanonicalFile());

		final File sub1Dir = createFile(localRepoManager.getLocalRoot(), "sub1");
		final File sub1SubAaaDir = createFile(sub1Dir, "aaa");

		sub1SubAaaDir.mkdirs();
		assertThat(sub1SubAaaDir.isDirectory()).isTrue();

		localRepoManager.close();

		localRepoManagerFactory.createLocalRepoManagerForNewRepository(sub1SubAaaDir);
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

}
