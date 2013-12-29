package co.codewizards.cloudstore.shared.repo.local;

import static org.assertj.core.api.Assertions.*;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.AbstractTest;
import co.codewizards.cloudstore.shared.repo.local.FileAlreadyRepositoryException;
import co.codewizards.cloudstore.shared.repo.local.FileNoDirectoryException;
import co.codewizards.cloudstore.shared.repo.local.FileNoRepositoryException;
import co.codewizards.cloudstore.shared.repo.local.FileNotFoundException;
import co.codewizards.cloudstore.shared.repo.local.RepositoryManager;

public class RepositoryManagerRegistryTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryManagerRegistryTest.class);
	@Test
	public void createRepositoryManagerForExistingNonRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();

		RepositoryManager repositoryManager2 = repositoryManagerRegistry.getRepositoryManager(new File(new File(localRoot, "bla"), ".."));
		assertThat(repositoryManager2).isNotNull();
		assertThat(repositoryManager2).isSameAs(repositoryManager);
	}

	@Test
	public void getRepositoryManagerForRepository() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();

		repositoryManager.close();

		RepositoryManager repositoryManager2 = repositoryManagerRegistry.getRepositoryManager(new File(new File(localRoot, "bla"), ".."));
		assertThat(repositoryManager2).isNotNull();
		assertThat(repositoryManager2).isNotSameAs(repositoryManager);
	}

	@Test(expected=FileNotFoundException.class)
	public void getRepositoryManagerForNonExistingDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		repositoryManagerRegistry.getRepositoryManager(localRoot);
	}

	@Test(expected=FileNoDirectoryException.class)
	public void getRepositoryManagerForExistingNonDirectoryFile() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		File localRootParent = localRoot.getParentFile();

		localRootParent.mkdirs();
		assertThat(localRootParent).isDirectory();

		localRoot.createNewFile();
		assertThat(localRoot).isFile();

		repositoryManagerRegistry.getRepositoryManager(localRoot);
	}

	@Test(expected=FileNoRepositoryException.class)
	public void getRepositoryManagerForExistingNonRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		repositoryManagerRegistry.getRepositoryManager(localRoot);
	}

	@Test(expected=FileNotFoundException.class)
	public void createRepositoryManagerForNonExistingDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		repositoryManagerRegistry.createRepositoryManager(localRoot);
	}

	@Test(expected=FileAlreadyRepositoryException.class)
	public void createRepositoryManagerForRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();
		repositoryManagerRegistry.createRepositoryManager(localRoot);
	}

	/**
	 * Expects the same behaviour as {@link #createRepositoryManagerForRepoDirectory()}
	 */
	@Test(expected=FileAlreadyRepositoryException.class)
	public void createRepositoryManagerForRepoDirectoryWithClose() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();
		repositoryManager.close();
		repositoryManagerRegistry.createRepositoryManager(localRoot);
	}

	@Test(expected=FileAlreadyRepositoryException.class)
	public void createRepositoryManagerForNonRepoDirInsideRepoDirectory() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);

		assertThat(repositoryManager.getLocalRoot()).isEqualTo(localRoot.getCanonicalFile());

		File sub1Dir = new File(repositoryManager.getLocalRoot(), "sub1");
		File sub1SubAaaDir = new File(sub1Dir, "aaa");

		sub1SubAaaDir.mkdirs();
		assertThat(sub1SubAaaDir).isDirectory();

		repositoryManagerRegistry.createRepositoryManager(sub1SubAaaDir);
	}

}
