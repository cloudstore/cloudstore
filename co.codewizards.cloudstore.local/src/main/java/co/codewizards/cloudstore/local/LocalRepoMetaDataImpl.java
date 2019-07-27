package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.List;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoMetaData;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class LocalRepoMetaDataImpl implements LocalRepoMetaData {

	private LocalRepoManager localRepoManager;

	/**
	 * Gets the {@link LocalRepoManager}.
	 * <p>
	 * <b>Important:</b> This must not be exposed! It is the real internal single instance - not the proxy!
	 * @return the {@link LocalRepoManager}. Never <code>null</code> in normal operation.
	 */
	protected LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}
	protected LocalRepoManager getLocalRepoManagerOrFail() {
		return requireNonNull(localRepoManager, "localRepoManager");
	}
	protected void setLocalRepoManager(LocalRepoManager localRepoManager) {
		this.localRepoManager = localRepoManager;
	}

	/**
	 * Begin a JDO transaction for read operations only in the underlying database.
	 * @return the transaction handle. Never <code>null</code>.
	 */
	protected LocalRepoTransaction beginReadTransaction() {
		return getLocalRepoManagerOrFail().beginReadTransaction();
	}

	/**
	 * Begin a JDO transaction for read and write operations in the underlying database.
	 * @return the transaction handle. Never <code>null</code>.
	 */
	protected LocalRepoTransaction beginWriteTransaction() {
		return getLocalRepoManagerOrFail().beginWriteTransaction();
	}

	@Override
	public RepoFileDto getRepoFileDto(final String path, final int depth) {
		requireNonNull(path, "path");

		final RepoFileDto result;
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final RepoFileDtoConverter converter = RepoFileDtoConverter.create(tx);

			final File localRoot = getLocalRepoManagerOrFail().getLocalRoot();
			final File file = createFile(localRoot, path);
			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getRepoFile(localRoot, file);

			if (repoFile == null)
				result = null;
			else
				result = converter.toRepoFileDto(repoFile, depth);

			tx.commit();
		}
		return result;
	}

	@Override
	public RepoFileDto getRepoFileDto(long repoFileId, int depth) {
		final RepoFileDto result;
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final RepoFileDtoConverter converter = RepoFileDtoConverter.create(tx);

			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getObjectByIdOrNull(repoFileId);

			if (repoFile == null)
				result = null;
			else
				result = converter.toRepoFileDto(repoFile, depth);

			tx.commit();
		}
		return result;
	}

	@Override
	public List<RepoFileDto> getChildRepoFileDtos(final long repoFileId, final int depth) {
		final List<RepoFileDto> result;
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getObjectByIdOrNull(repoFileId);
			result = getChildRepoFileDtos(tx, repoFile, depth);

			tx.commit();
		}
		return result;
	}

	@Override
	public List<RepoFileDto> getChildRepoFileDtos(final String path, final int depth) {
		final List<RepoFileDto> result;
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final File localRoot = getLocalRepoManagerOrFail().getLocalRoot();
			final File file = createFile(localRoot, path);
			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getRepoFile(localRoot, file);
			result = getChildRepoFileDtos(tx, repoFile, depth);

			tx.commit();
		}
		return result;
	}

	private List<RepoFileDto> getChildRepoFileDtos(final LocalRepoTransaction tx, final RepoFile repoFile, final int depth) {
		if (repoFile == null)
			return null;
		else {
			final RepoFileDao repoFileDao = tx.getDao(RepoFileDao.class);
			final RepoFileDtoConverter converter = RepoFileDtoConverter.create(tx);

			final List<RepoFileDto> result = new ArrayList<>();
			for (final RepoFile childRepoFile : repoFileDao.getChildRepoFiles(repoFile)) {
				final RepoFileDto dto = converter.toRepoFileDto(childRepoFile, depth);
				result.add(dto);
			}
			return result;
		}
	}
}
