package co.codewizards.cloudstore.local.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jdo.FetchPlan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.CopyModificationDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.local.LocalRepoTransactionImpl;
import co.codewizards.cloudstore.local.dto.DeleteModificationDtoConverter;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.dto.RepositoryDtoConverter;
import co.codewizards.cloudstore.local.persistence.CopyModification;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepoDao;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.Modification;
import co.codewizards.cloudstore.local.persistence.ModificationDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class ChangeSetDtoBuilder {

	private static final Logger logger = LoggerFactory.getLogger(ChangeSetDtoBuilder.class);

	private final LocalRepoTransaction transaction;
	private final RepoTransport repoTransport;
	private final UUID clientRepositoryId;
	private final String pathPrefix;

	protected ChangeSetDtoBuilder(final LocalRepoTransaction transaction, final RepoTransport repoTransport) {
		this.transaction = assertNotNull("transaction", transaction);
		this.repoTransport = assertNotNull("repoTransport", repoTransport);
		this.clientRepositoryId = assertNotNull("clientRepositoryId", repoTransport.getClientRepositoryId());
		this.pathPrefix = assertNotNull("pathPrefix", repoTransport.getPathPrefix());
	}

	public static ChangeSetDtoBuilder create(final LocalRepoTransaction transaction, final RepoTransport repoTransport) {
		return createObject(ChangeSetDtoBuilder.class, transaction, repoTransport);
	}

	public ChangeSetDto buildChangeSetDto() {
		final ChangeSetDto changeSetDto = createObject(ChangeSetDto.class);
		final LocalRepositoryDao localRepositoryDao = transaction.getDao(LocalRepositoryDao.class);
		final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
		final LastSyncToRemoteRepoDao lastSyncToRemoteRepoDao = transaction.getDao(LastSyncToRemoteRepoDao.class);
		final ModificationDao modificationDao = transaction.getDao(ModificationDao.class);
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);

		// We must *first* read the LocalRepository and afterwards all changes, because this way, we don't need to lock it in the DB.
		// If we *then* read RepoFiles with a newer localRevision, it doesn't do any harm - we'll simply read them again, in the
		// next run.
		final LocalRepository localRepository = localRepositoryDao.getLocalRepositoryOrFail();
		changeSetDto.setRepositoryDto(RepositoryDtoConverter.create().toRepositoryDto(localRepository));

		final RemoteRepository toRemoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(clientRepositoryId);

		LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.getLastSyncToRemoteRepo(toRemoteRepository);
		if (lastSyncToRemoteRepo == null) {
			lastSyncToRemoteRepo = new LastSyncToRemoteRepo();
			lastSyncToRemoteRepo.setRemoteRepository(toRemoteRepository);
			lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(-1);
		}
		lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());
		lastSyncToRemoteRepoDao.makePersistent(lastSyncToRemoteRepo);

		((LocalRepoTransactionImpl)transaction).getPersistenceManager().getFetchPlan().setGroup(FetchPlan.ALL);
		final Collection<Modification> modifications = modificationDao.getModificationsAfter(toRemoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
		changeSetDto.setModificationDtos(toModificationDtos(modifications));

		if (!pathPrefix.isEmpty()) {
			final Collection<DeleteModification> deleteModifications = transaction.getDao(DeleteModificationDao.class).getDeleteModificationsForPathOrParentOfPathAfter(
					pathPrefix, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced(), toRemoteRepository);
			if (!deleteModifications.isEmpty()) { // our virtual root was deleted => create synthetic DeleteModificationDto for virtual root
				final DeleteModificationDto deleteModificationDto = new DeleteModificationDto();
				deleteModificationDto.setId(0);
				deleteModificationDto.setLocalRevision(localRepository.getRevision());
				deleteModificationDto.setPath("");
				changeSetDto.getModificationDtos().add(deleteModificationDto);
			}
		}

		final Collection<RepoFile> repoFiles = repoFileDao.getRepoFilesChangedAfterExclLastSyncFromRepositoryId(
				lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced(), clientRepositoryId);
		RepoFile pathPrefixRepoFile = null; // the virtual root for the client
		if (!pathPrefix.isEmpty()) {
			pathPrefixRepoFile = repoFileDao.getRepoFile(getLocalRepoManager().getLocalRoot(), getPathPrefixFile());
		}
		final Map<Long, RepoFileDto> id2RepoFileDto = getId2RepoFileDtoWithParents(pathPrefixRepoFile, repoFiles, transaction);
		changeSetDto.setRepoFileDtos(new ArrayList<RepoFileDto>(id2RepoFileDto.values()));

		return changeSetDto;
	}

	protected File getPathPrefixFile() {
		if (pathPrefix.isEmpty())
			return getLocalRepoManager().getLocalRoot();
		else
			return createFile(getLocalRepoManager().getLocalRoot(), pathPrefix);
	}

	protected LocalRepoManager getLocalRepoManager() {
		return transaction.getLocalRepoManager();
	}

	private List<ModificationDto> toModificationDtos(final Collection<Modification> modifications) {
		final long startTimestamp = System.currentTimeMillis();
		final List<ModificationDto> result = new ArrayList<ModificationDto>(AssertUtil.assertNotNull("modifications", modifications).size());
		for (final Modification modification : modifications) {
			final ModificationDto modificationDto = toModificationDto(modification);
			if (modificationDto != null)
				result.add(modificationDto);
		}
		logger.debug("toModificationDtos: Creating {} ModificationDtos took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);
		return result;
	}

	private ModificationDto toModificationDto(final Modification modification) {
		ModificationDto modificationDto;
		if (modification instanceof CopyModification) {
			final CopyModification copyModification = (CopyModification) modification;

			String fromPath = copyModification.getFromPath();
			String toPath = copyModification.getToPath();
			if (!isPathUnderPathPrefix(fromPath) || !isPathUnderPathPrefix(toPath))
				return null;

			fromPath = repoTransport.unprefixPath(fromPath);
			toPath = repoTransport.unprefixPath(toPath);

			final CopyModificationDto copyModificationDto = new CopyModificationDto();
			modificationDto = copyModificationDto;
			copyModificationDto.setFromPath(fromPath);
			copyModificationDto.setToPath(toPath);
		}
		else if (modification instanceof DeleteModification) {
			final DeleteModification deleteModification = (DeleteModification) modification;

			String path = deleteModification.getPath();
			if (!isPathUnderPathPrefix(path))
				return null;

			path = repoTransport.unprefixPath(path);

			modificationDto = DeleteModificationDtoConverter.create().toDeleteModificationDto(deleteModification);
			((DeleteModificationDto) modificationDto).setPath(path);
		}
		else
			throw new IllegalArgumentException("Unknown modification type: " + modification);

		modificationDto.setId(modification.getId());
		modificationDto.setLocalRevision(modification.getLocalRevision());

		return modificationDto;
	}

	private Map<Long, RepoFileDto> getId2RepoFileDtoWithParents(final RepoFile pathPrefixRepoFile, final Collection<RepoFile> repoFiles, final LocalRepoTransaction transaction) {
		AssertUtil.assertNotNull("transaction", transaction);
		AssertUtil.assertNotNull("repoFiles", repoFiles);
		RepoFileDtoConverter repoFileDtoConverter = null;
		final Map<Long, RepoFileDto> entityID2RepoFileDto = new HashMap<Long, RepoFileDto>();
		for (final RepoFile repoFile : repoFiles) {
			RepoFile rf = repoFile;
			if (rf instanceof NormalFile) {
				final NormalFile nf = (NormalFile) rf;
				if (nf.isInProgress()) {
					continue;
				}
			}

			if (pathPrefixRepoFile != null && !isDirectOrIndirectParent(pathPrefixRepoFile, rf))
				continue;

			while (rf != null) {
				RepoFileDto repoFileDto = entityID2RepoFileDto.get(rf.getId());
				if (repoFileDto == null) {
					if (repoFileDtoConverter == null)
						repoFileDtoConverter = RepoFileDtoConverter.create(transaction);

					repoFileDto = repoFileDtoConverter.toRepoFileDto(rf, 0);
					repoFileDto.setNeededAsParent(true); // initially true, but not default-value in DTO so that it is omitted in the XML, if it is false (the majority are false).
					if (pathPrefixRepoFile != null && pathPrefixRepoFile.equals(rf)) {
						repoFileDto.setParentId(null); // virtual root has no parent!
						repoFileDto.setName(""); // virtual root has no name!
					}

					entityID2RepoFileDto.put(rf.getId(), repoFileDto);
				}

				if (repoFile == rf)
					repoFileDto.setNeededAsParent(false);

				if (pathPrefixRepoFile != null && pathPrefixRepoFile.equals(rf))
					break;

				rf = rf.getParent();
			}
		}
		return entityID2RepoFileDto;
	}

	private boolean isDirectOrIndirectParent(final RepoFile parentRepoFile, final RepoFile repoFile) {
		AssertUtil.assertNotNull("parentRepoFile", parentRepoFile);
		AssertUtil.assertNotNull("repoFile", repoFile);
		RepoFile rf = repoFile;
		while (rf != null) {
			if (parentRepoFile.equals(rf))
				return true;

			rf = rf.getParent();
		}
		return false;
	}

	protected boolean isPathUnderPathPrefix(final String path) {
		assertNotNull("path", path);
		if (pathPrefix.isEmpty())
			return true;

		return path.startsWith(pathPrefix);
	}
}
