package co.codewizards.cloudstore.core.oio.nio;

import static java.util.Objects.*;

import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
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

import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.IoFile;
import co.codewizards.cloudstore.core.util.childprocess.DumpStreamThread;

/**
 * File object with allowed imports to the java Java 1.7 NIO2 classes and packages.
 *
 * @author Sebastian Schefczyk
 */
public class NioFile extends IoFile implements File {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(NioFile.class);


	protected NioFile(final String pathname) {
		super(pathname);
	}

	protected NioFile(final File parent, final String child) {
		super(parent, child);
	}

	protected NioFile(final String parent, final String child) {
		super(parent, child);
	}

	protected NioFile(final URI uri) {
		super(uri);
	}

	protected NioFile(final java.io.File ioFile) {
		super(ioFile);
	}


	@Override
	public File getParentFile() {
		final java.io.File parentFile = this.ioFile.getParentFile();
		return parentFile != null ? new NioFile(parentFile) : null;
	}

	@Override
	public File[] listFiles() {
		final java.io.File[] ioFilesListFiles = this.ioFile.listFiles();
		return NioFileUtil.convert(ioFilesListFiles);
	}

	@Override
	public File[] listFiles(final FileFilter fileFilter) {
		final java.io.File[] ioFilesListFiles = this.ioFile.listFiles(fileFilter);
		return NioFileUtil.convert(ioFilesListFiles);
	}

	@Override
	public File[] listFiles(final FilenameFilter fileFilter) {
		final java.io.File[] ioFilesListFiles = this.ioFile.listFiles(fileFilter);
		return NioFileUtil.convert(ioFilesListFiles);
	}

	@Override
	public File getAbsoluteFile() {
		return new NioFile(ioFile.getAbsoluteFile());
	}

	@Override
	public boolean existsNoFollow() {
		return Files.exists(ioFile.toPath(), LinkOption.NOFOLLOW_LINKS);
	}

	@Override
	public int compareTo(final File otherFile) {
		return ioFile.compareTo(otherFile.getIoFile());
	}

	@Override
	public File getCanonicalFile() throws IOException {
		return new NioFile(ioFile.getCanonicalFile());
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
	public boolean isDirectoryNoFollowSymLinks() {
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
	public long lastModified() {
		if (! exists()) // https://github.com/cloudstore/cloudstore/issues/68
			return 0;

		try {
			final BasicFileAttributes attributes = Files.readAttributes(
					ioFile.toPath(), BasicFileAttributes.class);
			return attributes.lastModifiedTime().toMillis();
		} catch (final IOException e) {
			// The file might have been deleted between the check above and the attempt to read the
			// attributes => check again and just log + exit, if it does not exist (anymore).
			if (! exists()) {
				logger.warn("lastModified: Seems, the file '{}' was deleted while we accessed it: {}", getAbsolutePath(), e);
				return 0;
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getLastModifiedNoFollow() {
		if (! existsNoFollow()) // for symmetry reasons with lastModified() -- see also https://github.com/cloudstore/cloudstore/issues/68
			return 0;

		try {
			final BasicFileAttributes attributes = Files.readAttributes(
					ioFile.toPath(), BasicFileAttributes.class,
					LinkOption.NOFOLLOW_LINKS);
			return attributes.lastModifiedTime().toMillis();
		} catch (final IOException e) {
			// The file might have been deleted between the check above and the attempt to read the
			// attributes => check again and just log + exit, if it does not exist (anymore).
			if (! existsNoFollow()) {
				logger.warn("getLastModifiedNoFollow: Seems, the file '{}' was deleted while we accessed it: {}", getAbsolutePath(), e);
				return 0;
			}
			throw new RuntimeException(e);
		}
	}

	private static String toPathString(final Path path) {
		requireNonNull(path, "path");
		return path.toString().replace(java.io.File.separatorChar, '/');
	}

	@Override
	public boolean renameTo(final File dest) {
		return ioFile.renameTo(dest.getIoFile());
	}

	@Override
	public void createSymbolicLink(final String targetPath) throws IOException {
		Files.createSymbolicLink(ioFile.toPath(), Paths.get(targetPath)).toString();
	}

	@Override
	public void move(final File toFile) throws IOException {
		Files.move(ioFile.toPath(), toFile.getIoFile().toPath());
	}

	@Override
	public void copyToCopyAttributes(final File toFile) throws IOException {
		Files.copy(ioFile.toPath(), toFile.getIoFile().toPath(), StandardCopyOption.COPY_ATTRIBUTES);
	}

	@Override
	public boolean setLastModified(long lastModified) {
		final FileTime lastModifiedTime = FileTime.fromMillis(lastModified);
		try {
			Files.getFileAttributeView(ioFile.toPath(), BasicFileAttributeView.class).setTimes(lastModifiedTime, null, null);
		} catch (final Exception e) {
			logger.error("Setting the lastModified timestamp of '"+ ioFile +"' failed with the following error: " + e, e);
			return false;
		}
		return true;
	}

	@Override
	public boolean setLastModifiedNoFollow(final long lastModified) {
		final Path path = ioFile.toPath().toAbsolutePath();
		final List<Throwable> errors = new ArrayList<>();

		final FileTime lastModifiedTime = FileTime.fromMillis(lastModified);
		try {
			Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
			.setTimes(lastModifiedTime, null, null);

			return true;
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
			final DumpStreamThread dumpInputStreamThread = new DumpStreamThread(process.getInputStream(), stdOut, logger);
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

			return true;
		} catch (IOException | InterruptedException e) {
			errors.add(e);
		}

		if (!errors.isEmpty()) {
			logger.error("Setting the lastModified timestamp of '{}' failed with the following errors:", path);
			for (final Throwable error : errors) {
				logger.error("" + error, error);
			}
		}
		return false;
	}

	@Override
	public String relativize(final File target) {
		return ioFile.getAbsoluteFile().toPath().relativize(target.getIoFile().getAbsoluteFile().toPath()).toString();
	}

}
