package co.codewizards.cloudstore.core.oio.file;

import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;


/**
 * @author Sebastian Schefczyk
 *
 */
public interface File extends OioService {

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	File createNewFile(String pathname);
	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	File createNewFile(File parent, String child);
	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	File createNewFile(String parent, String child);
	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	File createNewFile(URI uri);


	File getParentFile();

	File[] listFiles();
	File[] listFiles(FileFilter fileFilter);
	File[] listFiles(FilenameFilter fileFilter);

	File getAbsoluteFile();

	boolean isSymbolicLink();

	String readSymbolicLinkToPathString() throws IOException;
	boolean exists();
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
	long length();
	void renameTo(File newFileName);
	void setLastModified(long lastModified);
	OutputStream createFileOutputStream() throws FileNotFoundException;
	InputStream createFileInputStream() throws FileNotFoundException;
	String getName();
	void createSymbolicLink(String targetPath) throws IOException;

	long lastModified();
	long getLastModifiedNoFollow();
	boolean isAbsolute();
	String getPath();
	boolean mkdirs();
	void copyToCopyAttributes(File toFile) throws IOException;
	void move(File toFile) throws IOException;
	URI toURI();
	RandomAccessFile createRandomAccessFile(String mode) throws FileNotFoundException;
	boolean isFile();
	void setLastModifiedNoFollow(long time);

}
