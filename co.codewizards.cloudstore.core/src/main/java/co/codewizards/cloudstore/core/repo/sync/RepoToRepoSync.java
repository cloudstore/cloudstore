package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.FileChunkSetDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOTreeNode;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;

/**
 * Logic for synchronising a local with a remote repository.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RepoToRepoSync {
	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSync.class);

	/**
	 * Sync in the inverse direction. This is only for testing whether the RepoTransport implementations
	 * are truly symmetric. It is less efficient! Therefore, this must NEVER be true in production!!!
	 */
	private static final boolean TEST_INVERSE = false;

	private final File localRoot;
	private final URL remoteRoot;
	private final LocalRepoManager localRepoManager;
	private final RepoTransport localRepoTransport;
	private final RepoTransport remoteRepoTransport;
	private final EntityID localRepositoryID;
	private final EntityID remoteRepositoryID;

	/**
	 * Create an instance.
	 * @param localRoot the root of the local repository or any file/directory inside it. This is
	 * automatically adjusted to fit the connection-point to the remote repository (the remote
	 * repository might be connected to a sub-directory).
	 * @param remoteRoot the root of the remote repository. This must exactly match the connection point.
	 * If a sub-directory of the remote repository is connected to the local repository, this sub-directory
	 * must be referenced here.
	 */
	public RepoToRepoSync(File localRoot, URL remoteRoot) {
		File localRootWithoutPathPrefix = LocalRepoHelper.getLocalRootContainingFile(assertNotNull("localRoot", localRoot));
		this.remoteRoot = UrlUtil.canonicalizeURL(assertNotNull("remoteRoot", remoteRoot));
		localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRootWithoutPathPrefix);
		this.localRoot = localRoot = new File(localRoot, getLocalPathPrefix(remoteRoot));

		localRepositoryID = localRepoManager.getRepositoryID();
		if (localRepositoryID == null)
			throw new IllegalStateException("localRepoManager.getRepositoryID() returned null!");

		remoteRepositoryID = readRemoteRepositoryIDFromLocalDB();

		remoteRepoTransport = createRepoTransport(remoteRoot, localRepositoryID);
		localRepoTransport = createRepoTransport(localRoot, remoteRepositoryID);
	}

	private String getLocalPathPrefix(URL remoteRoot) {
		String localPathPrefix;
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(remoteRoot);
			localPathPrefix = remoteRepository.getLocalPathPrefix();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		return localPathPrefix;
	}

	public void sync(ProgressMonitor monitor) {
		assertNotNull("monitor", monitor);
		monitor.beginTask("Synchronising...", 201);
		try {
			readRemoteRepositoryIDFromRepoTransport();
			monitor.worked(1);

			logger.info("sync: locally syncing {} ('{}')", localRepositoryID, localRoot);
			localRepoManager.localSync(new SubProgressMonitor(monitor, 50));

			if (!TEST_INVERSE) { // This is the normal sync (NOT test).
				logger.info("sync: down: fromID={} from='{}' toID={} to='{}'", remoteRepositoryID, remoteRoot, localRepositoryID, localRoot);
				sync(remoteRepoTransport, true, localRepoTransport, new SubProgressMonitor(monitor, 50));

				logger.info("sync: up: fromID={} from='{}' toID={} to='{}'", localRepositoryID, localRoot, remoteRepositoryID, remoteRoot);
				sync(localRepoTransport, false, remoteRepoTransport, new SubProgressMonitor(monitor, 50));

				// Immediately sync back to make sure the changes we caused don't cause problems later
				// (right now there's very likely no collision and this should be very fast).
				logger.info("sync: down again: fromID={} from='{}' toID={} to='{}'", remoteRepositoryID, remoteRoot, localRepositoryID, localRoot);
				sync(remoteRepoTransport, false, localRepoTransport, new SubProgressMonitor(monitor, 50));
			}
			else { // THIS IS FOR TESTING ONLY!
				logger.info("sync: locally syncing on *remote* side {} ('{}')", localRepositoryID, localRoot);
				remoteRepoTransport.getChangeSet(true); // trigger the local sync on the remote side (we don't need the change set)

				logger.info("sync: up: fromID={} from='{}' toID={} to='{}'", localRepositoryID, localRoot, remoteRepositoryID, remoteRoot);
				sync(localRepoTransport, false, remoteRepoTransport, new SubProgressMonitor(monitor, 50));

				logger.info("sync: down: fromID={} from='{}' toID={} to='{}'", remoteRepositoryID, remoteRoot, localRepositoryID, localRoot);
				sync(remoteRepoTransport, false, localRepoTransport, new SubProgressMonitor(monitor, 50));

				logger.info("sync: up again: fromID={} from='{}' toID={} to='{}'", localRepositoryID, localRoot, remoteRepositoryID, remoteRoot);
				sync(localRepoTransport, false, remoteRepoTransport, new SubProgressMonitor(monitor, 50));
			}
		} finally {
			monitor.done();
		}
	}

	private EntityID readRemoteRepositoryIDFromLocalDB() {
		EntityID remoteRepositoryID;
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(remoteRoot);
			remoteRepositoryID = remoteRepository.getEntityID();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		return remoteRepositoryID;
	}

	private void readRemoteRepositoryIDFromRepoTransport() {
		EntityID repositoryID = remoteRepoTransport.getRepositoryID();
		if (repositoryID == null)
			throw new IllegalStateException("remoteRepoTransport.getRepositoryID() returned null!");

		if (!repositoryID.equals(remoteRepositoryID))
			throw new IllegalStateException(
					String.format("remoteRepoTransport.getRepositoryID() does not match repositoryID in local DB! %s != %s", repositoryID, remoteRepositoryID));
	}

//	private EntityID getRepositoryID(RepoTransport repoTransport) {
//		if (localRepoTransport == repoTransport)
//			return localRepositoryID;
//
//		if (remoteRepoTransport == repoTransport)
//			return remoteRepositoryID;
//
//		throw new IllegalArgumentException("repoTransport is neither local nor remote!");
//	}

	private RepoTransport createRepoTransport(File rootFile, EntityID clientRepositoryID) {
		URL rootURL;
		try {
			rootURL = rootFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return createRepoTransport(rootURL, clientRepositoryID);
	}

	private RepoTransport createRepoTransport(URL remoteRoot, EntityID clientRepositoryID) {
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot);
		return repoTransportFactory.createRepoTransport(remoteRoot, clientRepositoryID);
	}

	private void sync(RepoTransport fromRepoTransport, boolean fromRepoLocalSync, RepoTransport toRepoTransport, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			ChangeSetDTO changeSetDTO = fromRepoTransport.getChangeSet(fromRepoLocalSync);
			monitor.worked(8);

			sync(fromRepoTransport, toRepoTransport, changeSetDTO, new SubProgressMonitor(monitor, 90));

			fromRepoTransport.endSyncFromRepository();
			toRepoTransport.endSyncToRepository(changeSetDTO.getRepositoryDTO().getRevision());
			monitor.worked(2);
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ChangeSetDTO changeSetDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", changeSetDTO.getModificationDTOs().size() + changeSetDTO.getRepoFileDTOs().size());
		try {
			for (ModificationDTO modificationDTO : changeSetDTO.getModificationDTOs()) {
				syncModification(fromRepoTransport, toRepoTransport, modificationDTO, new SubProgressMonitor(monitor, 1));
			}

			RepoFileDTOTreeNode repoFileDTOTree = RepoFileDTOTreeNode.createTree(changeSetDTO.getRepoFileDTOs());
			if (repoFileDTOTree != null) {
				for (RepoFileDTOTreeNode repoFileDTOTreeNode : repoFileDTOTree) {
					RepoFileDTO repoFileDTO = repoFileDTOTreeNode.getRepoFileDTO();
					if (repoFileDTO instanceof DirectoryDTO)
						syncDirectory(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, (DirectoryDTO) repoFileDTO, new SubProgressMonitor(monitor, 1));
					else if (repoFileDTO instanceof NormalFileDTO)
						syncFile(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, repoFileDTO, new SubProgressMonitor(monitor, 1));
					else
						throw new IllegalStateException("Unsupported RepoFileDTO type: " + repoFileDTO);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void syncModification(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ModificationDTO modificationDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			if (modificationDTO instanceof DeleteModificationDTO) {
				DeleteModificationDTO deleteModificationDTO = (DeleteModificationDTO) modificationDTO;
				logger.info("syncModification: Deleting '{}'", deleteModificationDTO.getPath());
				toRepoTransport.delete(deleteModificationDTO.getPath());
			}
			else
				throw new IllegalStateException("Unknown modificationDTO type: " + modificationDTO);
		} finally {
			monitor.done();
		}
	}

	private void syncDirectory(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, DirectoryDTO directoryDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			String path = repoFileDTOTreeNode.getPath();
			logger.info("syncDirectory: path='{}'", path);
			try {
				toRepoTransport.makeDirectory(path, directoryDTO.getLastModified());
			} catch (DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during makeDirectory: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
		} finally {
			monitor.done();
		}
	}

	private void syncFile(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, RepoFileDTO normalFileDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			String path = repoFileDTOTreeNode.getPath();
			logger.info("syncFile: path='{}'", path);

			FileChunkSetDTO fromFileChunkSetResponse = fromRepoTransport.getFileChunkSet(path);
			if (!assertNotNull("fromFileChunkSetResponse", fromFileChunkSetResponse).isFileExists()) {
				logger.warn("File was deleted during sync on source side: {}", path);
				return;
			}
			monitor.worked(10);

			FileChunkSetDTO toFileChunkSetResponse = toRepoTransport.getFileChunkSet(path);
			if (areFilesExistingAndEqual(fromFileChunkSetResponse, assertNotNull("toFileChunkSetResponse", toFileChunkSetResponse))) {
				logger.info("File is already equal on destination side: {}", path);
				return;
			}
			monitor.worked(10);

			try {
				toRepoTransport.beginPutFile(path);
			} catch (DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during beginPutFile: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
			monitor.worked(1);

			List<FileChunkDTO> fromFileChunksDirty = new ArrayList<FileChunkDTO>();
			Iterator<FileChunkDTO> toFileChunkIterator = toFileChunkSetResponse.getFileChunkDTOs().iterator();
			int fileChunkIndex = -1;
			for (FileChunkDTO fromFileChunk : fromFileChunkSetResponse.getFileChunkDTOs()) {
				FileChunkDTO toFileChunk = toFileChunkIterator.hasNext() ? toFileChunkIterator.next() : null;
				++fileChunkIndex;
				if (toFileChunk != null
						&& equal(fromFileChunk.getLength(), toFileChunk.getLength())
						&& equal(fromFileChunk.getSha1(), toFileChunk.getSha1())) {
					logger.debug("Skipping FileChunkDTO {} (already equal on destination side). File: {}", fileChunkIndex, path);
					continue;
				}
				fromFileChunksDirty.add(fromFileChunk);
			}

			ProgressMonitor subMonitor = new SubProgressMonitor(monitor, 73);
			subMonitor.beginTask("Synchronising...", fromFileChunksDirty.size());
			for (FileChunkDTO fileChunkDTO : fromFileChunksDirty) {
				byte[] fileData = fromRepoTransport.getFileData(path, fileChunkDTO.getOffset(), fileChunkDTO.getLength());

				if (fileData == null || fileData.length != fileChunkDTO.getLength() || !sha1(fileData).equals(fileChunkDTO.getSha1())) {
					logger.warn("Source file was modified or deleted during sync: {}", path);
					// The file is left in state 'inProgress'. Thus it should definitely not be synced back in the opposite
					// direction. The file should be synced again in the correct direction in the next run (after the source
					// repo did a local sync, too).
					return;
				}

				toRepoTransport.putFileData(path, fileChunkDTO.getOffset(), fileData);
				subMonitor.worked(1);
			}
			subMonitor.done();

			toRepoTransport.endPutFile(
					path, fromFileChunkSetResponse.getLastModified(),
					fromFileChunkSetResponse.getLength());

			monitor.worked(6);
		} finally {
			monitor.done();
		}
	}

	private String sha1(byte[] data) {
		assertNotNull("data", data);
		try {
			byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(data));
			return HashUtil.encodeHexStr(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean areFilesExistingAndEqual(FileChunkSetDTO fromFileChunkSetResponse, FileChunkSetDTO toFileChunkSetResponse) {
		return (fromFileChunkSetResponse.isFileExists()
				&& toFileChunkSetResponse.isFileExists()
				&& equal(fromFileChunkSetResponse.getLength(), toFileChunkSetResponse.getLength())
				&& equal(fromFileChunkSetResponse.getLastModified(), toFileChunkSetResponse.getLastModified())
				&& equal(fromFileChunkSetResponse.getSha1(), toFileChunkSetResponse.getSha1()));
	}

	public void close() {
		localRepoManager.close();
		localRepoTransport.close();
		remoteRepoTransport.close();
	}
}
