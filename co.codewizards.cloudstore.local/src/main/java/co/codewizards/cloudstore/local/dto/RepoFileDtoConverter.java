package co.codewizards.cloudstore.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.dto.TempChunkFileDto;
import co.codewizards.cloudstore.core.dto.jaxb.TempChunkFileDtoIo;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.persistence.Symlink;
import co.codewizards.cloudstore.local.transport.TempChunkFileManager;
import co.codewizards.cloudstore.local.transport.TempChunkFileWithDtoFile;

public class RepoFileDtoConverter {

	private static final Logger logger = LoggerFactory.getLogger(RepoFileDtoConverter.class);

	private final TempChunkFileManager tempChunkFileManager = TempChunkFileManager.getInstance();
	private final FileChunkDtoConverter fileChunkDtoConverter = FileChunkDtoConverter.create();
	private final LocalRepoManager localRepoManager;
	private final LocalRepoTransaction transaction;
	private final RepoFileDao repoFileDao;
	private boolean excludeLocalIds;
	private boolean excludeMutableData;

	public static RepoFileDtoConverter create(final LocalRepoTransaction transaction) {
		return createObject(RepoFileDtoConverter.class, transaction);
	}

	protected RepoFileDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = requireNonNull(transaction, "transaction");
		this.localRepoManager = requireNonNull(transaction.getLocalRepoManager(), "transaction.localRepoManager");
		this.repoFileDao = this.transaction.getDao(RepoFileDao.class);
	}

	public RepoFileDto toRepoFileDto(final RepoFile repoFile, final int depth) {
		requireNonNull(repoFileDao, "repoFileDao");
		requireNonNull(repoFile, "repoFile");
		final RepoFileDto repoFileDto;
		if (repoFile instanceof NormalFile) {
			final NormalFile normalFile = (NormalFile) repoFile;
			final NormalFileDto normalFileDto;
			repoFileDto = normalFileDto = createObject(NormalFileDto.class);
			if (isExcludeMutableData())
				normalFileDto.setLength(-1);
			else {
				normalFileDto.setLength(normalFile.getLength());
				normalFileDto.setSha1(normalFile.getSha1());
			}
			if (depth > 0) {
				// TODO this should actually be a SortedSet, but for whatever reason, I started
				// getting ClassCastExceptions and had to switch to a normal Set :-(
				final List<FileChunk> fileChunks = new ArrayList<>(normalFile.getFileChunks());
				Collections.sort(fileChunks);
				for (final FileChunk fileChunk : fileChunks) {
					normalFileDto.getFileChunkDtos().add(fileChunkDtoConverter.toFileChunkDto(fileChunk));
				}
			}
			if (depth > 1) {
				final TempChunkFileDtoIo tempChunkFileDtoIo = new TempChunkFileDtoIo();
				final File file = repoFile.getFile(localRepoManager.getLocalRoot());
				for (final TempChunkFileWithDtoFile tempChunkFileWithDtoFile : tempChunkFileManager.getOffset2TempChunkFileWithDtoFile(file).values()) {
					final File tempChunkFileDtoFile = tempChunkFileWithDtoFile.getTempChunkFileDtoFile();
					if (tempChunkFileDtoFile == null)
						continue; // incomplete: meta-data not yet written => ignore

					final TempChunkFileDto tempChunkFileDto;
					try {
						tempChunkFileDto = tempChunkFileDtoIo.deserialize(tempChunkFileDtoFile);
					} catch (final Exception x) {
						logger.warn("toRepoFileDto: Ignoring corrupt tempChunkFileDtoFile '" + tempChunkFileDtoFile.getAbsolutePath() + "': " + x, x);
						continue;
					}
					normalFileDto.getTempFileChunkDtos().add(requireNonNull(tempChunkFileDto.getFileChunkDto(), "tempChunkFileDto.fileChunkDto"));
				}
			}
		}
		else if (repoFile instanceof Directory) {
			repoFileDto = createObject(DirectoryDto.class);
		}
		else if (repoFile instanceof Symlink) {
			final Symlink symlink = (Symlink) repoFile;
			final SymlinkDto symlinkDto;
			repoFileDto = symlinkDto = createObject(SymlinkDto.class);
			if (! isExcludeMutableData())
				symlinkDto.setTarget(symlink.getTarget());
		}
		else
			throw new UnsupportedOperationException("RepoFile type not yet supported: " + repoFile);

		if (! isExcludeLocalIds()) {
			repoFileDto.setId(repoFile.getId());
			repoFileDto.setParentId(repoFile.getParent() == null ? null : repoFile.getParent().getId());
			repoFileDto.setLocalRevision(repoFile.getLocalRevision());
		}
		repoFileDto.setName(repoFile.getName());
		repoFileDto.setLastModified(repoFile.getLastModified());

		return repoFileDto;
	}

	public boolean isExcludeLocalIds() {
		return excludeLocalIds;
	}
	public void setExcludeLocalIds(final boolean excludeLocalIds) {
		this.excludeLocalIds = excludeLocalIds;
	}

	public boolean isExcludeMutableData() {
		return excludeMutableData;
	}
	public void setExcludeMutableData(boolean excludeMutableData) {
		this.excludeMutableData = excludeMutableData;
	}
}
