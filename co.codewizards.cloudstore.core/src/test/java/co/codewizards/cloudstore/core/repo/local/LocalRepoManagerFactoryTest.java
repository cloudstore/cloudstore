package co.codewizards.cloudstore.core.repo.local;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.AbstractTest;

public class LocalRepoManagerFactoryTest extends AbstractTest {

	private static long closeDeferredMillis;

	@BeforeClass
	public static void beforeClass() {
		closeDeferredMillis = LocalRepoManagerImpl.closeDeferredMillis;
		LocalRepoManagerImpl.closeDeferredMillis = 0;
	}

	@AfterClass
	public static void afterClass() {
		LocalRepoManagerImpl.closeDeferredMillis = closeDeferredMillis;
	}

	@Test
	public void createLocalRepoManagerForExistingNonRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(new File(new File(localRoot, "bla"), ".."));
		assertThat(localRepoManager2).isNotNull();
		assertThat(localRepoManager2).isNotSameAs(localRepoManager);

		assertThat(Proxy.isProxyClass(localRepoManager.getClass())).isTrue();
		assertThat(Proxy.isProxyClass(localRepoManager2.getClass())).isTrue();

		LocalRepoManagerInvocationHandler invocationHandler = (LocalRepoManagerInvocationHandler) Proxy.getInvocationHandler(localRepoManager);
		LocalRepoManagerInvocationHandler invocationHandler2 = (LocalRepoManagerInvocationHandler) Proxy.getInvocationHandler(localRepoManager2);
		assertThat(invocationHandler).isNotSameAs(invocationHandler2);
		assertThat(invocationHandler.localRepoManagerImpl).isSameAs(invocationHandler2.localRepoManagerImpl);
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

		LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(new File(new File(localRoot, "bla"), ".."));
		assertThat(localRepoManager2).isNotNull();
		assertThat(localRepoManager2).isNotSameAs(localRepoManager);
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
		localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
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

		File sub1Dir = new File(localRepoManager.getLocalRoot(), "sub1");
		File sub1SubAaaDir = new File(sub1Dir, "aaa");

		sub1SubAaaDir.mkdirs();
		assertThat(sub1SubAaaDir).isDirectory();

		localRepoManagerFactory.createLocalRepoManagerForNewRepository(sub1SubAaaDir);
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

}
