package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;

public final class ZipUtil {
	private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);

	private ZipUtil() { }

	/**
	 * Recursively zips all entries of the given zipInputFolder to
	 * a zipFile defined by zipOutputFile.
	 *
	 * @param zipOutputFile The file to write to (will be deleted if existent).
	 * @param zipInputFolder The inputFolder to zip.
	 * @throws IOException in case of an I/O error.
	 */
	public static void zipFolder(final File zipOutputFile, final File zipInputFolder)
	throws IOException
	{
		zipFolder(zipOutputFile, zipInputFolder, (ProgressMonitor) null);
	}

	/**
	 * Recursively zips all entries of the given zipInputFolder to
	 * a zipFile defined by zipOutputFile.
	 *
	 * @param zipOutputFile The file to write to (will be deleted if existent).
	 * @param zipInputFolder The inputFolder to zip.
	 * @param monitor an optional monitor for progress feedback (can be <code>null</code>).
	 * @throws IOException in case of an I/O error.
	 */
	public static void zipFolder(final File zipOutputFile, final File zipInputFolder, final ProgressMonitor monitor)
	throws IOException
	{
		zipFilesRecursively(zipOutputFile, zipInputFolder.listFiles(), zipInputFolder.getAbsoluteFile(), monitor);
	}

	/**
	 * Recursively zips all given files to a zipFile defined by zipOutputFile.
	 *
	 * @param zipOutputFile The file to write to (will be deleted if existent).
	 * @param files The files to zip (optional, defaults to all files recursively). It must not be <code>null</code>,
	 *		if <code>entryRoot</code> is <code>null</code>.
	 * @param entryRoot The root folder of all entries. Entries in subfolders will be
	 *		added relative to this. If <code>entryRoot==null</code>, all given files will be
	 *		added without any path (directly into the zip's root). <code>entryRoot</code> and <code>files</code> must not
	 *		both be <code>null</code> at the same time.
	 * @throws IOException in case of an I/O error.
	 */
	public static void zipFilesRecursively(final File zipOutputFile, final File[] files, final File entryRoot)
	throws IOException
	{
		zipFilesRecursively(zipOutputFile, files, entryRoot, (ProgressMonitor) null);
	}

	/**
	 * Recursively zips all given files to a zipFile defined by zipOutputFile.
	 *
	 * @param zipOutputFile The file to write to (will be deleted if existent).
	 * @param files The files to zip (optional, defaults to all files recursively). It must not be <code>null</code>,
	 *		if <code>entryRoot</code> is <code>null</code>.
	 * @param entryRoot The root folder of all entries. Entries in subfolders will be
	 *		added relative to this. If <code>entryRoot==null</code>, all given files will be
	 *		added without any path (directly into the zip's root). <code>entryRoot</code> and <code>files</code> must not
	 *		both be <code>null</code> at the same time.
	 * @param monitor an optional monitor for progress feedback (can be <code>null</code>).
	 * @throws IOException in case of an I/O error.
	 */
	public static void zipFilesRecursively(final File zipOutputFile, final File[] files, final File entryRoot, final ProgressMonitor monitor)
	throws IOException
	{
		final OutputStream fout = castStream(zipOutputFile.createOutputStream());
		final ZipOutputStream out = new ZipOutputStream(fout);
		try {
			zipFilesRecursively(out, zipOutputFile, files, entryRoot, monitor);
		} finally {
			out.close();
		}
	}

	/**
	 * Recursively writes all found files as entries into the given ZipOutputStream.
	 *
	 * @param out The ZipOutputStream to write to.
	 * @param zipOutputFile the output zipFile. optional. if it is null, this method cannot check whether
	 *		your current output file is located within the zipped directory tree. You must not locate
	 *		your zip-output file within the source directory, if you leave this <code>null</code>.
	 * @param files The files to zip (optional, defaults to all files recursively). It must not be <code>null</code>,
	 *		if <code>entryRoot</code> is <code>null</code>.
	 * @param entryRoot The root folder of all entries. Entries in subfolders will be
	 *		added relative to this. If <code>entryRoot==null</code>, all given files will be
	 *		added without any path (directly into the zip's root). <code>entryRoot</code> and <code>files</code> must not
	 *		both be <code>null</code> at the same time.
	 * @throws IOException in case of an I/O error.
	 */
	public static void zipFilesRecursively(final ZipOutputStream out, final File zipOutputFile, final File[] files, final File entryRoot)
	throws IOException
	{
		zipFilesRecursively(out, zipOutputFile, files, entryRoot, (ProgressMonitor) null);
	}

	/**
	 * Recursively writes all found files as entries into the given ZipOutputStream.
	 *
	 * @param out The ZipOutputStream to write to.
	 * @param zipOutputFile the output zipFile. optional. if it is null, this method cannot check whether
	 *		your current output file is located within the zipped directory tree. You must not locate
	 *		your zip-output file within the source directory, if you leave this <code>null</code>.
	 * @param files The files to zip (optional, defaults to all files recursively). It must not be <code>null</code>,
	 *		if <code>entryRoot</code> is <code>null</code>.
	 * @param entryRoot The root folder of all entries. Entries in subfolders will be
	 *		added relative to this. If <code>entryRoot==null</code>, all given files will be
	 *		added without any path (directly into the zip's root). <code>entryRoot</code> and <code>files</code> must not
	 *		both be <code>null</code> at the same time.
	 * @param monitor an optional monitor for progress feedback (can be <code>null</code>).
	 * @throws IOException in case of an I/O error.
	 */
	public static void zipFilesRecursively(final ZipOutputStream out, final File zipOutputFile, File[] files, final File entryRoot, final ProgressMonitor monitor)
	throws IOException
	{
		if (entryRoot == null && files == null)
			throw new IllegalArgumentException("entryRoot and files must not both be null!");

		if (entryRoot != null && !entryRoot.isDirectory())
			throw new IllegalArgumentException("entryRoot is not a directory: "+entryRoot.getAbsolutePath());

		if ( files == null ) {
			files = new File[] { entryRoot };
		}

		if (monitor != null) {
			int dirCount = 0;
			int fileCount = 0;
			for (final File file : files) {
				if (file.isDirectory())
					++dirCount;
				else
					++fileCount;
			}

			monitor.beginTask("Zipping files", dirCount * 10 + fileCount);
		}
		try {
			final byte[] buf = new byte[1024 * 5];
			for (final File file : files) {
				if (zipOutputFile != null && file.equals(zipOutputFile)) {
					if (monitor != null)
						monitor.worked(1);

					continue;
				}

				String relativePath = entryRoot == null ? file.getName() : getRelativePath(entryRoot, file.getAbsoluteFile());
				// The method ZipEntry.isDirectory checks for 'name.endsWith("/");' and thus seems not to take
				// File.separator into account. Furthermore, I browsed the web for source codes (both implementation and
				// usage of ZipFile/ZipEntry and it seems to always use '/' - even in Windows.
				// Thus, I assume that all backslashes should be converted to slashes here. Marco.
				relativePath = relativePath.replace('\\', '/');
				if ( file.isDirectory() ) {
					// store directory (necessary, in case the directory is empty - otherwise it's lost)
					relativePath += '/';
					final ZipEntry entry = new ZipEntry(relativePath);
					entry.setTime(file.lastModified());
					entry.setSize(0);
					entry.setCompressedSize(0);
					entry.setCrc(0);
					entry.setMethod(ZipEntry.STORED);
					out.putNextEntry(entry);
					out.closeEntry();

					// recurse
					final File[] dirFiles = file.listFiles();
					if (dirFiles == null) {
						logger.error("zipFilesRecursively: file.listFiles() returned null, even though file is a directory! file=\"{}\"", file.getAbsolutePath());
						if (monitor != null)
							monitor.worked(10);
					}
					else {
						zipFilesRecursively(
								out,
								zipOutputFile,
								dirFiles,
								entryRoot,
								monitor == null ? null : new SubProgressMonitor(monitor, 10)
						);
					}
				}
				else {
					// Create a new zipEntry
					final BufferedInputStream in = new BufferedInputStream(castStream(file.createInputStream()));
					final ZipEntry entry = new ZipEntry(relativePath);
					entry.setTime(file.lastModified());
					out.putNextEntry(entry);

					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}

					out.closeEntry();
					in.close();

					if (monitor != null)
						monitor.worked(1);
				}
			} // end of for ( int i = 0; i < files.length; i++ )
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	private static final String PROPERTY_KEY_ZIP_TIMESTAMP = "zip.timestamp";
	private static final String PROPERTY_KEY_ZIP_FILESIZE = "zip.size";

	/**
	 * Calls {@link #unzipArchiveIfModified(URL, File)} converting the File-parameter zipArchive to an url.
	 *
	 * @see #unzipArchiveIfModified(URL, File).
	 */
	public static synchronized void unzipArchiveIfModified(final File zipArchive, final File unzipRootFolder)
	throws IOException
	{
		unzipArchive(zipArchive.toURI().toURL(), unzipRootFolder);
	}

	/**
	 * Unzip the given archive into the given folder, if the archive was modified
	 * after being unzipped the last time by this method.
	 * <p>
	 * The current implementation
	 * of this method creates a file named ".archive.properties" inside the
	 * <code>unzipRootFolder</code> and stores the <code>zipArchive</code>'s file size and
	 * last-modified-timestamp to decide whether a future call to this method needs
	 * to unzip the data again.
	 * </p>
	 * <p>
	 * Note, that this method deletes the <code>unzipRootFolder</code> prior to unzipping
	 * in order to guarantee that content which was removed from the <code>zipArchive</code> is not existing
	 * in the <code>unzipRootFolder</code> anymore, too.
	 * </p>
	 * TODO instead of being synchronized, this method should use lower (= operating-system) locking mechanisms. Marco.
	 *
	 * @param zipArchive The zip file to unzip.
	 * @param unzipRootFolder The folder to unzip to.
	 * @throws IOException in case of an I/O error.
	 */
	public static synchronized void unzipArchiveIfModified(final URL zipArchive, final File unzipRootFolder)
	throws IOException
	{
		final File metaFile = createFile(unzipRootFolder, ".archive.properties");
		long timestamp = Long.MIN_VALUE;
		long fileSize = Long.MIN_VALUE;

		final Properties properties = new Properties();
		if (metaFile.exists()) {
			final InputStream in = castStream(metaFile.createInputStream());
			try {
				properties.load(in);
			} finally {
				in.close();
			}

			final String timestampS = properties.getProperty(PROPERTY_KEY_ZIP_TIMESTAMP);
			if (timestampS != null) {
				try {
					timestamp = Long.parseLong(timestampS, 36);
				} catch (final NumberFormatException x) {
					// ignore
				}
			}

			final String fileSizeS = properties.getProperty(PROPERTY_KEY_ZIP_FILESIZE);
			if (fileSizeS != null) {
				try {
					fileSize = Long.parseLong(fileSizeS, 36);
				} catch (final NumberFormatException x) {
					// ignore
				}
			}
		}

		boolean doUnzip = true;
		long zipLength = -1;
		long zipLastModified = System.currentTimeMillis(); // not using chronos, because timestamp given by file-system.

		if ("file".equals(zipArchive.getProtocol())) {
			final File fileToCheck = createFile(UrlUtil.urlToUri(zipArchive));
			zipLastModified = fileToCheck.lastModified();
			zipLength = fileToCheck.length();
			doUnzip = !unzipRootFolder.exists() || zipLastModified != timestamp || zipLength != fileSize;
		}

		if (doUnzip) {
			deleteDirectoryRecursively(unzipRootFolder);
			unzipArchive(zipArchive, unzipRootFolder);
			properties.setProperty(PROPERTY_KEY_ZIP_FILESIZE, Long.toString(zipLength, 36));
			properties.setProperty(PROPERTY_KEY_ZIP_TIMESTAMP, Long.toString(zipLastModified, 36));
			try (final OutputStream out = castStream(metaFile.createOutputStream())) {
				properties.store(out, null);
			}
		}
	}

	/**
	 * Unzip the given archive into the given folder.
	 *
	 * @param zipArchive The zip file to unzip.
	 * @param unzipRootFolder The folder to unzip to.
	 * @throws IOException in case of an I/O error.
	 */
	public static void unzipArchive(final URL zipArchive, final File unzipRootFolder)
	throws IOException
	{
		final ZipInputStream in = new ZipInputStream(zipArchive.openStream());
		try {
			ZipEntry entry = null;
			while ((entry = in.getNextEntry()) != null) {
				if(entry.isDirectory()) {
					// create the directory
					final File dir = createFile(unzipRootFolder, entry.getName());
					if (!dir.exists() && !dir.mkdirs())
						throw new IllegalStateException("Could not create directory entry, possibly permission issues.");
				}
				else {
					final File file = createFile(unzipRootFolder, entry.getName());

					final File dir = file.getParentFile();
					if (dir.exists( )) {
						assert (dir.isDirectory( ));
					}
					else {
						dir.mkdirs( );
					}

					try (final BufferedOutputStream out = new BufferedOutputStream(castStream(file.createOutputStream()))) {
						int len;
						final byte[] buf = new byte[1024 * 5];
						while( (len = in.read(buf)) > 0 )
						{
							out.write(buf, 0, len);
						}
					}
				}
			}
		} finally {
			if (in != null)
				in.close();
		}
	}

	/**
	 * Calls {@link #unzipArchive(URL, File)} converting the File-parameter zipArchive to an url.
	 *
	 * @see #unzipArchive(URL, File).
	 */
	public static void unzipArchive(final File zipArchive, final File unzipRootFolder)
	throws IOException
	{
		unzipArchive(zipArchive.toURI().toURL(), unzipRootFolder);
	}
}
