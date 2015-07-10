package co.codewizards.cloudstore.core.oio;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sebastian Schefczyk
 */
public class IoFileUtil {

	private IoFileUtil() { }

	private static final Logger logger = LoggerFactory.getLogger(IoFileUtil.class);

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
				return createFile(tmpDir);
			}
		}
		throw new IllegalStateException("Could not create a tmpDir in the directory '" + javaIoTmpDir + "'!");
	}

	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return createFile(java.io.File.createTempFile (prefix, suffix));
	}

	public static File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return createFile(java.io.File.createTempFile (prefix, suffix, castOrFail(dir).ioFile));
	}

	public static java.io.File getIoFile(final File file) {
		return castOrFail(file).ioFile;
	}

	public static File[] listRoots() {
		final java.io.File[] roots = java.io.File.listRoots();
		assertNotNull("java.io.File.listRoots()", roots);
		final File[] result = new File[roots.length];
		for (int i = 0; i < roots.length; i++)
			result[i] = createFile(roots[i]);

		return result;
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
	 * Deletes recursively, but uses a stack instead of recursion, so it should be
	 * safe for very large file storages.
	 * <p/>
	 * If a symlink is found, only this link will be deleted,
	 * neither the symlinked directory nor its content (due to {@link File#delete()}).
	 * <p/>
	 * Discussion of best solution: http://stackoverflow.com/a/10337535/2287604
	 *
	 * @param dir The directory to delete recursively.
	 * @return True, if the param dir does not exist at the end.
	 */
	static boolean deleteRecursively(final File dir) {
		final LinkedList<File> stack = new LinkedList<File>();
		stack.addFirst(dir);
		while (!stack.isEmpty()) {
			final File stackElement = stack.getFirst();
			final File[] currList = stackElement.listFiles();
			if (null != currList && currList.length > 0) {
				for (final File curr : currList) {
					try {
						final boolean delete = curr.delete();
						/* Remark: delete() should succeed on empty directories, files and symlinks;
						 * only if was not successful, this *directory* should be
						 * pushed to the stack. So symlinked directories
						 * and its contents will not be deleted.
						 */
						if (!delete) {
							stack.addFirst(curr);
						}
					} catch(final SecurityException e) {
						logger.warn("Problem on delete of '{}'! ", curr, e.getMessage());
					}
				}
			} else {
				if (stackElement != stack.removeFirst())
					throw new IllegalStateException("WTF?!");

				deleteOrLog(stackElement);
			}
		}
		return !dir.exists();
	}

	private static void deleteOrLog(final File file) {
		try {
			file.delete();
		} catch(final SecurityException e) {
			logger.warn("Problem on delete of '{}'! ", file, e.getMessage());
		}
	}

	/**
	 * Directories will be created/deleted, files renamed.
	 * There is no rollback, if something happens during operation.
	 * @throws IOException
	 * 			If a directory itself could not be renamed, created or deleted.
	 * @throws SecurityException Not catched. If this happens, some files will
	 * 			be moved, some not, but no data loss should have happened.
	 */
	public static void moveRecursively(final File fromDir, final File toDir) throws IOException {
		checkRenameDir(fromDir, toDir);
		final boolean mkdir = toDir.mkdir();
		if (!mkdir)
			throw new IOException("Could not create directory toDir, aborting move!");

		final File[] listFiles = fromDir.listFiles();
		for (final File file : listFiles) {
			final File newFileName = newFileNameForRenameTo(fromDir, toDir, file);
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

	/**
	 * @param fromDir The from-dir of a move operation.
	 * @param toDir The destination dir of a move operation.
	 * @param current The current file, to compute the new name for. Must be inside of fromDir
	 * @return On moving from /a/fromDir/ to /a/toDir/, and current file is
	 * 			/a/fromDir/b/c, it will return /a/toDir/b/c
	 * @throws IOException
	 */
	public static File newFileNameForRenameTo(final File fromDir, final File toDir, final File current) throws IOException {
		final String newParentDirName = toDir.getAbsolutePath() + OioFileFactory.FILE_SEPARATOR_CHAR +
				IoFileRelativePathUtil.getRelativePath(fromDir.getAbsolutePath(), current.getAbsolutePath(), true, OioFileFactory.FILE_SEPARATOR_CHAR);
		return createFile(newParentDirName);
	}

	/** Before starting a moveRecursively operation, which will make use of the
	 * File.renameTo method, it is useful to determine, whether this is possible
	 * or not. The reason: On a recursive rename, you will first move the inner
	 * content, and as last the root-directory.
	 */
	protected static void checkRenameDir(final File fromDir, final File toDir) throws IOException {
		//first, lets check with a simple rename:
		if (!fromDir.isDirectory())
			throw new IOException("fromDir must be directory, aborting move!");
		final File f = createFile(fromDir.getParentFile(), Long.toString(System.currentTimeMillis()));
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
