package co.codewizards.cloudstore.shared.repo.local;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;

import co.codewizards.cloudstore.shared.persistence.RepoFile;
import co.codewizards.cloudstore.shared.persistence.RepoFileDAO;

public abstract class AbstractTest {

	protected static final Random random = new Random();
	protected static RepositoryManagerRegistry repositoryManagerRegistry = RepositoryManagerRegistry.getInstance();
	private Map<File, Set<File>> localRoot2FilesInRepo = new HashMap<File, Set<File>>();

	protected File newTestRepositoryLocalRoot() throws IOException {
		long timestamp = System.currentTimeMillis();
		int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36);
		File localRoot = new File(getTestRepositoryBaseDir(), repoName);
		addToFilesInRepo(localRoot, localRoot);
		return localRoot;
	}

	protected File getTestRepositoryBaseDir() {
		File dir = new File(new File("target"), "repo");
		dir.mkdirs();
		return dir;
	}

	@Before
	public void before() {
		localRoot2FilesInRepo.clear();
	}

	protected File createDirectory(File parent, String name) throws IOException {
		File dir = new File(parent, name);
		assertThat(dir).doesNotExist();
		dir.mkdir();
		assertThat(dir).isDirectory();
		addToFilesInRepo(dir);
		return dir;
	}

	protected void addToFilesInRepo(File file) throws IOException {
		file = file.getAbsoluteFile();
		File localRoot = getLocalRootOrFail(file);
		addToFilesInRepo(localRoot, file);
	}
	protected void addToFilesInRepo(File localRoot, File file) throws IOException {
		localRoot = localRoot.getCanonicalFile();
		file = file.getAbsoluteFile();
		Set<File> filesInRepo = localRoot2FilesInRepo.get(localRoot);
		if (filesInRepo == null) {
			filesInRepo = new HashSet<File>();
			localRoot2FilesInRepo.put(localRoot, filesInRepo);
		}
		filesInRepo.add(file);
	}

	protected File createFileWithRandomContent(File parent, String name) throws IOException {
		File file = new File(parent, name);
		assertThat(file).doesNotExist(); // prevent accidentally overwriting important data ;-)
		OutputStream out = new FileOutputStream(file);
		byte[] buf = new byte[1 + random.nextInt(10241)];
		int loops = 1 + random.nextInt(100);
		for (int i = 0; i < loops; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		out.close();
		assertThat(file).isFile();
		addToFilesInRepo(file);
		return file;
	}

	private File getLocalRootOrFail(File file) throws IOException {
		String filePath = file.getCanonicalPath();
		Collection<RepositoryManager> repositoryManagers = repositoryManagerRegistry.getRepositoryManagers();
		for (RepositoryManager repositoryManager : repositoryManagers) {
			String localRootPath = repositoryManager.getLocalRoot().getPath();
			if (filePath.startsWith(localRootPath)) {
				return repositoryManager.getLocalRoot();
			}
		}
		throw new IllegalArgumentException("file is not contained in any open repository: " + filePath);
	}

	protected void assertThatFilesInRepoAreCorrect(File localRoot) {
		RepositoryManager repositoryManager = RepositoryManagerRegistry.getInstance().getRepositoryManager(localRoot);
		localRoot = repositoryManager.getLocalRoot(); // get canonical File
		RepositoryTransaction transaction = repositoryManager.createTransaction();
		try {
			RepoFileDAO repoFileDAO = new RepoFileDAO().persistenceManager(transaction.getPersistenceManager());
			Set<File> filesInRepo = localRoot2FilesInRepo.get(localRoot);
			assertThat(filesInRepo).isNotNull();

			for (File file : filesInRepo) {
				RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
				if (repoFile == null) {
					Assert.fail("Corresponding RepoFile missing in repository for file: " + file);
				}
			}

			filesInRepo = new HashSet<File>(filesInRepo);
			Collection<RepoFile> repoFiles = repoFileDAO.getObjects();
			for (RepoFile repoFile : repoFiles) {
				File file = repoFile.getFile(localRoot);
				if (!filesInRepo.remove(file)) {
					Assert.fail("Corresponding file in file-system missing for RepoFile: " + repoFile + " " + file);
				}
			}
		} finally {
			transaction.rollbackIfActive();
		}
	}

}
