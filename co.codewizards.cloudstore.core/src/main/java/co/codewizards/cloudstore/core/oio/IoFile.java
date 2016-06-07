package co.codewizards.cloudstore.core.oio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * @author Sebastian Schefczyk
 *
 */
public class IoFile implements File {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(IoFile.class);


	protected final java.io.File ioFile;

	protected IoFile(final String pathname) {
		this.ioFile = new java.io.File(pathname);
//		_debug_assert_NioFile();
	}

	protected IoFile(final File parent, final String child) {
		final java.io.File ioParent = parent.getIoFile();
		this.ioFile = new java.io.File(ioParent, child);
//		_debug_assert_NioFile();
	}

	protected IoFile(final String parent, final String child) {
		this.ioFile = new java.io.File(parent, child);
//		_debug_assert_NioFile();
	}

	protected IoFile(final URI uri) {
		this.ioFile = new java.io.File(uri);
//		_debug_assert_NioFile();
	}

	protected IoFile(final java.io.File ioFile) {
		this.ioFile = ioFile;
//		_debug_assert_NioFile();
	}

//	private final void _debug_assert_NioFile() {
//		if (this.getClass() == IoFile.class)
//			throw new IllegalStateException("This should not be an instance of IoFile! " + ioFile);
//
//		if (! this.getClass().getSimpleName().equals("NioFile"))
//			throw new IllegalStateException("This should be an instance of NioFile! " + ioFile);
//	}


	@Override
	public File getParentFile() {
		final java.io.File parentFile = this.ioFile.getParentFile();
		return parentFile != null ? new IoFile(parentFile) : null;
	}

	@Override
	public String[] list() {
		return this.ioFile.list();
	}

	@Override
	public String[] list(final FilenameFilter filenameFilter) {
		return this.ioFile.list(filenameFilter);
	}

	@Override
	public File[] listFiles() {
		final java.io.File[] ioFiles = this.ioFile.listFiles();
		return IoFileUtil.convert(ioFiles);
	}

	@Override
	public File[] listFiles(final java.io.FileFilter fileFilter) {
		final java.io.File[] ioFiles = this.ioFile.listFiles(fileFilter);
		return IoFileUtil.convert(ioFiles);
	}

	@Override
	public File[] listFiles(final FileFilter fileFilter) {
		final java.io.File[] ioFiles = this.ioFile.listFiles(new FileFilterWrapper(fileFilter));
		return IoFileUtil.convert(ioFiles);
	}

	@Override
	public File[] listFiles(final FilenameFilter fileFilter) {
		final java.io.File[] ioFiles = this.ioFile.listFiles(fileFilter);
		return IoFileUtil.convert(ioFiles);
	}

	@Override
	public File getAbsoluteFile() {
		return new IoFile(ioFile.getAbsoluteFile());
	}

	@Override
	public boolean exists() {
		return ioFile.exists();
	}

	@Override
	public boolean existsNoFollow() {
		return ioFile.exists();
	}
	@Override
	public boolean createNewFile() throws IOException {
		return ioFile.createNewFile();
	}

	@Override
	public boolean canExecute() {
		return ioFile.canExecute();
	}

	@Override
	public boolean canRead() {
		return ioFile.canRead();
	}

	@Override
	public boolean canWrite() {
		return ioFile.canWrite();
	}

	@Override
	public boolean setExecutable(final boolean executable) {
		return ioFile.setExecutable(executable);
	}
	@Override
	public boolean setExecutable(final boolean executable, final boolean ownerOnly) {
		return ioFile.setExecutable(executable, ownerOnly);
	}

	@Override
	public boolean setReadable(final boolean readable) {
		return ioFile.setReadable(readable);
	}
	@Override
	public boolean setReadable(final boolean readable, final boolean ownerOnly) {
		return ioFile.setReadable(readable, ownerOnly);
	}

	@Override
	public boolean setWritable(final boolean writable) {
		return ioFile.setWritable(writable);
	}
	@Override
	public boolean setWritable(final boolean writable, final boolean ownerOnly) {
		return ioFile.setWritable(writable, ownerOnly);
	}

	@Override
	public int compareTo(final File otherFile) {
		return ioFile.compareTo(otherFile.getIoFile());
	}

	@Override
	public boolean delete() {
		return ioFile.delete();
	}

	@Override
	public void deleteOnExit() {
		ioFile.deleteOnExit();
	}

	@Override
	public void deleteRecursively() {
		IoFileUtil.deleteRecursively(this);
	}

	@Override
	public String getAbsolutePath() {
		return ioFile.getAbsolutePath();
	}

	@Override
	public File getCanonicalFile() throws IOException {
		return new IoFile(ioFile.getCanonicalFile());
	}

	@Override
	public String getCanonicalPath() throws IOException {
		return ioFile.getCanonicalPath();
	}

	@Override
	public long getFreeSpace() {
		return ioFile.getFreeSpace();
	}

	@Override
	public long length() {
		return ioFile.length();
	}

	@Override
	public boolean isRegularFileNoFollowLinks() {
		return this.ioFile.isFile();
	}

	@Override
	public boolean isRegularFileFollowLinks() {
		return this.ioFile.isFile();
	}

	@Override
	public boolean isDirectoryNoFollowSymLinks() {
		return this.ioFile.isDirectory();
	}

	@Override
	public boolean isDirectoryFollowSymLinks() {
		return this.ioFile.isDirectory();
	}

	@Override
	public boolean isSymbolicLink() {
		// currently: no support for symlinks in this implementation
		return false;
	}

	@Override
	public String readSymbolicLinkToPathString() throws IOException {
		throw new IllegalStateException("Impossible operation within this implementation: check use method 'isSymbolicLink' before!");
	}

	@Override
	public long getLastModifiedNoFollow() {
		// currently: no support for symlinks in this implementation => cannot do anything about follow/no-follow (that's exactly the reason for the nio-implementation!)
		return lastModified();
	}

	@Override
	public boolean renameTo(final File dest) {
		return ioFile.renameTo(dest.getIoFile());
	}

	@Override
	public boolean setLastModified(final long lastModified) {
		return ioFile.setLastModified(lastModified);
	}

	@Override
	public OutputStream createOutputStream() throws FileNotFoundException {
		return new FileOutputStream(ioFile);
	}

	@Override
	public InputStream createInputStream() throws FileNotFoundException {
		return new FileInputStream(ioFile);
	}

	@Override
	public OutputStream createOutputStream(final boolean append) throws FileNotFoundException {
		return new FileOutputStream(ioFile, append);
	}

	@Override
	public String getName() {
		return ioFile.getName();
	}

	@Override
	public void createSymbolicLink(final String targetPath) throws IOException {
		throw new IllegalStateException("Impossible operation within this implementation. Check whether symlinks are available here!");
	}

	@Override
	public long lastModified() {
		final long result = ioFile.lastModified();
		return result;
	}

	@Override
	public boolean isAbsolute() {
		return ioFile.isAbsolute();
	}

	@Override
	public String getPath() {
		return ioFile.getPath();
	}

	@Override
	public boolean mkdir() {
		return ioFile.mkdir();
	}

	@Override
	public boolean mkdirs() {
		return ioFile.mkdirs();
	}

	@Override
	public boolean isDirectory() {
		return ioFile.isDirectory();
	}

	@Override
	public void move(final File toFile) throws IOException {
		if (toFile.exists())
			throw new IOException("toFile file did already exists!");
		if (!this.ioFile.exists())
			throw new IllegalArgumentException("Source file did not exists!");
		if (this.ioFile.getCanonicalPath().equals(toFile.getCanonicalPath()))
			return; //nothing to do!

		final boolean renameTo = this.ioFile.renameTo(toFile.getIoFile());
		// we try to do a simple rename, but this won't be successful between partitions or non-empty directories.
		if (renameTo)
			return;

		// 2nd solution: file: copy and delete
		if (this.ioFile.isFile()) {
			IOUtil.copyFile(this, toFile);
			final boolean delete = this.delete();
			if (!delete)
				throw new IllegalStateException("Problem on moving file from '"
						+ this.ioFile.getCanonicalPath() +
						"' to " + toFile.getIoFile().getCanonicalPath());
		} else if (this.ioFile.isDirectory()) {
			/* If empty, but has failed the simple renameTo(), the destination
			 * is probably on another partition (very unlikely on IOS).
			 */
			if (this.ioFile.listFiles().length == 0) {
				throw new IllegalArgumentException("Should not occure!");
			} else { // non-empty directory
				/* remark: on IOS should be only one partition available to this app.
				 * And a copy/delete operation of a non-empty directory could
				 * last very long; as in Files.move(), this should be prevented.
				 * Best solution would be to rename recursively, because renaming
				 * also only works on same partition.
				 * Assuming there is only on partition, we assume renaming never fails.
				 * Partition-Detection: Its very hard to detect, whether to files are on the same
				 * partition, in a OS independent and without java.nio.Files.
				 * So we just assume one partition because of IOS. */
				IoFileUtil.moveRecursively(this, toFile);
			}
		}
	}

	@Override
	public void copyToCopyAttributes(final File toFile) throws IOException {
		if (toFile.exists())
			throw new IOException("toFile file did already exists!");
		if (!this.ioFile.exists())
			throw new IllegalArgumentException("Source file did not exists!");
		if (this.ioFile.getCanonicalPath().equals(toFile.getCanonicalPath()))
			return; //nothing to do!

		if (this.ioFile.isFile()) {
			IOUtil.copyFile(this, toFile);
		} else {
			// java.nio.Files.copy is non-recursive, so must this implementation be!
			final boolean mkdir = toFile.mkdir();
			if (!mkdir)
				throw new IllegalStateException("Problem on moving directory from '"
						+ this.ioFile.getCanonicalPath() +
						"'! Could not create directory " + toFile.getIoFile().getCanonicalPath());
		}
	}

	@Override
	public RandomAccessFile createRandomAccessFile(final String mode) throws FileNotFoundException {
		return new RandomAccessFile(ioFile, mode);
	}

	@Override
	public URI toURI() {
		return ioFile.toURI();
	}

	@Override
	public boolean isFile() {
		return ioFile.isFile();
	}

	@Override
	public void setLastModifiedNoFollow(final long lastModified) {
		this.ioFile.setLastModified(lastModified);
	}

	@Override
	public String relativize(final File target) throws IOException {
//		return IOUtil.getRelativePath(this, target); // TODO result is different, should have a look; the first shared folder was appended.
		return IoFileRelativePathUtil.getRelativePath(this.ioFile, target.getIoFile());
	}

	@Override
	public long getUsableSpace() {
		return this.ioFile.getUsableSpace();
	}

	@Override
	public java.io.File getIoFile() {
		return ioFile;
	}

	@Override
	public boolean equals(final Object obj) {
		if ( !(obj instanceof IoFile) )
			return false;

		final IoFile ioFile = (IoFile) obj;
		return this.ioFile.equals(ioFile.ioFile);
	}

	@Override
	public int hashCode() {
		return ioFile.hashCode();
	}

	@Override
	public String toString() {
		return this.ioFile.toString();
	}

	@Override
	public File createFile(String ... children) {
		return OioFileFactory.createFile(this, children);
	}

//	private void writeObject(ObjectOutputStream out) throws IOException {
//		if (!ioFile.isAbsolute())
//			logger.warn("File is not absolute! This may cause w");
//
//		out.defaultWriteObject();
//	}
//
//	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//
//	}

}
