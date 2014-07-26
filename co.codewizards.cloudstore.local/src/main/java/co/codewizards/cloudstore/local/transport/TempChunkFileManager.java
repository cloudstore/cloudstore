package co.codewizards.cloudstore.local.transport;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.TempChunkFileDTO;
import co.codewizards.cloudstore.core.dto.jaxb.TempChunkFileDTOIO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.HashUtil;

public class TempChunkFileManager {

	private static final Logger logger = LoggerFactory.getLogger(TempChunkFileManager.class);

	private static final String TEMP_CHUNK_FILE_PREFIX = "chunk_";
	private static final String TEMP_CHUNK_FILE_DTO_FILE_SUFFIX = ".xml";

	private static final class Holder {
		static final TempChunkFileManager instance = new TempChunkFileManager();
	}

	private TempChunkFileManager() { }

	public static TempChunkFileManager getInstance() {
		return Holder.instance;
	}

	public void writeFileDataToTempChunkFile(final File destFile, final long offset, final byte[] fileData) {
		assertNotNull("destFile", destFile);
		assertNotNull("fileData", fileData);
		try {
			final File tempChunkFile = createTempChunkFile(destFile, offset);
			final File tempChunkFileDTOFile = getTempChunkFileDTOFile(tempChunkFile);

			// Delete the meta-data-file, in case we overwrite an older temp-chunk-file. This way it
			// is guaranteed, that if the meta-data-file exists, it is consistent with either
			// the temp-chunk-file or the chunk was already written into the final destination.
			deleteOrFail(tempChunkFileDTOFile);

			final FileOutputStream out = new FileOutputStream(tempChunkFile);
			try {
				out.write(fileData);
			} finally {
				out.close();
			}
			final String sha1 = sha1(fileData);
			logger.trace("writeFileDataToTempChunkFile: Wrote {} bytes with SHA1 '{}' to '{}'.", fileData.length, sha1, tempChunkFile.getAbsolutePath());
			final TempChunkFileDTO tempChunkFileDTO = createTempChunkFileDTO(offset, tempChunkFile, sha1);
			new TempChunkFileDTOIO().serialize(tempChunkFileDTO, tempChunkFileDTOFile);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteTempChunkFilesWithoutDTOFile(final Collection<TempChunkFileWithDTOFile> tempChunkFileWithDTOFiles) {
		for (final TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileWithDTOFiles) {
			final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();
			if (tempChunkFileDTOFile == null || !tempChunkFileDTOFile.exists()) {
				final File tempChunkFile = tempChunkFileWithDTOFile.getTempChunkFile();
				logger.warn("deleteTempChunkFilesWithoutDTOFile: No DTO-file for temporary chunk-file '{}'! DELETING this temporary file!", tempChunkFile.getAbsolutePath());
				deleteOrFail(tempChunkFile);
				continue;
			}
		}
	}

	public Map<Long, TempChunkFileWithDTOFile> getOffset2TempChunkFileWithDTOFile(final File destFile) {
		final File[] tempFiles = getTempDir(destFile).listFiles();
		if (tempFiles == null)
			return Collections.emptyMap();

		final String destFileName = destFile.getName();
		final Map<Long, TempChunkFileWithDTOFile> result = new TreeMap<Long, TempChunkFileWithDTOFile>();
		for (final File tempFile : tempFiles) {
			String tempFileName = tempFile.getName();
			if (!tempFileName.startsWith(TEMP_CHUNK_FILE_PREFIX))
				continue;

			final boolean dtoFile;
			if (tempFileName.endsWith(TEMP_CHUNK_FILE_DTO_FILE_SUFFIX)) {
				dtoFile = true;
				tempFileName = tempFileName.substring(0, tempFileName.length() - TEMP_CHUNK_FILE_DTO_FILE_SUFFIX.length());
			}
			else
				dtoFile = false;

			final int lastUnderscoreIndex = tempFileName.lastIndexOf('_');
			if (lastUnderscoreIndex < 0)
				throw new IllegalStateException("lastUnderscoreIndex < 0 :: tempFileName='" + tempFileName + '\'');

			final String tempFileDestFileName = tempFileName.substring(TEMP_CHUNK_FILE_PREFIX.length(), lastUnderscoreIndex);
			if (!destFileName.equals(tempFileDestFileName))
				continue;

			final String offsetStr = tempFileName.substring(lastUnderscoreIndex + 1);
			final Long offset = Long.valueOf(offsetStr, 36);
			TempChunkFileWithDTOFile tempChunkFileWithDTOFile = result.get(offset);
			if (tempChunkFileWithDTOFile == null) {
				tempChunkFileWithDTOFile = new TempChunkFileWithDTOFile();
				result.put(offset, tempChunkFileWithDTOFile);
			}
			if (dtoFile)
				tempChunkFileWithDTOFile.setTempChunkFileDTOFile(tempFile);
			else
				tempChunkFileWithDTOFile.setTempChunkFile(tempFile);
		}
		return Collections.unmodifiableMap(result);
	}

	public File getTempChunkFileDTOFile(final File file) {
		return new File(file.getParentFile(), file.getName() + TEMP_CHUNK_FILE_DTO_FILE_SUFFIX);
	}

	private String sha1(final byte[] data) {
		assertNotNull("data", data);
		try {
			final byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(data));
			return HashUtil.encodeHexStr(hash);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create the temporary file for the given {@code destFile} and {@code offset}.
	 * <p>
	 * The returned file is created, if it does not yet exist; but it is <i>not</i> overwritten,
	 * if it already exists.
	 * <p>
	 * The {@linkplain #getTempDir(File) temporary directory} in which the temporary file is located
	 * is created, if necessary. In order to prevent collisions with code trying to delete the empty
	 * temporary directory, this method and the corresponding {@link #deleteTempDirIfEmpty(File)} are
	 * both synchronized.
	 * @param destFile the destination file for which to resolve and create the temporary file.
	 * Must not be <code>null</code>.
	 * @param offset the offset (inside the final destination file and the source file) of the block to
	 * be temporarily stored in the temporary file created by this method. The temporary file will hold
	 * solely this block (thus the offset in the temporary file is 0).
	 * @return the temporary file. Never <code>null</code>. The file is already created in the file system
	 * (empty), if it did not yet exist.
	 */
	public synchronized File createTempChunkFile(final File destFile, final long offset) {
		final File tempDir = getTempDir(destFile);
		tempDir.mkdir();
		if (!tempDir.isDirectory())
			throw new IllegalStateException("Creating the directory failed (it does not exist after mkdir): " + tempDir.getAbsolutePath());

		final File tempFile = new File(tempDir, String.format("%s%s_%s",
				TEMP_CHUNK_FILE_PREFIX, destFile.getName(), Long.toString(offset, 36)));
		try {
			tempFile.createNewFile();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return tempFile;
	}

	/**
	 * Deletes the {@linkplain #getTempDir(File) temporary directory} for the given {@code destFile},
	 * if this directory is empty.
	 * <p>
	 * This method is synchronized to prevent it from colliding with {@link #createTempChunkFile(File, long)}
	 * which first creates the temporary directory and then the file in it. Without synchronisation, the
	 * newly created directory might be deleted by this method, before the temporary file in it is created.
	 * @param destFile the destination file for which to resolve and delete the temporary directory.
	 * Must not be <code>null</code>.
	 */
	public synchronized void deleteTempDirIfEmpty(final File destFile) {
		final File tempDir = getTempDir(destFile);
		tempDir.delete(); // deletes only empty directories ;-)
	}

	public File getTempDir(final File destFile) {
		assertNotNull("destFile", destFile);
		final File parentDir = destFile.getParentFile();
		return new File(parentDir, LocalRepoManager.TEMP_DIR_NAME);
	}

	/**
	 * @param offset the offset in the (real) destination file (<i>not</i> in {@code tempChunkFile}! there the offset is always 0).
	 * @param tempChunkFile the tempChunkFile containing the chunk's data. Must not be <code>null</code>.
	 * @param sha1 the sha1 of the single chunk (in {@code tempChunkFile}). Must not be <code>null</code>.
	 * @return the DTO. Never <code>null</code>.
	 */
	public TempChunkFileDTO createTempChunkFileDTO(final long offset, final File tempChunkFile, final String sha1) {
		assertNotNull("tempChunkFile", tempChunkFile);
		assertNotNull("sha1", sha1);

		if (!tempChunkFile.exists())
			throw new IllegalArgumentException("The tempChunkFile does not exist: " + tempChunkFile.getAbsolutePath());

		final FileChunkDTO fileChunkDTO = new FileChunkDTO();
		fileChunkDTO.setOffset(offset);

		final long tempChunkFileLength = tempChunkFile.length();
		if (tempChunkFileLength > Integer.MAX_VALUE)
			throw new IllegalStateException("tempChunkFile.length > Integer.MAX_VALUE");

		fileChunkDTO.setLength((int) tempChunkFileLength);
		fileChunkDTO.setSha1(sha1);

		final TempChunkFileDTO tempChunkFileDTO = new TempChunkFileDTO();
		tempChunkFileDTO.setFileChunkDTO(fileChunkDTO);
		return tempChunkFileDTO;
	}

	public void deleteTempChunkFiles(final Collection<TempChunkFileWithDTOFile> tempChunkFileWithDTOFiles) {
		for (final TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileWithDTOFiles) {
			final File tempChunkFile = tempChunkFileWithDTOFile.getTempChunkFile(); // tempChunkFile may be null!!!
			final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();

			if (tempChunkFile != null && tempChunkFile.exists())
				deleteOrFail(tempChunkFile);

			if (tempChunkFileDTOFile != null && tempChunkFileDTOFile.exists())
				deleteOrFail(tempChunkFileDTOFile);
		}
	}
}
