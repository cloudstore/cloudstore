package co.codewizards.cloudstore.ls.rest.server.service;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.CopyModificationDao;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.DirectoryDao;
import co.codewizards.cloudstore.local.persistence.NormalFileDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequestDao;
import co.codewizards.cloudstore.ls.core.dto.RemoteRepositoryDto;
import co.codewizards.cloudstore.ls.core.dto.RemoteRepositoryRequestDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoRequestDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoResponseDto;

@Path("RepoInfo")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class RepoInfoService
{
	private static final Logger logger = LoggerFactory.getLogger(RepoInfoService.class);

	{
		logger.debug("<init>: Instance created.");
	}

	private LocalRepoTransaction transaction;
	private RepoInfoResponseDto repoInfoResponseDto;

	@POST
	public RepoInfoResponseDto run(final RepoInfoRequestDto repoInfoRequestDto)
	{
		assertNotNull("repoInfoRequestDto", repoInfoRequestDto);
		repoInfoResponseDto = new RepoInfoResponseDto();

		final File localRoot = createFile(assertNotNull("", repoInfoRequestDto.getLocalRoot()));
		final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			try ( LocalRepoTransaction transaction = localRepoManager.beginReadTransaction(); ) {
				this.transaction = transaction;

				collectMainProperties();
				collectRemoteRepositories();
				collectRemoteRepositoryRequests();
				collectRepositoryStats();

				transaction.commit();
			}
		} finally {
			localRepoManager.close();
		}

		return repoInfoResponseDto;
	}

	private void collectMainProperties() {
		final LocalRepoManager localRepoManager = transaction.getLocalRepoManager();

		repoInfoResponseDto.setRepositoryId(localRepoManager.getRepositoryId());
		repoInfoResponseDto.setLocalRoot(localRepoManager.getLocalRoot().getPath());
		repoInfoResponseDto.setPublicKey(localRepoManager.getPublicKey());

		final Collection<String> repositoryAliases = LocalRepoRegistryImpl.getInstance().getRepositoryAliasesOrFail(localRepoManager.getRepositoryId().toString());
		repoInfoResponseDto.getRepositoryAliases().addAll(repositoryAliases);
	}

	private void collectRemoteRepositories() {
		final Collection<RemoteRepository> remoteRepositories = transaction.getDao(RemoteRepositoryDao.class).getObjects();
		for (final RemoteRepository remoteRepository : remoteRepositories) {
			final RemoteRepositoryDto remoteRepositoryDto = new RemoteRepositoryDto();
			remoteRepositoryDto.setRepositoryId(remoteRepository.getRepositoryId());
			remoteRepositoryDto.setPublicKey(remoteRepository.getPublicKey());

			if (remoteRepository.getRemoteRoot() != null)
				remoteRepositoryDto.setRemoteRoot(remoteRepository.getRemoteRoot().toExternalForm());

			repoInfoResponseDto.getRemoteRepositoryDtos().add(remoteRepositoryDto);
		}
	}

	private void collectRemoteRepositoryRequests() {
		final Collection<RemoteRepositoryRequest> remoteRepositoryRequests = transaction.getDao(RemoteRepositoryRequestDao.class).getObjects();
		for (final RemoteRepositoryRequest remoteRepositoryRequest : remoteRepositoryRequests) {
			final RemoteRepositoryRequestDto remoteRepositoryRequestDto = new RemoteRepositoryRequestDto();
			remoteRepositoryRequestDto.setRepositoryId(remoteRepositoryRequest.getRepositoryId());
			remoteRepositoryRequestDto.setPublicKey(remoteRepositoryRequest.getPublicKey());
			remoteRepositoryRequestDto.setCreated(remoteRepositoryRequest.getCreated());
			remoteRepositoryRequestDto.setChanged(remoteRepositoryRequest.getChanged());

			repoInfoResponseDto.getRemoteRepositoryRequestDtos().add(remoteRepositoryRequestDto);
		}
	}

	private void collectRepositoryStats() {
		final NormalFileDao normalFileDao = transaction.getDao(NormalFileDao.class);
		final DirectoryDao directoryDao = transaction.getDao(DirectoryDao.class);
		final CopyModificationDao copyModificationDao = transaction.getDao(CopyModificationDao.class);
		final DeleteModificationDao deleteModificationDao = transaction.getDao(DeleteModificationDao.class);

		repoInfoResponseDto.setNormalFileCount(normalFileDao.getObjectsCount());
		repoInfoResponseDto.setDirectoryCount(directoryDao.getObjectsCount());
		repoInfoResponseDto.setCopyModificationCount(copyModificationDao.getObjectsCount());
		repoInfoResponseDto.setDeleteModificationCount(deleteModificationDao.getObjectsCount());
	}
}
