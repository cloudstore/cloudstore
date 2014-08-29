package co.codewizards.cloudstore.oio.test;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFactory;
import co.codewizards.cloudstore.oio.io.IoFileFactory;
import co.codewizards.cloudstore.oio.nio.NioFileFactory;

@RunWith(value = Parameterized.class)
public class FileTest {

	private static final Random RANDOM = new Random();

	private final FileFactory fileFactory;

	private File file = null;

	private final File testBaseDir;

	/** One map for each reflection-test, to compare the two implementations. */
	private static Map<String, String> resultsFile = new ConcurrentHashMap<String, String>();
	private static Map<String, String> resultsTempDir = new ConcurrentHashMap<String, String>();
	private static Map<String, String> resultsTempFile = new ConcurrentHashMap<String, String>();
	/** One extra map for manual added test results. */
	private static Map<String, String> resultsManuallyAdded = new ConcurrentHashMap<String, String>();

	public FileTest(final FileFactory fileFactory) {
		this.fileFactory = fileFactory;
		testBaseDir = getTestRepositoryBaseDir();
	}


	@AfterClass
	public static void afterClass() throws IOException {
		checkResultMap(resultsFile);
		checkResultMap(resultsTempDir);
		checkResultMap(resultsTempFile);
	}

	private static void checkResultMap(final Map<String, String> results) {
		if (!results.isEmpty()) {
			System.err.println("These results are different:");
			for (final Entry<String, String> e : results.entrySet()) {
				System.err.println(e.getKey() + "=" + e.getValue());
			}
			fail("Each result should have been removed on equality!");
		}
	}

	@Before
	public void beforeTestMethod() throws IOException {
		if (file != null && file.exists()) {
			final boolean delete = file.delete();
			if (!delete)
				throw new IllegalStateException("Could not delete file");
		}
		file = createFile("");
		final boolean createNewFile = file.createNewFile();
		if (!createNewFile) {
			throw new IllegalStateException("Could not create file " + file);
		}
		System.out.println("beforeTestMethod: file: " + file);
	}

	@Test
	public final void create() throws IOException {
		final File f = createFile("asdf");

		final boolean createNewFile = f.createNewFile();
		assertThat(createNewFile).isTrue();
		assertThat(f.exists()).isTrue();
	}

	@Test
	public final void delete() throws IOException {
		assertThat(file.exists()).isTrue();
		final boolean delete = file.delete();
		assertThat(delete).isTrue();
		assertThat(file.exists()).isFalse();
	}

	/***** BEGIN some static value assertions, to check changes in implementation *****/
	@Test
	public final void canExecute_file() throws IOException {
		final boolean canExecute = file.canExecute();
		assertThat(canExecute).isFalse();
	}
	@Test
	public final void canExecute_tmpFile() throws IOException {
		final boolean canExecute = fileFactory.createTempFile("asdf1", "qwer").canExecute();
		assertThat(canExecute).isFalse();
	}
	@Test
	public final void canExecute_tmpDir() throws IOException {
		final boolean canExecute = fileFactory.createTempDirectory("asdf2").canExecute();
		assertThat(canExecute).isTrue();
	}

	@Test
	public final void canRead() throws IOException {
		final boolean canRead = file.canRead();
		assertThat(canRead).isTrue();
	}
	@Test
	public final void canRead_tmpFile() throws IOException {
		final boolean canRead = fileFactory.createTempFile("asdf1", "qwer").canRead();
		assertThat(canRead).isTrue();
	}
	@Test
	public final void canRead_tmpDir() throws IOException {
		final boolean canRead = fileFactory.createTempDirectory("asdf2").canRead();
		assertThat(canRead).isTrue();
	}

	@Test
	public final void canWrite() throws IOException {
		final boolean canWrite = file.canWrite();
		assertThat(canWrite).isTrue();
	}
	@Test
	public final void canWrite_tmpFile() throws IOException {
		final boolean canWrite = fileFactory.createTempFile("asdf1", "qwer").canWrite();
		assertThat(canWrite).isTrue();
	}
	@Test
	public final void canWrite_tmpDir() throws IOException {
		final boolean canWrite = fileFactory.createTempDirectory("asdf2").canWrite();
		assertThat(canWrite).isTrue();
	}

	@Test
	public final void isAbsolute_file() throws IOException {
		final boolean canWrite = file.isAbsolute();
		assertThat(canWrite).isFalse();
	}
	@Test
	public final void isAbsolute_tmpFile() throws IOException {
		final boolean canWrite = fileFactory.createTempFile("asdf1", "qwer").isAbsolute();
		assertThat(canWrite).isTrue();
	}
	@Test
	public final void isAbsolute_tmpDir() throws IOException {
		final boolean canWrite = fileFactory.createTempDirectory("asdf2").isAbsolute();
		assertThat(canWrite).isTrue();
	}

	/***** END some static value assertions, to check changes in implementation *****/

	/***** BEGIN rename/move/copy/ *****/

	@Test
	public final void rename_file() throws IOException {
		final File dest = createFile("rename-dest0");
		final boolean renamed = file.renameTo(dest);
		assertThat(renamed).isTrue();
	}
	@Test
	public final void rename_tmpFile() throws IOException {
		final File tmpFile = fileFactory.createTempFile("asdf1", "qwer");
		final File dest = createFile("rename-dest1-" + Math.abs(RANDOM.nextInt())).getAbsoluteFile();
		final boolean renamed = tmpFile.renameTo(dest);
		// the result is system dependent (plz see {@link java.io.File#renameTo(java.io.File)})
		compareResults(resultsManuallyAdded, "rename_tmpFile", Boolean.toString(renamed));

		final File tmpDestFile = fileFactory.createTempFile("asdf1-dest", "qwer");
		final boolean renamedTmp = tmpFile.renameTo(tmpDestFile);
		compareResults(resultsManuallyAdded, "rename_tmpFile_renamedTmp", Boolean.toString(renamedTmp));
	}
	@Test
	public final void rename_tmpDir() throws IOException {
		final File tmpDir = fileFactory.createTempDirectory("asdf2");
		final File dest = createFile("rename-dest2-" + Math.abs(RANDOM.nextInt())).getAbsoluteFile();
		final boolean renamed = tmpDir.renameTo(dest);
		// the result is system dependent (plz see {@link java.io.File#renameTo(java.io.File)})
		compareResults(resultsManuallyAdded, "rename_tmpFile", Boolean.toString(renamed));

		final File tmpDestDir = fileFactory.createTempDirectory("asdf2-dest");
		final boolean renamedTmp = tmpDir.renameTo(tmpDestDir);
		compareResults(resultsManuallyAdded, "rename_tmpFile_renamedTmp", Boolean.toString(renamedTmp));
	}

	@Test
	public final void move_file() throws IOException {
		final String destName = "move-dest0";
		final File dest = createFile(destName);
		file.move(dest.getAbsoluteFile());
		assertThat(dest.getAbsolutePath()).contains(destName);
		assertThat(dest.exists()).isTrue();
		assertThat(file.exists()).isFalse();
	}
	@Test
	public final void move_tmpFile_differentPartitions() throws IOException {
		final File tmpFile = fileFactory.createTempFile("asdf1", "qwer");
		final String prefix = "move-dest1-" + Math.abs(RANDOM.nextInt());
		final File dest = createFile(prefix).getAbsoluteFile();
		// move works even between partitions
		tmpFile.move(dest);
		assertThat(dest.exists()).isTrue();
		assertThat(tmpFile.exists()).isFalse();
	}
	@Test
	public final void move_tmpDir_samePartition() throws IOException {
		final File tmpDir = fileFactory.createTempDirectory("asdf2");
		final File dest = fileFactory.createTempDirectory("asdf2-dest2").getAbsoluteFile();
		// move works even between partitions
		try {
			tmpDir.move(dest);
		} catch (final Exception e) {
			//ignore the exception, just check the equality of both results.
		}
		compareResults(resultsManuallyAdded, "move_tmpDir_samePartition_tmpDir.exists", Boolean.toString(tmpDir.exists()));
		compareResults(resultsManuallyAdded, "move_tmpDir_samePartition_tmpDir.listFiles", getListFilesCount(tmpDir));
		compareResults(resultsManuallyAdded, "move_tmpDir_samePartition_dest.exists", Boolean.toString(dest.exists()));
		compareResults(resultsManuallyAdded, "move_tmpDir_samePartition_dest.listFiles", getListFilesCount(dest));
	}
	@Test
	public final void move_tmpDir_recursive_differentPartitions() throws IOException {
		//source is in /tmp
		final File tmpDir = fileFactory.createTempDirectory("asdf3");
		final File childFile = fileFactory.createFile(tmpDir, "childFile");
		final boolean createNewFile = childFile.createNewFile();
		final File childDir = fileFactory.createFile(tmpDir, "childDir");
		final boolean mkdir = childDir.mkdir();
		if (!createNewFile || !mkdir)
			throw new IllegalStateException("Test setup failed");

		// destination is in target-folder
		final String prefix = "move-dest2-" + Math.abs(RANDOM.nextInt());
		final File dest = createFile(prefix).getAbsoluteFile();
		try {
			// result of move depends on partitioning of the tmp-folder.
			tmpDir.move(dest);
			compareResults(resultsManuallyAdded, "move_tmpDir_recursive_differentPartitions_moveWorks", Boolean.TRUE.toString());
		} catch (final IOException e) {
			compareResults(resultsManuallyAdded, "move_tmpDir_recursive_differentPartitions_moveWorks", Boolean.FALSE.toString());
		}
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_differentPartitions_destExists", Boolean.toString(dest.exists()));
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_differentPartitions_tmpDirListFilesSize", getListFilesCount(tmpDir));
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_differentPartitions_destListFilesSize", getListFilesCount(dest));
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_differentPartitions_tmpDirExists", Boolean.toString(tmpDir.exists()));
	}
	@Test
	public final void move_tmpDir_recursive_samePartition() throws IOException {
		final File tmpDir = fileFactory.createTempDirectory("asdf2");
		final File childFile = fileFactory.createFile(tmpDir, "childFile");
		final boolean createNewFile = childFile.createNewFile();
		final File childDir = fileFactory.createFile(tmpDir, "childDir");
		final boolean mkdir = childDir.mkdir();
		if (!createNewFile || !mkdir)
			throw new IllegalStateException("Test setup failed");

		final File dest = fileFactory.createTempFile("dest-prefix", "dest-suffix", fileFactory.createTempDirectory("destination-asdf3"));
		// move works even between partitions
		try {
			tmpDir.move(dest);
		} catch (final Exception e) {
			//ignore the exception, just check the equality of both results.
		}
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_samePartition_tmpDir.exists", Boolean.toString(tmpDir.exists()));
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_samePartition_tmpDir.listFiles", getListFilesCount(tmpDir));
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_samePartition_dest.exists", Boolean.toString(dest.exists()));
		compareResults(resultsManuallyAdded, "move_tmpDir_recursive_samePartition_dest.listFiles", getListFilesCount(dest));
	}

	/** Returns listFiles.length as String or appropriate describing String. */
	private String getListFilesCount(final File dest) {
		if (!dest.exists())
			return "!exists";
		final File[] listFiles = dest.listFiles();
		if (listFiles == null)
			return "null";
		return Integer.toString(listFiles.length);
	}

	@Test
	public final void copy_file() throws IOException {
		final String destName = "copy-dest0";
		final File dest = createFile(destName);
		file.copyToCopyAttributes(dest.getAbsoluteFile());
		assertThat(dest.getAbsolutePath()).contains(destName);
		assertThat(dest.exists()).isTrue();
		compareAttributes(file, dest);
	}
	@Test
	public final void copy_tmpFile() throws IOException {
		final File tmpFile = fileFactory.createTempFile("asdf1", "qwer");
		assertThat(tmpFile.exists()).isTrue();
		final String prefix = "copy-dest1-" + Math.abs(RANDOM.nextInt());
		final File dest = createFile(prefix).getAbsoluteFile();
		tmpFile.copyToCopyAttributes(dest);
		assertThat(tmpFile.exists()).isTrue();
		assertThat(dest.exists()).isTrue();
		compareAttributes(tmpFile, dest);
	}
	@Test
	public final void copy_tmpDir() throws IOException {
		final File tmpDir = fileFactory.createTempDirectory("asdf2");
		final String prefix = "copy-dest2-" + Math.abs(RANDOM.nextInt());
		final File dest = createFile(prefix).getAbsoluteFile();
		tmpDir.copyToCopyAttributes(dest);
		assertThat(dest.exists()).isTrue();
		compareAttributes(tmpDir, dest);
	}
	@Test
	public final void copy_tmpDir_checkNonRecursive() throws IOException {
		final File tmpDir = fileFactory.createTempDirectory("asdf2");
		final File childFile = fileFactory.createFile(tmpDir, "childFile");
		final boolean createNewFile = childFile.createNewFile();
		final File childDir = fileFactory.createFile(tmpDir, "childDir");
		final boolean mkdir = childDir.mkdir();
		if (!createNewFile || !mkdir)
			throw new IllegalStateException("Test setup failed");

		final String prefix = "copy-dest2-" + Math.abs(RANDOM.nextInt());
		final File dest = createFile(prefix).getAbsoluteFile();

		tmpDir.copyToCopyAttributes(dest);

		assertThat(dest.exists()).isTrue();
		assertThat(dest.listFiles()).isEmpty();
		compareAttributes(tmpDir, dest);
	}
	/***** END move/copy/rename *****/


	/***** BEGIN Compare the two implementations: check methods with boolean return values with no arguments *****/
	@Test
	public final void compareBooleanReturnResults_file() throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (final Method method : File.class.getDeclaredMethods()) {
			final File file = createFile("cf");
			final Class<?> returnType = method.getReturnType();
			if (!returnType.equals(Boolean.TYPE)) {
				continue;
			}
			// check methods without parameters
			final Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length > 0) {
				continue;
			}
			final String name = method.getName();
			final Boolean b = (Boolean) method.invoke(file);
			compareResults(resultsFile, name, Boolean.toString(b));
		}
	}
	@Test
	public final void compareBooleanReturnResults_tempFile() throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (final Method method : File.class.getDeclaredMethods()) {
			final File file = fileFactory.createTempFile("ctf-asdf", "ctf-qwer");
			final Class<?> returnType = method.getReturnType();
			if (!returnType.equals(Boolean.TYPE)) {
				continue;
			}
			// check methods without parameters
			final Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length > 0) {
				continue;
			}
			final String name = method.getName();
			final Boolean b = (Boolean) method.invoke(file);
			compareResults(resultsTempFile, name, Boolean.toString(b));
		}
	}
	@Test
	public final void compareBooleanReturnResults_tempDir() throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (final Method method : File.class.getDeclaredMethods()) {
			final File file = fileFactory.createTempDirectory("ctd");
			final Class<?> returnType = method.getReturnType();
			if (!returnType.equals(Boolean.TYPE)) {
				continue;
			}
			// check methods without parameters
			final Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length > 0) {
				continue;
			}
			final String name = method.getName();
			final Boolean b = (Boolean) method.invoke(file);
			compareResults(resultsTempDir, name, Boolean.toString(b));
		}
	}
	/***** END Compare the two implementations: check methods with boolean return values with no arguments *****/


	@Test
	public void lastModifiedNoFollow() {
		final long tolerance = 1000;
		long lastModified = file.lastModified();
		final long currentTimeMillis = System.currentTimeMillis();
		assertThat(lastModified).isBetween(currentTimeMillis - tolerance, currentTimeMillis + tolerance);

		final long newLastModified = currentTimeMillis - 60 * 1000;
		file.setLastModifiedNoFollow(newLastModified);
		lastModified = file.lastModified();
		assertThat(lastModified).isBetween(newLastModified - tolerance, newLastModified + tolerance);
	}

	@Test
	public void relativize() throws IOException {
		final File root = fileFactory.createTempDirectory("root");

		final File childFileA1 = fileFactory.createFile(root, "childFileA1");
		assertThat(childFileA1.mkdir()).isTrue();

		final File childFileA2 = fileFactory.createFile(childFileA1, "childFileA2");
		assertThat(childFileA2.createNewFile()).isTrue();


		final File childFileB1 = fileFactory.createFile(root, "childFileB1");
		assertThat(childFileB1.mkdir()).isTrue();

		final File childFileB2 = fileFactory.createFile(childFileB1, "childFileB2");
		assertThat(childFileB2.createNewFile()).isTrue();

		assertThat(childFileB2.relativize(root)).isEqualTo("../..");
		assertThat(childFileB1.relativize(root)).isEqualTo("..");
		assertThat(childFileA2.relativize(root)).isEqualTo("../..");
		assertThat(childFileA1.relativize(root)).isEqualTo("..");

		assertThat(childFileB2.relativize(childFileA1)).isEqualTo("../../" + "childFileA1");
		assertThat(childFileB2.relativize(childFileA2)).isEqualTo("../../" + "childFileA1" + "/" + "childFileA2");
		assertThat(childFileB1.relativize(childFileA1)).isEqualTo("../" + "childFileA1");
		assertThat(childFileB1.relativize(childFileA2)).isEqualTo("../" + "childFileA1" + "/" + "childFileA2");

	}


	private void compareAttributes(final File source, final File target) {
		assertThat(source.isDirectory()).isEqualTo(target.isDirectory());
		assertThat(source.isFile()).isEqualTo(target.isFile());
		assertThat(source.canExecute()).isEqualTo(target.canExecute());
		assertThat(source.canWrite()).isEqualTo(target.canWrite());
		assertThat(source.canRead()).isEqualTo(target.canRead());
	}

	private void compareResults(final Map<String, String> results, final String key, final String value) {
		final String earlierResult = results.get(key);
		if (earlierResult == null) {
			results.put(key, value);
		} else {
			if (earlierResult.equals(value)) {
				results.remove(key);
			} else {
				fail("compareResults: " + key + " was '" + value + "' instead of '" + earlierResult + "';"
						+ " current factory=" + fileFactory.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Creates a file object, but does not make the according IO store/create
	 * operation. Appends the name of the factory to the prefix.
	 * <p/>
	 * Uses a directory in the projects target-folder.
	 */
	private File createFile(final String prefix) {
//		System.out.println("fileFactory="
//				+ fileFactory.getClass().getSimpleName());
		final long id = Math.abs(RANDOM.nextInt());
		final File f = fileFactory.createFile(testBaseDir, prefix + "-" + id
				+ "-" + fileFactory.getClass().getSimpleName());
//		System.out.println("created file: " + f);
		return f;
	}

	protected File getTestRepositoryBaseDir() {
		final File dir = fileFactory.createFile(fileFactory
				.createFile("target"), this.getClass().getSimpleName());
		dir.mkdirs();
		return dir;
	}

	@Parameterized.Parameters(name = "{index}:{0}:{1}")
	public static Collection<Object[]> instancesToTest() {
		return Arrays.asList(new Object[] { new NioFileFactory() },
				new Object[] { new IoFileFactory() });
	}

}
