package co.codewizards.cloudstore.local.dto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.SymlinkDTO;
import co.codewizards.cloudstore.core.dto.TempChunkFileDTO;
import co.codewizards.cloudstore.core.dto.jaxb.TempChunkFileDTOIO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDAO;
import co.codewizards.cloudstore.local.persistence.Symlink;
import co.codewizards.cloudstore.local.transport.TempChunkFileManager;
import co.codewizards.cloudstore.local.transport.TempChunkFileWithDTOFile;

public class RepoFileDTOConverter {

	private static final Logger logger = LoggerFactory.getLogger(RepoFileDTOConverter.class);

	private final TempChunkFileManager tempChunkFileManager = TempChunkFileManager.getInstance();
	private final LocalRepoManager localRepoManager;
	private final LocalRepoTransaction transaction;
	private final RepoFileDAO repoFileDAO;

	public RepoFileDTOConverter(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
		this.localRepoManager = assertNotNull("transaction.localRepoManager", transaction.getLocalRepoManager());
		this.repoFileDAO = this.transaction.getDAO(RepoFileDAO.class);
	}

	public RepoFileDTO toRepoFileDTO(final RepoFile repoFile, final int depth) {
		assertNotNull("repoFileDAO", repoFileDAO);
		assertNotNull("repoFile", repoFile);
		final RepoFileDTO repoFileDTO;
		if (repoFile instanceof NormalFile) {
			final NormalFile normalFile = (NormalFile) repoFile;
			final NormalFileDTO normalFileDTO;
			repoFileDTO = normalFileDTO = new NormalFileDTO();
			normalFileDTO.setLength(normalFile.getLength());
			normalFileDTO.setSha1(normalFile.getSha1());
			if (depth > 0) {
				// TODO this should actually be a SortedSet, but for whatever reason, I started
				// getting ClassCastExceptions and had to switch to a normal Set :-(
				final List<FileChunk> fileChunks = new ArrayList<>(normalFile.getFileChunks());
				Collections.sort(fileChunks);
				for (final FileChunk fileChunk : fileChunks) {
					normalFileDTO.getFileChunkDTOs().add(toFileChunkDTO(fileChunk));
				}
			}
			if (depth > 1) {
				final TempChunkFileDTOIO tempChunkFileDTOIO = new TempChunkFileDTOIO();
				final File file = repoFile.getFile(localRepoManager.getLocalRoot());
				for (final TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileManager.getOffset2TempChunkFileWithDTOFile(file).values()) {
					final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();
					if (tempChunkFileDTOFile == null)
						continue; // incomplete: meta-data not yet written => ignore

					final TempChunkFileDTO tempChunkFileDTO;
					try {
						tempChunkFileDTO = tempChunkFileDTOIO.deserialize(tempChunkFileDTOFile);
					} catch (final Exception x) {
						logger.warn("toRepoFileDTO: Ignoring corrupt tempChunkFileDTOFile '" + tempChunkFileDTOFile.getAbsolutePath() + "': " + x, x);
						continue;
					}
					normalFileDTO.getTempFileChunkDTOs().add(assertNotNull("tempChunkFileDTO.fileChunkDTO", tempChunkFileDTO.getFileChunkDTO()));
				}
			}
		}
		else if (repoFile instanceof Directory) {
			repoFileDTO = new DirectoryDTO();
		}
		else if (repoFile instanceof Symlink) {
			final Symlink symlink = (Symlink) repoFile;
			final SymlinkDTO symlinkDTO;
			repoFileDTO = symlinkDTO = new SymlinkDTO();
			symlinkDTO.setTarget(symlink.getTarget());
		}
		else
			throw new UnsupportedOperationException("RepoFile type not yet supported: " + repoFile);

		repoFileDTO.setId(repoFile.getId());
		repoFileDTO.setLocalRevision(repoFile.getLocalRevision());
		repoFileDTO.setName(repoFile.getName());
		repoFileDTO.setParentId(repoFile.getParent() == null ? null : repoFile.getParent().getId());
		repoFileDTO.setLastModified(repoFile.getLastModified());

		return repoFileDTO;
	}

	private FileChunkDTO toFileChunkDTO(final FileChunk fileChunk) { // TODO there should be a separate FileChunkDTOConverter!
		final FileChunkDTO fileChunkDTO = new FileChunkDTO();
		fileChunkDTO.setOffset(fileChunk.getOffset());
		fileChunkDTO.setLength(fileChunk.getLength());
		fileChunkDTO.setSha1(fileChunk.getSha1());
		return fileChunkDTO;
	}
}
