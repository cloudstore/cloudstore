package co.codewizards.cloudstore.oio.nio;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayOutputStream;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.childprocess.DumpStreamThread;
import co.codewizards.cloudstore.core.oio.file.File;

/**
 * File object with allowed imports to "java.nio.*".
 *
 * @author Sebastian Schefczyk
 */
public class NioFile implements File {

	private static final Logger LOGGER = LoggerFactory.getLogger(NioFile.class);

	private final java.io.File ioFile;

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public File createNewFile(final String pathname) {
		return new NioFile(pathname);
	}

	@Override
	public File createNewFile(final String parent, final String child) {
		return new NioFile(parent, child);
	}

	@Override
	public File createNewFile(final File parent, final String child) {
		return new NioFile(parent, child);
	}

	@Override
	public File createNewFile(final java.io.File file) {
		return new NioFile(file);
	}

	@Override
	public File createNewFile(final URI uri) {
		return new NioFile(uri);
	}

	private NioFile(final String pathname) {
		this.ioFile = new java.io.File(pathname);
	}

	private NioFile(final File parent, final String child) {
		final java.io.File ioParent = castOrFail(parent).ioFile;
		this.ioFile = new java.io.File(ioParent, child);
	}

	private NioFile(final String parent, final String child) {
		this.ioFile = new java.io.File(parent, child);
	}

	private NioFile(final URI uri) {
		this.ioFile = new java.io.File(uri);
	}

	private NioFile(final java.io.File ioFile) {
		this.ioFile = ioFile;
	}

	@Override
	public File getParentFile() {
		return new NioFile(this.ioFile.getParentFile());
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
		final java.io.File[] ioFilesListFiles = this.ioFile.listFiles();
		return convert(ioFilesListFiles);
	}

	@Override
	public File[] listFiles(final FileFilter fileFilter) {
		final java.io.File[] ioFilesListFiles = this.ioFile
				.listFiles(fileFilter);
		return convert(ioFilesListFiles);
	}

	@Override
	public File[] listFiles(final FilenameFilter fileFilter) {
		final java.io.File[] ioFilesListFiles = this.ioFile
				.listFiles(fileFilter);
		return convert(ioFilesListFiles);
	}

	@Override
	public File getAbsoluteFile() {
		return new NioFile(ioFile.getAbsoluteFile());
	}

	@Override
	public boolean exists() {
		return ioFile.exists();
	}

	@Override
	public boolean existsNoFollow() {
		return Files.exists(ioFile.toPath(), LinkOption.NOFOLLOW_LINKS);
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
	public int compareTo(final File otherFile) {
		return ioFile.compareTo(castOrFail(otherFile).ioFile);
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
	public String getAbsolutePath() {
		return ioFile.getAbsolutePath();
	}

	@Override
	public File getCanonicalFile() throws IOException {
		return new NioFile(ioFile.getCanonicalFile());
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
		return Files.isRegularFile(ioFile.toPath(), LinkOption.NOFOLLOW_LINKS);
	}

	@Override
	public boolean isRegularFileFollowLinks() {
		return Files.isRegularFile(ioFile.toPath());
	}

	@Override
	public boolean isDirectoryFileNoFollowSymLinks() {
		return Files.isDirectory(ioFile.toPath(), LinkOption.NOFOLLOW_LINKS);
	}

	@Override
	public boolean isDirectoryFollowSymLinks() {
		return Files.isDirectory(ioFile.toPath());
	}

	@Override
	public boolean isSymbolicLink() {
		return Files.isSymbolicLink(ioFile.toPath());
	}

	@Override
	public String readSymbolicLinkToPathString() throws IOException {
		final Path symlinkPath = ioFile.toPath();
		final Path currentTargetPath = Files.readSymbolicLink(symlinkPath);
		final String currentTarget = toPathString(currentTargetPath);
		return currentTarget;
	}

	@Override
	public long getLastModifiedNoFollow() {
		try {
			final BasicFileAttributes attributes = Files.readAttributes(
					ioFile.toPath(), BasicFileAttributes.class,
					LinkOption.NOFOLLOW_LINKS);
			return attributes.lastModifiedTime().toMillis();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static NioFile castOrFail(final File file) {
		if (file instanceof NioFile)
			return (NioFile) file;
		else
			throw new IllegalArgumentException("Could not cast file: "
					+ file.getClass().getCanonicalName());
	}

	private static String toPathString(final Path path) {
		assertNotNull("path", path);
		return path.toString().replace(java.io.File.separatorChar, '/');
	}

	private String toPathString() {
		final Path path = ioFile.toPath();
		assertNotNull("path", path);
		return path.toString().replace(java.io.File.separatorChar, '/');
	}

	private File[] convert(final java.io.File[] ioFilesListFiles) {
		final File[] listFiles = new NioFile[ioFilesListFiles.length];
		for (int i = 0; i < ioFilesListFiles.length; i++) {
			listFiles[i] = new NioFile(ioFilesListFiles[i]);
		}
		return listFiles;
	}

	@Override
	public boolean renameTo(final File dest) {
		return ioFile.renameTo(castOrFail(dest).ioFile);
	}

	@Override
	public void setLastModified(final long lastModified) {
		ioFile.setLastModified(lastModified);
	}

	@Override
	public boolean setExecutable(final boolean executable, final boolean ownerOnly) {
		return ioFile.setExecutable(executable, ownerOnly);
	}

	@Override
	public OutputStream createFileOutputStream() throws FileNotFoundException {
		return new FileOutputStream(ioFile);
	}

	@Override
	public InputStream createFileInputStream() throws FileNotFoundException {
		return new FileInputStream(ioFile);
	}

	@Override
	public OutputStream createFileOutputStream(final boolean append) throws FileNotFoundException {
		return new FileOutputStream(ioFile, append);
	}

	@Override
	public String getName() {
		return ioFile.getName();
	}

	@Override
	public String createSymbolicLink(final String targetPath) throws IOException {
		return Files.createSymbolicLink(ioFile.toPath(), Paths.get(targetPath)).toString();
	}

	@Override
	public long lastModified() {
		return ioFile.lastModified();
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
		Files.move(ioFile.toPath(), castOrFail(toFile).ioFile.toPath());
	}

	@Override
	public void copyToCopyAttributes(final File toFile) throws IOException {
		Files.copy(ioFile.toPath(), castOrFail(toFile).ioFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
	}

	@Override
	public RandomAccessFile createRandomAccessFile(final String mode) throws FileNotFoundException {
		return new RandomAccessFile(ioFile, "rw");
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
		final Path path = ioFile.toPath().toAbsolutePath();
		final List<Throwable> errors = new ArrayList<>();

		final FileTime lastModifiedTime = FileTime.fromMillis(lastModified);
		try {
			Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
			.setTimes(lastModifiedTime, null, null);

			return;
		} catch (final IOException e) {
			errors.add(e);
		}

		// It's currently impossible to modify the 'lastModified' timestamp of a symlink :-(
		// http://stackoverflow.com/questions/17308363/symlink-lastmodifiedtime-in-java-1-7
		// Therefore, we fall back to the touch command, if the above code failed.

		final String timestamp = new SimpleDateFormat("YYYYMMddHHmm.ss").format(new Date(lastModified));
		final ProcessBuilder processBuilder = new ProcessBuilder("touch", "-c", "-h", "-m", "-t", timestamp, path.toString());
		processBuilder.redirectErrorStream(true);
		try {
			final Process process = processBuilder.start();
			final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
			final int processExitCode;
			final DumpStreamThread dumpInputStreamThread = new DumpStreamThread(process.getInputStream(), stdOut, LOGGER);
			try {
				dumpInputStreamThread.start();
				processExitCode = process.waitFor();
			} finally {
				dumpInputStreamThread.flushBuffer();
				dumpInputStreamThread.interrupt();
			}

			if (processExitCode != 0) {
				final String stdOutString = new String(stdOut.toByteArray());
				throw new IOException(String.format(
						"Command 'touch' failed with exitCode=%s and the following message: %s",
						processExitCode, stdOutString));
			}

			return;
		} catch (IOException | InterruptedException e) {
			errors.add(e);
		}

		if (!errors.isEmpty()) {
			LOGGER.error("Setting the lastModified timestamp of '{}' failed with the following errors:", path);
			for (final Throwable error : errors) {
				LOGGER.error("" + error, error);
			}
		}
	}

	@Override
	public String relativize(final File target) {
		return ioFile.toPath().relativize(castOrFail(target).ioFile.toPath()).toString();
	}

	@Override
	public File createTempDirectory(final String prefix) throws IOException {
		return new NioFile(Files.createTempDirectory(prefix).toFile());
	}

	@Override
	public File createTempFile(final String prefix, final String suffix) throws IOException {
		return new NioFile(java.io.File.createTempFile (prefix, suffix));
	}

	@Override
	public File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return new NioFile(java.io.File.createTempFile (prefix, suffix, castOrFail(dir).ioFile));
	}

	@Override
	public long getUsableSpace() {
		return this.ioFile.getUsableSpace();
	}

	@Override
	public java.io.File getIoFile() {
		return ioFile;
	}

}
