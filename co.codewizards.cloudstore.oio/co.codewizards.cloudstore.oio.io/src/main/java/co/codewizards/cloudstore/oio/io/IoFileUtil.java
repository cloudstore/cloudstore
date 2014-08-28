package co.codewizards.cloudstore.oio.io;

import java.io.IOException;
import java.util.Stack;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * @author Sebastian Schefczyk
 */
public class IoFileUtil {

	/**
	 * Discussion for best solution:
	 * http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
	 */
	public static File createTempDirectory(final String prefix) throws IOException {
		final String checkedPrefix = (prefix == null) ? "" : prefix;
		final java.io.File javaIoTmpDir = new java.io.File(System.getProperty("java.io.tmpdir"));
		final String baseName = checkedPrefix + System.currentTimeMillis();
		final int attempts = 1000;
		for (int i = 0; i < attempts; i++) {
			final java.io.File tmpDir = new java.io.File(javaIoTmpDir, baseName + i);
			if (tmpDir.mkdir()) {
				return new IoFile(tmpDir);
			}
		}
		throw new IllegalStateException("Could not create a tmpDir in the directory '" + javaIoTmpDir + "'!");
	}

	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return new IoFile(java.io.File.createTempFile (prefix, suffix));
	}

	public static File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return new IoFile(java.io.File.createTempFile (prefix, suffix, castOrFail(dir).ioFile));
	}

	public static java.io.File getIoFile(final File file) {
		return castOrFail(file).ioFile;
	}


	public static IoFile castOrFail(final File file) {
		if (file instanceof IoFile)
			return (IoFile) file;
		else
			throw new IllegalArgumentException("Could not cast file: "
					+ file.getClass().getCanonicalName());
	}

	static File[] convert(final java.io.File[] ioFilesListFiles) {
		if (ioFilesListFiles == null)
			return null;
		final File[] listFiles = new IoFile[ioFilesListFiles.length];
		for (int i = 0; i < ioFilesListFiles.length; i++) {
			listFiles[i] = new IoFile(ioFilesListFiles[i]);
		}
		return listFiles;
	}

	/**
	 * rm -r; with a stack, without recursion.
	 * <p/>
	 * Discussion of best solution: http://stackoverflow.com/a/10337535/2287604
	 *
	 * @param dir The directory to delete recursively.
	 */
	public static void deleteRecursively(final java.io.File dir) {
		java.io.File[] currList;
		final Stack<java.io.File> stack = new Stack<java.io.File>();
		stack.push(dir);
		while (!stack.isEmpty()) {
			if (stack.lastElement().isDirectory()) {
				currList = stack.lastElement().listFiles();
				if (null != currList && currList.length > 0) {
					for (final java.io.File curr : currList) {
						stack.push(curr);
					}
				} else {
					stack.pop().delete();
				}
			} else {
				stack.pop().delete();
			}
		}
	}

	/**
	 * Directories will be created, files renamed.
	 * There is no rollback, if something happens during operation.
	 */
	public static void moveRecursively(final File fromDir, final File toDir) throws IOException {
		checkRenameDir(fromDir, toDir);
		final boolean mkdir = toDir.mkdir();
		if (!mkdir)
			throw new IOException("Could not create directory toDir, aborting move!");

		final File[] listFiles = fromDir.listFiles();
		for (final File file : listFiles) {
			final File newFileName = newFileName(fromDir, toDir, file);
			if (file.isDirectory()) {
				newFileName.mkdir();
				 // remove this ; this could crash on big file collections!
				moveRecursively(file, newFileName);
			} else if (file.isFile()) {
				file.renameTo(newFileName);
			}
		}
		final boolean delete = fromDir.delete();
		if (!delete)
			throw new IOException("Could not delete directory, which should be empty, aborting move! file=" + fromDir.getAbsolutePath());

	}


	public static File newFileName(final File fromDir, final File toDir, final File current) throws IOException {
		final String newParentDirName = toDir.getAbsolutePath() + java.io.File.separator + IOUtil.getRelativePath(fromDir, current.getPath());
		return new IoFile(newParentDirName);
	}

	protected static void checkRenameDir(final File fromDir, final File toDir) throws IOException {
		//first, lets check with a simple rename:
		if (!fromDir.isDirectory())
			throw new IOException("fromDir must be directory, aborting move!");
		final File f = new IoFile(fromDir.getParentFile(), Long.toString(System.currentTimeMillis()));
		final boolean mkdir = f.mkdir();
		if (!mkdir)
			throw new IOException("Can not create mkdir, aborting move!");
		final boolean renameTo = f.renameTo(toDir);
		if (!renameTo)
			throw new IOException("Rename would not work, aborting move!");
		final boolean delete = toDir.delete();
		if (!delete)
			throw new IOException("Could not delete, right after renaming!!!");
	}

}
