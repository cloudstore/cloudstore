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

	String readSymbolicLinkToPathString() throws IOException;
	boolean exists();
	boolean existsNoFollow();
	boolean canWrite();
	boolean canRead();
	boolean canExecute();
	boolean createNewFile() throws IOException;
	int compareTo(File otherFile);
	boolean delete();
	void deleteOnExit();
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
	boolean renameTo(File newFileName);
	boolean setLastModified(long lastModified);
	OutputStream createFileOutputStream() throws FileNotFoundException;
	OutputStream createFileOutputStream(boolean append) throws FileNotFoundException;
	InputStream createFileInputStream() throws FileNotFoundException;
	String getName();
	String createSymbolicLink(String targetPath) throws IOException;

	long lastModified();
	long getLastModifiedNoFollow();
	boolean isAbsolute();
	String getPath();
	boolean mkdirs();
	String copyToCopyAttributes(File toFile) throws IOException;
	String move(File toFile) throws IOException;
	URI toURI();
	RandomAccessFile createRandomAccessFile(String mode) throws FileNotFoundException;
	boolean isFile();
	void setLastModifiedNoFollow(long time);
	String relativize(File target);

	boolean setExecutable(boolean executable, boolean ownerOnly);
	/** <b>Caution:</b> <i>Only use this when forced by 3rd party interface!</i> */
	java.io.File getIoFile();

}
