package co.codewizards.cloudstore.local.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jdo.FetchPlan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
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

	/**
	 * The path-prefix of the opposite side.
	 * <p>
	 * For example, when we are building the {@code ChangeSetDto} on the server-side, then this is
	 * the prefix used by the client. Thus, let's assume that the client has checked-out the
	 * sub-directory "/documents", then this is the sub-directory on the server-side inside the server's
	 * root-directory.
	 * <p>
	 * If, in this same scenario, the {@code ChangeSetDto} is built on the client-side, then this
	 * is an empty string.
	 */
	private final String pathPrefix;

	private LocalRepository localRepository;
	private RemoteRepository remoteRepository;
	private LastSyncToRemoteRepo lastSyncToRemoteRepo;
	private Collection<Modification> modifications;

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
		logger.trace(">>> buildChangeSetDto >>>");

		localRepository = null; remoteRepository = null;
		lastSyncToRemoteRepo = null; modifications = null;

		final ChangeSetDto changeSetDto = createObject(ChangeSetDto.class);

		final LocalRepositoryDao localRepositoryDao = transaction.getDao(LocalRepositoryDao.class);
		final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
		final ModificationDao modificationDao = transaction.getDao(ModificationDao.class);
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);

		localRepository = localRepositoryDao.getLocalRepositoryOrFail();
		remoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(clientRepositoryId);

		logger.trace("localRepositoryId: {}", localRepository.getRepositoryId());
		logger.trace("remoteRepositoryId: {}", remoteRepository.getRepositoryId());
//		logger.trace("remoteRepository.localPathPrefix: {}", remoteRepository.getLocalPathPrefix()); // same as pathPrefix
		logger.trace("pathPrefix: {}", pathPrefix);

		changeSetDto.setRepositoryDto(RepositoryDtoConverter.create().toRepositoryDto(localRepository));

		prepareLastSyncToRemoteRepo();
		logger.info("buildChangeSetDto: localRepositoryId={} remoteRepositoryId={} localRepositoryRevisionSynced={} localRepositoryRevisionInProgress={}",
				localRepository.getRepositoryId(), remoteRepository.getRepositoryId(),
				lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced(),
				lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress());

		((LocalRepoTransactionImpl)transaction).getPersistenceManager().getFetchPlan().setGroup(FetchPlan.ALL);
		modifications = modificationDao.getModificationsAfter(remoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
		changeSetDto.setModificationDtos(toModificationDtos(modifications));

		if (!pathPrefix.isEmpty()) {
			final Collection<DeleteModification> deleteModifications = transaction.getDao(DeleteModificationDao.class).getDeleteModificationsForPathOrParentOfPathAfter(
					pathPrefix, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced(), remoteRepository);
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

		changeSetDto.setParentConfigPropSetDto(buildParentConfigPropSetDto());
		logger.trace("<<< buildChangeSetDto <<<");
		return changeSetDto;
	}

	protected void prepareLastSyncToRemoteRepo() {
		final LastSyncToRemoteRepoDao lastSyncToRemoteRepoDao = transaction.getDao(LastSyncToRemoteRepoDao.class);
		lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.getLastSyncToRemoteRepo(remoteRepository);
		if (lastSyncToRemoteRepo == null) {
			lastSyncToRemoteRepo = new LastSyncToRemoteRepo();
			lastSyncToRemoteRepo.setRemoteRepository(remoteRepository);
			lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(-1);
		}
		lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());
		lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.makePersistent(lastSyncToRemoteRepo);
	}

	/**
	 * @return the {@code ConfigPropSetDto} for the parent configs or <code>null</code>, if no sync needed.
	 */
	protected ConfigPropSetDto buildParentConfigPropSetDto() {
		logger.trace(">>> buildConfigPropSetDto >>>");
		if (pathPrefix.isEmpty()) {
			logger.debug("buildConfigPropSetDto: pathPrefix is empty => returning null.");
			logger.trace("<<< buildConfigPropSetDto <<< null");
			return null;
		}

		final List<File> configFiles = getExistingConfigFilesAbovePathPrefix();
		if (! isFileModifiedAfterLastSync(configFiles) && ! isConfigFileDeletedAfterLastSync()) {
			logger.trace("<<< buildConfigPropSetDto <<< null");
			return null;
		}

		final Properties properties = new Properties();
		for (final File configFile : configFiles) {
			try {
				try (InputStream in = configFile.createInputStream()) {
					properties.load(in); // overwrites entries with same key
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		final ConfigPropSetDto result = new ConfigPropSetDto(properties);

		logger.trace("<<< buildConfigPropSetDto <<< {}", result);
		return result;
	}

	private boolean isConfigFileDeletedAfterLastSync() {
		final String searchSuffix = "/" + Config.PROPERTIES_FILE_NAME_FOR_DIRECTORY;
		for (final Modification modification : assertNotNull("modifications", modifications)) {
			if (modification instanceof DeleteModification) {
				final DeleteModification deleteModification = (DeleteModification) modification;
				if (deleteModification.getPath().endsWith(searchSuffix)) {
					logger.trace("isConfigFileDeletedAfterLastSync: returning true, because of deletion: {}", deleteModification.getPath());
					return true;
				}
			}
		}
		logger.trace("isConfigFileDeletedAfterLastSync: returning false");
		return false;
	}

	protected List<File> getExistingConfigFilesAbovePathPrefix() {
		final ArrayList<File> result = new ArrayList<>();
		final File localRoot = transaction.getLocalRepoManager().getLocalRoot();

		File dir = getPathPrefixFile();
		while (! localRoot.equals(dir)) {
			dir = assertNotNull("dir.parentFile [dir=" + dir + "]", dir.getParentFile());
			File configFile = dir.createFile(Config.PROPERTIES_FILE_NAME_FOR_DIRECTORY);
			if (configFile.isFile()) {
				result.add(configFile);
				logger.trace("getExistingConfigFilesAbovePathPrefix: enlisted configFile: {}", configFile);
			}
			else
				logger.trace("getExistingConfigFilesAbovePathPrefix: skipped non-existing configFile: {}", configFile);
		}

		// Highly unlikely, but maybe another client is connected to an already path-prefixed repository
		// in a cascaded setup.
		final File metaDir = localRoot.createFile(LocalRepoManager.META_DIR_NAME);
		final File parentConfigFile = metaDir.createFile(Config.PROPERTIES_FILE_NAME_PARENT);
		if (parentConfigFile.isFile()) {
			result.add(parentConfigFile);
			logger.trace("getExistingConfigFilesAbovePathPrefix: enlisted configFile: {}", parentConfigFile);
		}
		else
			logger.trace("getExistingConfigFilesAbovePathPrefix: skipped non-existing configFile: {}", parentConfigFile);

		Collections.reverse(result); // must be sorted according to inheritance hierarchy with following file overriding previous file
		return result;
	}

	protected boolean isFileModifiedAfterLastSync(final Collection<File> files) {
		assertNotNull("files", files);
		assertNotNull("lastSyncToRemoteRepo", lastSyncToRemoteRepo);

		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final File localRoot = transaction.getLocalRepoManager().getLocalRoot();
		for (final File file : files) {
			RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
			if (repoFile == null) {
				logger.warn("isFileModifiedAfterLastSync: RepoFile not found for (assuming it is new): {}", file);
				return true;
			}
			if (repoFile.getLocalRevision() > lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced()) {
				logger.trace("isFileModifiedAfterLastSync: file modified: {}", file);
				return true;
			}
		}
		logger.trace("isFileModifiedAfterLastSync: returning false");
		return false;
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
