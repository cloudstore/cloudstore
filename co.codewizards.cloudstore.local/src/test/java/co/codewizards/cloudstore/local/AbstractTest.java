package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;
import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.DevMode;
import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.IoFile;
import co.codewizards.cloudstore.core.oio.nio.NioFileFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.db.DatabaseAdapterFactoryRegistry;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.persistence.Symlink;

public abstract class AbstractTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);
	protected static String jvmInstanceDir;

	static {
		DevMode.enableDevMode();
		final Uid jvmInstanceId = new Uid(); // for parallel test execution ;-)
		jvmInstanceDir = "target/jvm/" + jvmInstanceId;
		final String configDirString = jvmInstanceDir + "/.cloudstore";
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, configDirString);
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_KEY_SIZE, "1024");

		createFile(configDirString).mkdirs();
	}

	protected static final Random random = new Random();
	protected static LocalRepoManagerFactory localRepoManagerFactory = LocalRepoManagerFactory.Helper.getInstance();
	private final Map<File, Set<File>> localRoot2FilesInRepo = new HashMap<File, Set<File>>();

	protected File newTestRepositoryLocalRoot(final String suffix) throws IOException {
		assertThat(suffix).isNotNull();
		final long timestamp = System.currentTimeMillis();
		final int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		final String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36) + (suffix.isEmpty() ? "" : "-") + suffix;
		final File localRoot = createFile(getTestRepositoryBaseDir(), repoName);
		addToFilesInRepo(localRoot, localRoot);
		return localRoot;
	}

	protected File getTestRepositoryBaseDir() {
		final File dir = createFile(createFile("target"), "repo");
		dir.mkdirs();
		return dir;
	}

	@Before
	public void before() {
		logger.debug(">>> === >>> === >>> {}.before() >>> === >>> === >>>", this.getClass().getName());
		localRoot2FilesInRepo.clear();
	}

	@After
	public void after() {
		logger.debug("<<< === <<< === <<< {}.after() <<< === <<< === <<<", this.getClass().getName());
	}

	protected File createDirectory(final File parent, final String name) throws IOException {
		final File dir = createFile(parent, name);
		return createDirectory(dir);
	}
	protected File createDirectory(final File dir) throws IOException {
		assertThat(dir.exists()).isFalse();
		dir.mkdir();
		assertThat(dir.isDirectory()).isTrue();
		addToFilesInRepo(dir);
		return dir;
	}

	protected void addToFilesInRepo(File file) throws IOException {
		file = file.getAbsoluteFile();
		final File localRoot = getLocalRootOrFail(file);
		addToFilesInRepo(localRoot, file);
	}
	protected void addToFilesInRepo(File localRoot, File file) throws IOException {
		localRoot = localRoot.getAbsoluteFile();
		file = file.getAbsoluteFile();
		Set<File> filesInRepo = localRoot2FilesInRepo.get(localRoot);
		if (filesInRepo == null) {
			filesInRepo = new HashSet<File>();
			localRoot2FilesInRepo.put(localRoot, filesInRepo);
		}
		filesInRepo.add(file);
	}

	protected File createFileWithRandomContent(final File parent, final String name) throws IOException {
		return createFileWithRandomContent(parent, name, 0);
	}

	protected File createFileWithRandomContent(final File parent, final String name, final long minLength) throws IOException {
		final File file = createFile(parent, name);
		return createFileWithRandomContent(file, minLength);
	}

	protected File createFileWithRandomContent(final File file) throws IOException {
		return createFileWithRandomContent(file, 0);
	}

	protected File createFileWithRandomContent(final File file, final long minLength) throws IOException {
		assertThat(file.exists()).isFalse(); // prevent accidentally overwriting important data ;-)
		final OutputStream out = castStream(file.createOutputStream());
		final byte[] buf = new byte[1 + random.nextInt(10241)];
		final int loops = 1 + random.nextInt(100);
		for (int i = 0; i < loops; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		out.flush();
		while (file.length() < minLength) {
			random.nextBytes(buf);
			out.write(buf);
			out.flush();
		}
		out.close();
		assertThat(file.isFile()).isTrue();
		addToFilesInRepo(file);
		return file;
	}

	/** TODO Remove duplicate code: AbstractIT.java and AbstractTest.java */
	protected File createRelativeSymlink(File symlink, final File target) throws IOException {
		assertThat(symlink.exists()).isFalse();
		final File symlinkParent = symlink.getParentFile();

		final String relativeTargetString = symlinkParent.relativize(target);

		if (symlink instanceof IoFile) {
			/* Only do this in a test! If symlink is instance of IoFile,
			 * createSymbolicLink would throw an Exception (by intention)!
			 * But in productive code you would never call this, besides you
			 * know the environment and the implementation supports this.
			 */
			symlink = new NioFileFactory().createFile(symlink.getIoFile());
		}
		symlink.createSymbolicLink(relativeTargetString);
		assertThat(symlink.getAbsoluteFile()).isEqualTo(symlink.getAbsoluteFile());
		assertThat(symlink.existsNoFollow()).isTrue();
		assertThat(symlink.isSymbolicLink()).isTrue();
		addToFilesInRepo(symlink);
		return symlink;
	}

	protected void deleteFile(File file) throws IOException {
		file = file.getAbsoluteFile();
		assertThat(file.exists()).isTrue();
		file.delete();
		assertThat(file.exists()).isFalse();

		final File localRoot = getLocalRootOrFail(file);
		final Set<File> filesInRepo = localRoot2FilesInRepo.get(localRoot);
		if (filesInRepo == null)
			throw new IllegalStateException("No filesInRepo for localRoot: " + localRoot);

		if (!filesInRepo.remove(file))
			throw new IllegalStateException("File did not exist in filesInRepo: " + file);
	}

	private File getLocalRootOrFail(final File file) throws IOException {
		final String filePath = file.getCanonicalPath();
		final Set<File> localRoots = localRepoManagerFactory.getLocalRoots();
		for (final File localRoot : localRoots) {
			final String localRootPath = localRoot.getPath();
			if (filePath.startsWith(localRootPath)) {
				return localRoot;
			}
		}
		throw new IllegalArgumentException("file is not contained in any open repository: " + filePath);
	}

	protected void assertThatFilesInRepoAreCorrect(File localRoot) {
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		localRoot = localRepoManager.getLocalRoot(); // get canonical File
		final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();
		try {
			final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
			Set<File> filesInRepo = localRoot2FilesInRepo.get(localRoot);
			assertThat(filesInRepo).isNotNull();

			for (final File file : filesInRepo) {
				final RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
				if (repoFile == null) {
					Assert.fail("Corresponding RepoFile missing in repository for file: " + file);
				}
				if (file.isSymbolicLink())
					assertThat(repoFile).isInstanceOf(Symlink.class);
				else if (file.isFile())
					assertThat(repoFile).isInstanceOf(NormalFile.class);
				else if (file.isDirectory())
					assertThat(repoFile).isInstanceOf(Directory.class);
			}

			filesInRepo = new HashSet<File>(filesInRepo);
			final Collection<RepoFile> repoFiles = repoFileDao.getObjects();
			final Map<File, RepoFile> file2RepoFile = new HashMap<File, RepoFile>();
			for (final RepoFile repoFile : repoFiles) {
				final File file = repoFile.getFile(localRoot);
				final RepoFile duplicateRepoFile = file2RepoFile.put(file, repoFile);
				if (duplicateRepoFile != null)
					Assert.fail("There are 2 RepoFile instances for the same file! " + repoFile + " " + duplicateRepoFile + " " + file);

				if (!filesInRepo.remove(file))
					Assert.fail("Corresponding file in file-system missing for RepoFile: " + repoFile + " " + file);
			}
		} finally {
			transaction.rollbackIfActive();
			localRepoManager.close();
		}
	}

	protected void assertDirectoriesAreEqualRecursively(final File dir1, final File dir2) throws IOException {
		assertThat(dir1.isDirectory()).isTrue();
		assertThat(dir2.isDirectory()).isTrue();

		final boolean dir1IsSymbolicLink = dir1.isSymbolicLink();
		final boolean dir2IsSymbolicLink = dir2.isSymbolicLink();

		assertThat(dir1IsSymbolicLink).isEqualTo(dir2IsSymbolicLink);

		if (dir1IsSymbolicLink) {
			final String target1 = dir1.readSymbolicLinkToPathString();
			final String target2 = dir2.readSymbolicLinkToPathString();
			assertThat(target1).isEqualTo(target2);
			return;
		}

		final String[] children1 = dir1.list(new FilenameFilterSkipMetaDir());
		assertThat(children1).isNotNull();

		final String[] children2 = dir2.list(new FilenameFilterSkipMetaDir());
		assertThat(children2).isNotNull();

		Arrays.sort(children1);
		Arrays.sort(children2);

//		assertThat(children1).containsOnly(children2);
		assertThat(children1).isEqualTo(children2);

		for (final String childName : children1) {
			final File child1 = createFile(dir1, childName);
			final File child2 = createFile(dir2, childName);

			final boolean child1IsSymbolicLink = child1.isSymbolicLink();
			final boolean child2IsSymbolicLink = child2.isSymbolicLink();

			assertThat(child1IsSymbolicLink).isEqualTo(child2IsSymbolicLink);

			if (child1.getLastModifiedNoFollow() != child2.getLastModifiedNoFollow())
				fail("Different 'lastModified' timestamps: " + child1 + " vs. " + child2);

			if (child1IsSymbolicLink) {
				final String child1Symlink = child1.readSymbolicLinkToPathString();
				final String child2Symlink = child2.readSymbolicLinkToPathString();
				assertThat(child1Symlink).isEqualTo(child2Symlink);
			}
			else if (child1.isFile()) {
				assertThat(child2.isFile());
				assertThat(IOUtil.compareFiles(child1, child2)).isTrue();
			}
			else if (child1.isDirectory())
				assertDirectoriesAreEqualRecursively(child1, child2);
		}
	}

	protected static void enablePostgresql() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME, "postgresql");

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME, getEnvOrFail("TEST_PG_HOST_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME, getEnvOrFail("TEST_PG_USER_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD, getEnvOrFail("TEST_PG_PASSWORD"));

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX, "TEST_");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX, "_TEST");
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	protected static void disablePostgresql() {
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX);
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	protected static String getEnvOrFail(String key) {
		String value = System.getenv(key);
		if (value == null)
			throw new IllegalStateException("Environment-variable not set: " + key);

		return value;
	}
}
