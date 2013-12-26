package co.codewizards.cloudstore.shared.repo;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryRegistryTest {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryRegistryTest.class);
	private static final SecureRandom random = new SecureRandom();

	private static RepositoryManagerRegistry repositoryManagerRegistry = RepositoryManagerRegistry.getInstance();

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

	private File newTestRepositoryLocalRoot() {
		long timestamp = System.currentTimeMillis();
		int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36);
		return new File(getTestRepositoryBaseDir(), repoName);
	}

	private File getTestRepositoryBaseDir() {
		File dir = new File(new File("target"), "repo");
		dir.mkdirs();
		return dir;
	}

}
