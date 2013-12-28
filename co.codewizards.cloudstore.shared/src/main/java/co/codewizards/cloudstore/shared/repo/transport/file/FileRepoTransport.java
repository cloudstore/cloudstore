package co.codewizards.cloudstore.shared.repo.transport.file;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.codewizards.cloudstore.shared.dto.DirectoryDTO;
import co.codewizards.cloudstore.shared.dto.EntityID;
import co.codewizards.cloudstore.shared.dto.NormalFileDTO;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;
import co.codewizards.cloudstore.shared.dto.RepoFileDTOList;
import co.codewizards.cloudstore.shared.persistence.Directory;
import co.codewizards.cloudstore.shared.persistence.NormalFile;
import co.codewizards.cloudstore.shared.persistence.RepoFile;
import co.codewizards.cloudstore.shared.persistence.RepoFileDAO;
import co.codewizards.cloudstore.shared.repo.local.RepositoryManager;
import co.codewizards.cloudstore.shared.repo.local.RepositoryManagerRegistry;
import co.codewizards.cloudstore.shared.repo.local.RepositoryTransaction;
import co.codewizards.cloudstore.shared.repo.transport.AbstractRepoTransport;

public class FileRepoTransport extends AbstractRepoTransport {

	private RepositoryManager repositoryManager;

	protected RepositoryManager getRepositoryManager() {
		if (repositoryManager == null) {
			File remoteRootFile;
			try {
				remoteRootFile = new File(getRemoteRoot().toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			repositoryManager = RepositoryManagerRegistry.getInstance().getRepositoryManager(remoteRootFile);
		}
		return repositoryManager;
	}

	@Override
	public RepoFileDTOList getRepoFileDTOsChangedAfter(long localRevision) {
		RepoFileDTOList result = new RepoFileDTOList();
		RepositoryTransaction transaction = repositoryManager.createTransaction();
		try {
			RepoFileDAO repoFileDAO = new RepoFileDAO().persistenceManager(transaction.getPersistenceManager());
			Collection<RepoFile> repoFiles = repoFileDAO.getRepoFilesChangedAfter(localRevision);
			Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = getEntityID2RepoFileDTOWithParents(repoFileDAO, repoFiles);
			result.setElements(new ArrayList<RepoFileDTO>(entityID2RepoFileDTO.values()));
			transaction.commit();
			return result;
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private Map<EntityID, RepoFileDTO> getEntityID2RepoFileDTOWithParents(RepoFileDAO repoFileDAO, Collection<RepoFile> repoFiles) {
		Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = new HashMap<EntityID, RepoFileDTO>();
		for (RepoFile repoFile : repoFiles) {
			RepoFile rf = repoFile;
			while (rf != null) {
				if (!entityID2RepoFileDTO.containsKey(rf.getEntityID())) {
					entityID2RepoFileDTO.put(rf.getEntityID(), toRepoFileDTO(repoFileDAO, rf));
				}
				rf = rf.getParent();
			}
		}
		return entityID2RepoFileDTO;
	}

	private RepoFileDTO toRepoFileDTO(RepoFileDAO repoFileDAO, RepoFile repoFile) {
		RepoFileDTO repoFileDTO;
		if (repoFile instanceof NormalFile) {
			NormalFile normalFile = (NormalFile) repoFile;
			NormalFileDTO normalFileDTO;
			repoFileDTO = normalFileDTO = new NormalFileDTO();
			normalFileDTO.setLastModified(normalFile.getLastModified());
			normalFileDTO.setLength(normalFile.getLength());
			normalFileDTO.setSha1(normalFile.getSha1());
		}
		else if (repoFile instanceof Directory) {
			DirectoryDTO directoryDTO;
			repoFileDTO = directoryDTO = new DirectoryDTO();
			Collection<RepoFile> childRepoFiles = repoFileDAO.getChildRepoFiles(repoFile);
			List<String> childNames = directoryDTO.getChildNames();
			for (RepoFile childRepoFile : childRepoFiles) {
				childNames.add(childRepoFile.getName());
			}
		}
		else // TODO support symlinks!
			throw new UnsupportedOperationException("RepoFile type not yet supported: " + repoFile);

		repoFileDTO.setEntityID(repoFile.getEntityID());
		repoFileDTO.setLocalRevision(repoFile.getLocalRevision());
		repoFileDTO.setName(repoFile.getName());
		repoFileDTO.setParentEntityID(repoFile.getParent() == null ? null : repoFile.getParent().getEntityID());

		return repoFileDTO;
	}
}
