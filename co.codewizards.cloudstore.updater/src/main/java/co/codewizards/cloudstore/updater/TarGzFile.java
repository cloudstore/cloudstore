package co.codewizards.cloudstore.updater;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class TarGzFile {
	private static final Logger logger = LoggerFactory.getLogger(TarGzFile.class);

	private final File tarGzFile;
	private TarGzEntryNameConverter tarGzEntryNameConverter;
	private FileFilter fileFilter;

	public TarGzFile(final File tarGzFile) {
		this.tarGzFile = AssertUtil.assertNotNull(tarGzFile, "tarGzFile");
	}

	/**
	 * Gets the {@link FileFilter} deciding whether to process a file or not.
	 * @return the {@link FileFilter} deciding whether to process a file or not. May be <code>null</code>.
	 * If there is none, all files are processed.
	 */
	public FileFilter getFileFilter() {
		return fileFilter;
	}
	public void setFileFilter(final FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}
	public TarGzFile fileFilter(final FileFilter fileFilter) {
		setFileFilter(fileFilter);
		return this;
	}

	public TarGzEntryNameConverter getTarGzEntryNameConverter() {
		return tarGzEntryNameConverter;
	}
	public void setTarGzEntryNameConverter(final TarGzEntryNameConverter tarGzEntryNameConverter) {
		this.tarGzEntryNameConverter = tarGzEntryNameConverter;
	}
	public TarGzFile tarGzEntryNameConverter(final TarGzEntryNameConverter tarGzEntryNameConverter) {
		setTarGzEntryNameConverter(tarGzEntryNameConverter);
		return this;
	}

	public void compress(final File rootDir) throws IOException {
		boolean deleteIncompleteTarGzFile = false;
		final OutputStream fout = castStream(tarGzFile.createOutputStream());
		try {
			deleteIncompleteTarGzFile = true;

			final GzipParameters gzipParameters = new GzipParameters();
			gzipParameters.setCompressionLevel(Deflater.BEST_COMPRESSION);
			final TarArchiveOutputStream out = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(fout), gzipParameters));
			try {
				writeTar(out, rootDir, rootDir);
			} finally {
				out.close();
			}
			deleteIncompleteTarGzFile = false;
		} finally {
			fout.close();
			if (deleteIncompleteTarGzFile)
				tarGzFile.delete();
		}
	}

	private static final TarGzEntryNameConverter defaultEntryNameConverter = new DefaultTarGzEntryNameConverter();

	private void writeTar(final TarArchiveOutputStream out, final File rootDir, final File dir) throws IOException {
		final TarGzEntryNameConverter tarGzEntryNameConverter = this.tarGzEntryNameConverter == null ? defaultEntryNameConverter : this.tarGzEntryNameConverter;
		try {
			final File[] children = dir.listFiles(fileFilter);
			if (children != null) {
				for (final File child : children) {
					final String entryName = tarGzEntryNameConverter.getEntryName(rootDir, child);
					final TarArchiveEntry archiveEntry = (TarArchiveEntry) out.createArchiveEntry(child.getIoFile(), entryName);

					if (child.canExecute())
						archiveEntry.setMode(archiveEntry.getMode() | 0111);

					out.putArchiveEntry(archiveEntry);
					try {
						if (child.isFile()) {
							final InputStream in = castStream(child.createInputStream());
							try {
								transferStreamData(in, out);
							} finally {
								in.close();
							}
						}
					} finally {
						out.closeArchiveEntry();
					}

					if (child.isDirectory())
						writeTar(out, rootDir, child);
				}
			}
		} catch (IOException | RuntimeException x) {
			logger.error(x.toString(), x);
			throw x;
		}
	}

	public void extract(final File rootDir) throws IOException {
		rootDir.mkdirs();
		final TarGzEntryNameConverter tarGzEntryNameConverter = this.tarGzEntryNameConverter == null ? defaultEntryNameConverter : this.tarGzEntryNameConverter;
		final FileFilter fileFilter = this.fileFilter;
		final InputStream fin = castStream(tarGzFile.createInputStream());
		try {
			final TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(fin)));
			try {
				TarArchiveEntry entry;
				while (null != (entry = in.getNextTarEntry())) {
					if(entry.isDirectory()) {
						// create the directory
						final File dir = tarGzEntryNameConverter.getFile(rootDir, entry.getName());
						if (fileFilter == null || fileFilter.accept(dir.getIoFile())) {
							if (!dir.exists() && !dir.mkdirs())
								throw new IllegalStateException("Could not create directory entry, possibly permission issues: " + dir.getAbsolutePath());
						}
					}
					else {
						final File file = tarGzEntryNameConverter.getFile(rootDir, entry.getName());
						if (fileFilter == null || fileFilter.accept(file.getIoFile())) {
							final File dir = file.getParentFile();
							if (!dir.isDirectory())
								dir.mkdirs();

							// If the file already exists, we delete it and write into a new one - if possible.
							// This has the advantage (in GNU/Linux)
							if (file.isFile())
								file.delete();

							final OutputStream out = castStream(file.createOutputStream());
							try {
								transferStreamData(in, out);
							} finally {
								out.close();
							}

							if ((entry.getMode() & 0100) != 0 || (entry.getMode() & 010) != 0 || (entry.getMode() & 01) != 0)
								file.setExecutable(true, false);
						}
					}
				}
			} finally {
				in.close();
			}
		} finally {
			fin.close();
		}
	}

	private void transferStreamData(final InputStream in, final OutputStream out) throws IOException {
		int len;
		final byte[] buf = new byte[1024 * 16];
		while( (len = in.read(buf)) > 0 ) {
			if (len > 0)
				out.write(buf, 0, len);
		}
	}

}
