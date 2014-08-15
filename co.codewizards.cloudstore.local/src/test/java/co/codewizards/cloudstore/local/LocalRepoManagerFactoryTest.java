package co.codewizards.cloudstore.local;

import static org.assertj.core.api.Assertions.*;

import co.codewizards.cloudstore.core.oio.file.File;
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
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(newFile(newFile(localRoot, "bla"), ".."));
		assertThat(localRepoManager2).isNotNull();
		assertThat(localRepoManager2).isNotSameAs(localRepoManager);

		assertThat(Proxy.isProxyClass(localRepoManager.getClass())).isTrue();
		assertThat(Proxy.isProxyClass(localRepoManager2.getClass())).isTrue();

		LocalRepoManagerInvocationHandler invocationHandler = (LocalRepoManagerInvocationHandler) Proxy.getInvocationHandler(localRepoManager);
		LocalRepoManagerInvocationHandler invocationHandler2 = (LocalRepoManagerInvocationHandler) Proxy.getInvocationHandler(localRepoManager2);
		assertThat(invocationHandler).isNotSameAs(invocationHandler2);
		assertThat(invocationHandler.localRepoManagerImpl).isSameAs(invocationHandler2.localRepoManagerImpl);

		localRepoManager.close();
		localRepoManager2.close();
	}

	@Test
	public void getLocalRepoManagerForExistingRepository() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		localRepoManager.close();

		LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(newFile(newFile(localRoot, "bla"), ".."));
		assertThat(localRepoManager2).isNotNull();
		assertThat(localRepoManager2).isNotSameAs(localRepoManager);

		localRepoManager2.close();
	}

	@Test(expected=FileNotFoundException.class)
	public void getLocalRepoManagerForNonExistingDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
	}

	@Test(expected=FileNoDirectoryException.class)
	public void getLocalRepoManagerForExistingNonDirectoryFile() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		File localRootParent = localRoot.getParentFile();

		localRootParent.mkdirs();
		assertThat(localRootParent).isDirectory();

		localRoot.createNewFile();
		assertThat(localRoot).isFile();

		localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
	}

	@Test(expected=FileNoRepositoryException.class)
	public void getLocalRepoManagerForExistingNonRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		localRepoManager.close();
	}

	@Test(expected=FileNotFoundException.class)
	public void createLocalRepoManagerForNonExistingDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
	}

	@Test(expected=FileAlreadyRepositoryException.class)
	public void createLocalRepoManagerForRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		localRepoManager.close();

		localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
	}

	/**
	 * Expects the same behaviour as {@link #createLocalRepoManagerForRepoDirectory()}
	 */
	@Test(expected=FileAlreadyRepositoryException.class)
	public void createLocalRepoManagerForRepoDirectoryWithClose() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		localRepoManager.close();
		localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
	}

	@Test(expected=FileAlreadyRepositoryException.class)
	public void createLocalRepoManagerForNonRepoDirInsideRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);

		assertThat(localRepoManager.getLocalRoot()).isEqualTo(localRoot.getCanonicalFile());

		File sub1Dir = newFile(localRepoManager.getLocalRoot(), "sub1");
		File sub1SubAaaDir = newFile(sub1Dir, "aaa");

		sub1SubAaaDir.mkdirs();
		assertThat(sub1SubAaaDir).isDirectory();

		localRepoManager.close();

		localRepoManagerFactory.createLocalRepoManagerForNewRepository(sub1SubAaaDir);
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

}
