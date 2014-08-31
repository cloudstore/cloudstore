package co.codewizards.cloudstore.core.oio;

import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;


/**
 * Substitute for java.io.File. Also there are many methods as known from
 * java.nio.file.Files and java.nio.file.Paths.
 *
 * @author Sebastian Schefczyk
 */
public interface File {

	String[] list();
	String[] list(FilenameFilter filenameFilter);
	File[] listFiles();
	File[] listFiles(FileFilter fileFilter);
	File[] listFiles(FilenameFilter fileFilter);

	File getAbsoluteFile();
	File getParentFile();

	boolean isSymbolicLink();

	boolean canWrite();
	boolean canRead();
	boolean canExecute();
	boolean setExecutable(boolean executable);
	boolean setReadable(boolean readable);
	boolean setReadable(boolean readable, boolean ownerOnly);
	boolean setWritable(boolean writable);
	boolean setWritable(boolean writable, boolean ownerOnly);

	/** Convenience method for the often called chain:
	 * <p/>
	 * java.io.File --&gt; java.nio.Path --&gt; Files.readSymbolicLink --&gt; IOUtil.toPathString
	 * <p/>Without symlinks, this method would not be needed.
	 */
	String readSymbolicLinkToPathString() throws IOException;
	boolean exists();
	boolean existsNoFollow();
	boolean createNewFile() throws IOException;
	int compareTo(File otherFile);
	boolean delete();
	void deleteOnExit();
	void deleteRecursively();
	long getFreeSpace();
	String getCanonicalPath() throws IOException;
	File getCanonicalFile() throws IOException;
	String getAbsolutePath();
	boolean isRegularFileFollowLinks();
	boolean isRegularFileNoFollowLinks();
	boolean mkdir();
	boolean isDirectory();
	boolean isDirectoryFileNoFollowSymLinks();
	boolean isDirectoryFollowSymLinks();
	long getUsableSpace();
	long length();
	/** This is platform dependent (e.g. might fail at renaming between different partitions). Plz see {@link java.io.File#renameTo(java.io.File)}. */
	boolean renameTo(File newFileName);
	boolean setLastModified(long lastModified);
	OutputStream createFileOutputStream() throws FileNotFoundException;
	OutputStream createFileOutputStream(boolean append) throws FileNotFoundException;
	InputStream createFileInputStream() throws FileNotFoundException;
	String getName();
	void createSymbolicLink(String targetPath) throws IOException;

	long lastModified();
	long getLastModifiedNoFollow();
	boolean isAbsolute();
	String getPath();
	boolean mkdirs();
	/** Copies a file, a symlink (depends on environment/implementation) or a directory (non-recursive). */
	void copyToCopyAttributes(File toFile) throws IOException;
	/** This is platform independent, in contrast to {@link #renameTo(File)} respectively {@link java.io.File#renameTo(java.io.File)}. */
	void move(File toFile) throws IOException;
	URI toURI();
	RandomAccessFile createRandomAccessFile(String mode) throws FileNotFoundException;
	boolean isFile();
	void setLastModifiedNoFollow(long time);
	String relativize(File target) throws IOException;

	boolean setExecutable(boolean executable, boolean ownerOnly);
	/** <b>Caution:</b> <i>Only use this when forced by 3rd party interface!</i> */
	java.io.File getIoFile();

}
