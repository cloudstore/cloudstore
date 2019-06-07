package co.codewizards.cloudstore.local.transport;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.dto.TempChunkFileDto;
import co.codewizards.cloudstore.core.dto.VersionInfoDto;
import co.codewizards.cloudstore.core.dto.jaxb.TempChunkFileDtoIo;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.CollisionException;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.repo.transport.FileWriteStrategy;
import co.codewizards.cloudstore.core.repo.transport.LocalRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.TransferDoneMarkerType;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;
import co.codewizards.cloudstore.core.version.VersionInfoProvider;
import co.codewizards.cloudstore.local.FilenameFilterSkipMetaDir;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.dto.RepositoryDtoConverter;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarker;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarkerDao;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepoDao;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.Modification;
import co.codewizards.cloudstore.local.persistence.ModificationDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequestDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.persistence.Symlink;
import co.codewizards.cloudstore.local.persistence.TransferDoneMarker;
import co.codewizards.cloudstore.local.persistence.TransferDoneMarkerDao;

public class FileRepoTransport extends AbstractRepoTransport implements LocalRepoTransport {
	private static final Logger logger = LoggerFactory.getLogger(FileRepoTransport.class);

	private static final long MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY = 100; // TODO make configurable!

	private LocalRepoManager localRepoManager;
	private final TempChunkFileManager tempChunkFileManager = TempChunkFileManager.getInstance();

	@Override
	public void close() {
		if (localRepoManager != null) {
			logger.debug("close: Closing localRepoManager.");
			localRepoManager.close();
		} else
			logger.debug("close: There is no localRepoManager.");

		super.close();
	}

	@Override
	public UUID getRepositoryId() {
		return getLocalRepoManager().getRepositoryId();
	}

	@Override
	public byte[] getPublicKey() {
		return getLocalRepoManager().getPublicKey();
	}

	@Override
	public void requestRepoConnection(final byte[] publicKey) {
		assertNotNull(publicKey, "publicKey");
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepository(clientRepositoryId);
			if (remoteRepository != null)
				throw new IllegalArgumentException("RemoteRepository already connected! repositoryId=" + clientRepositoryId);

			final String localPathPrefix = getPathPrefix();
			final RemoteRepositoryRequestDao remoteRepositoryRequestDao = transaction.getDao(RemoteRepositoryRequestDao.class);
			RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDao.getRemoteRepositoryRequest(clientRepositoryId);
			if (remoteRepositoryRequest != null) {
				logger.info("RemoteRepository already requested to be connected. repositoryId={}", clientRepositoryId);

				// For security reasons, we do not allow to modify the public key! If we did,
				// an attacker might replace the public key while the user is verifying the public key's
				// fingerprint. The user would see & confirm the old public key, but the new public key
				// would be written to the RemoteRepository. This requires really lucky timing, but
				// if the attacker surveils the user, this might be feasable.
				if (!Arrays.equals(remoteRepositoryRequest.getPublicKey(), publicKey))
					throw new IllegalStateException("Cannot modify the public key! Use 'dropRepoConnection' to drop the old request or wait until it expired.");

				// For the same reasons stated above, we do not allow changing the local path-prefix, too.
				if (!remoteRepositoryRequest.getLocalPathPrefix().equals(localPathPrefix))
					throw new IllegalStateException("Cannot modify the local path-prefix! Use 'dropRepoConnection' to drop the old request or wait until it expired.");

				remoteRepositoryRequest.setChanged(new Date()); // make sure it is not deleted soon (the request expires after a while)
			}
			else {
				final long remoteRepositoryRequestsCount = remoteRepositoryRequestDao.getObjectsCount();
				if (remoteRepositoryRequestsCount >= MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY)
					throw new IllegalStateException(String.format(
							"The maximum number of connection requests (%s) is reached or exceeded! Please retry later, when old requests were accepted or expired.", MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY));

				remoteRepositoryRequest = new RemoteRepositoryRequest();
				remoteRepositoryRequest.setRepositoryId(clientRepositoryId);
				remoteRepositoryRequest.setPublicKey(publicKey);
				remoteRepositoryRequest.setLocalPathPrefix(localPathPrefix);
				remoteRepositoryRequestDao.makePersistent(remoteRepositoryRequest);
			}

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public RepositoryDto getRepositoryDto() {
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); ) {
			final LocalRepositoryDao localRepositoryDao = transaction.getDao(LocalRepositoryDao.class);
			final LocalRepository localRepository = localRepositoryDao.getLocalRepositoryOrFail();
			final RepositoryDto repositoryDto = RepositoryDtoConverter.create().toRepositoryDto(localRepository);
			transaction.commit();
			return repositoryDto;
		}
	}

	@Override
	public RepositoryDto getClientRepositoryDto() {
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); ) {
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepository(clientRepositoryId);
			assertNotNull(remoteRepository, "remoteRepository[" + clientRepositoryId + "]");
			final RepositoryDto repositoryDto = RepositoryDtoConverter.create().toRepositoryDto(remoteRepository);
			transaction.commit();
			return repositoryDto;
		}
	}

	@Override
	public ChangeSetDto getChangeSetDto(final boolean localSync, final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		if (localSync)
			getLocalRepoManager().localSync(new LoggerProgressMonitor(logger));

		RepositoryDto repositoryDto;
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			// We use a WRITE tx, because we write the LastSyncToRemoteRepo!
			repositoryDto = ChangeSetDtoBuilder
					.create(transaction, this)
					.prepareBuildChangeSetDto(lastSyncToRemoteRepoLocalRepositoryRevisionSynced);

			transaction.commit();
		}
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); ) {
			final ChangeSetDto changeSetDto = ChangeSetDtoBuilder
					.create(transaction, this)
					.buildChangeSetDto(repositoryDto);

			transaction.commit();
			return changeSetDto;
		}
	}

	@Override
	public void prepareForChangeSetDto(ChangeSetDto changeSetDto) {
		// nothing to do here.
	}

	@Override
	public void makeDirectory(String path, final Date lastModified) {
		path = prefixPath(path);
		final File file = getFile(path);
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);
			mkDir(transaction, clientRepositoryId, file, lastModified);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void makeSymlink(String path, final String target, final Date lastModified) {
		path = prefixPath(path);
		AssertUtil.assertNotNull(target, "target");
		final File file = getFile(path);
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);

			final File parentFile = file.getParentFile();
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);

				if (file.existsNoFollow() && !file.isSymbolicLink())
					handleFileTypeCollision(transaction, clientRepositoryId, file, SymlinkDto.class);
//					file.renameTo(IOUtil.createCollisionFile(file));

				if (file.existsNoFollow() && !file.isSymbolicLink())
					throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

				final File localRoot = getLocalRepoManager().getLocalRoot();

				try {
					final boolean currentTargetEqualsNewTarget;
//					final Path symlinkPath = file.toPath();
					if (file.isSymbolicLink()) {
//						final Path currentTargetPath = Files.readSymbolicLink(symlinkPath);
						final String currentTarget = file.readSymbolicLinkToPathString();
						currentTargetEqualsNewTarget = currentTarget.equals(target);
						if (!currentTargetEqualsNewTarget) {
							final RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
							if (repoFile == null) // it's new - just created
								handleFileCollision(transaction, clientRepositoryId, file);
							else
								detectAndHandleFileCollision(transaction, clientRepositoryId, parentFile, repoFile);

							file.delete();
						}
					}
					else
						currentTargetEqualsNewTarget = false;

					if (!currentTargetEqualsNewTarget)
						file.createSymbolicLink(target);

					if (lastModified != null)
						file.setLastModifiedNoFollow(lastModified.getTime());

				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				final RepoFile repoFile = syncRepoFile(transaction, file);

				if (repoFile == null)
					throw new IllegalStateException("LocalRepoSync.sync(...) did not create the RepoFile for file: " + file);

				if (!(repoFile instanceof Symlink))
					throw new IllegalStateException("LocalRepoSync.sync(...) created an instance of " + repoFile.getClass().getName() + " instead  of a Symlink for file: " + file);

				repoFile.setLastSyncFromRepositoryId(clientRepositoryId);

				final Collection<TempChunkFileWithDtoFile> tempChunkFileWithDtoFiles = tempChunkFileManager.getOffset2TempChunkFileWithDtoFile(file).values();
				for (final TempChunkFileWithDtoFile tempChunkFileWithDtoFile : tempChunkFileWithDtoFiles) {
					if (tempChunkFileWithDtoFile.getTempChunkFileDtoFile() != null)
						deleteOrFail(tempChunkFileWithDtoFile.getTempChunkFileDtoFile());

					if (tempChunkFileWithDtoFile.getTempChunkFile() != null)
						deleteOrFail(tempChunkFileWithDtoFile.getTempChunkFile());
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}

			transaction.commit();
		}
	}

	protected void assertNoDeleteModificationCollision(final LocalRepoTransaction transaction, final UUID fromRepositoryId, String path) throws CollisionException {
		final RemoteRepository fromRemoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(fromRepositoryId);
		final long lastSyncFromRemoteRepositoryLocalRevision = fromRemoteRepository.getLocalRevision();

		if (!path.startsWith("/"))
			path = '/' + path;

		final DeleteModificationDao deleteModificationDao = transaction.getDao(DeleteModificationDao.class);
		final Collection<DeleteModification> deleteModifications = deleteModificationDao.getDeleteModificationsForPathOrParentOfPathAfter(
				path, lastSyncFromRemoteRepositoryLocalRevision, fromRemoteRepository);

		if (!deleteModifications.isEmpty())
			throw new DeleteModificationCollisionException(
					String.format("There is at least one DeleteModification for repositoryId=%s path='%s'", fromRepositoryId, path));
	}

	@Override
	public void copy(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);

		final File fromFile = getFile(fromPath);
		final File toFile = getFile(toPath);

		if (!fromFile.isFile()) // TODO throw an exception and catch in RepoToRepoSync!
			return;

		if (toFile.existsNoFollow()) // TODO either simply throw an exception or implement proper collision check.
			return;

		final File toParentFile = toFile.getParentFile();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(toParentFile);
			try {
				try {
					if (!toParentFile.isDirectory())
						toParentFile.mkdirs();

					fromFile.copyToCopyAttributes(toFile);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				final LocalRepoSync localRepoSync = LocalRepoSync.create(transaction);
				final RepoFile toRepoFile = localRepoSync.sync(toFile, new NullProgressMonitor(), true);
				AssertUtil.assertNotNull(toRepoFile, "toRepoFile");
				toRepoFile.setLastSyncFromRepositoryId(getClientRepositoryIdOrFail());
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(toParentFile);
			}
			transaction.commit();
		}
	}

	@Override
	public void move(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);

		final File fromFile = getFile(fromPath);
		final File toFile = getFile(toPath);

		if (!fromFile.isFile()) // TODO throw an exception and catch in RepoToRepoSync!
			return;

		if (toFile.existsNoFollow()) // TODO either simply throw an exception or implement proper collision check.
			return;

		final File fromParentFile = fromFile.getParentFile();
		final File toParentFile = toFile.getParentFile();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(fromParentFile);
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(toParentFile);
			try {
				try {
					if (!toParentFile.isDirectory())
						toParentFile.mkdirs();

					fromFile.move(toFile);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				final LocalRepoSync localRepoSync = LocalRepoSync.create(transaction);
				final RepoFile toRepoFile = localRepoSync.sync(toFile, new NullProgressMonitor(), true);
				final RepoFile fromRepoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fromFile);
				if (fromRepoFile != null)
					localRepoSync.deleteRepoFile(fromRepoFile);

				assertNotNull(toRepoFile, "toRepoFile");

				toRepoFile.setLastSyncFromRepositoryId(getClientRepositoryIdOrFail());
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(fromParentFile);
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(toParentFile);
			}
			transaction.commit();
		}
		moveFileInProgressLocalRepo(getClientRepositoryId(), getRepositoryId(), fromPath, toPath);
		tempChunkFileManager.moveChunks(fromFile, toFile);
	}

	private void moveFileInProgressLocalRepo(final UUID fromRepositoryId, final UUID toRepositoryId,
			String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final FileInProgressMarkerDao fileInProgressMarkerDao = transaction.getDao(FileInProgressMarkerDao.class);
			final FileInProgressMarker toFileInProgressMarker = fileInProgressMarkerDao.getFileInProgressMarker(fromRepositoryId, toRepositoryId, fromPath);
			if (toFileInProgressMarker != null ) {
				logger.info("Updating FileInProgressMarker: {}, new toPath={}", toFileInProgressMarker, toPath);
				toFileInProgressMarker.setPath(toPath);
			}
			transaction.commit();
		}
	}

	@Override
	public void delete(String path) {
		path = prefixPath(path);
		final File file = getFile(path);
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final boolean fileIsLocalRoot = getLocalRepoManager().getLocalRoot().equals(file);
		final File parentFile = file.getParentFile();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				final LocalRepoSync localRepoSync = LocalRepoSync.create(transaction); // not sure about the ignoreRulesEnabled here.
				localRepoSync.sync(file, new NullProgressMonitor(), true);

				if (fileIsLocalRoot) {
					// Cannot delete the repository's root! Deleting all its contents instead.
					final long fileLastModified = file.lastModified();
					try {
						final File[] children = file.listFiles(new FilenameFilterSkipMetaDir());
						if (children == null)
							throw new IllegalStateException("File-listing localRoot returned null: " + file);

						for (final File child : children)
							delete(transaction, localRepoSync, clientRepositoryId, child);
					} finally {
						file.setLastModified(fileLastModified);
					}
				}
				else
					delete(transaction, localRepoSync, clientRepositoryId, file);

			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		}
	}

	private void delete(final LocalRepoTransaction transaction, final LocalRepoSync localRepoSync, final UUID fromRepositoryId, final File file) {
		if (detectFileCollisionRecursively(transaction, fromRepositoryId, file))
			handleFileCollision(transaction, fromRepositoryId, file);

		if (!IOUtil.deleteDirectoryRecursively(file)) {
			throw new IllegalStateException("Deleting file or directory failed: " + file);
		}

		final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
		if (repoFile != null)
			localRepoSync.deleteRepoFile(repoFile);
	}

	@Override
	public RepoFileDto getRepoFileDto(String path) {
		RepoFileDto repoFileDto = null;
		path = prefixPath(path);
		final File file = getFile(path);
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			// WRITE tx, because it performs a local sync!

			final LocalRepoSync localRepoSync = LocalRepoSync.create(transaction);
			localRepoSync.sync(file, new NullProgressMonitor(), false); // TODO or do we need recursiveChildren==true here?

			final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
			final RepoFile repoFile = repoFileDao.getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (repoFile != null) {
				final RepoFileDtoConverter converter = RepoFileDtoConverter.create(transaction);
				repoFileDto = converter.toRepoFileDto(repoFile, Integer.MAX_VALUE); // TODO pass depth as argument - or maybe leave it this way?
			}

			transaction.commit();
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
		return repoFileDto;
	}

	@Override
	public LocalRepoManager getLocalRepoManager() {
		if (localRepoManager == null) {
			logger.debug("getLocalRepoManager: Creating a new LocalRepoManager.");
			File remoteRootFile;
			try {
				remoteRootFile = createFile(getRemoteRootWithoutPathPrefix().toURI());
			} catch (final URISyntaxException e) {
				throw new RuntimeException(e);
			}
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(remoteRootFile);
		}
		return localRepoManager;
	}

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		final File remoteRootFile = UrlUtil.getFile(getRemoteRoot());

		final File localRootFile = LocalRepoHelper.getLocalRootContainingFile(remoteRootFile);
		if (localRootFile == null)
			throw new IllegalStateException(String.format(
					"remoteRoot='%s' does not point to a file or directory within an existing repository (nor its root directory)!",
					getRemoteRoot()));

		try {
			return localRootFile.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

//	private List<FileChunkDto> toFileChunkDtos(final Set<FileChunk> fileChunks) {
//		final long startTimestamp = System.currentTimeMillis();
//		final List<FileChunkDto> result = new ArrayList<FileChunkDto>(AssertUtil.assertNotNull("fileChunks", fileChunks).size());
//		for (final FileChunk fileChunk : fileChunks) {
//			final FileChunkDto fileChunkDto = toFileChunkDto(fileChunk);
//			if (fileChunkDto != null)
//				result.add(fileChunkDto);
//		}
//		logger.debug("toFileChunkDtos: Creating {} FileChunkDtos took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);
//		return result;
//	}
//
//	private FileChunkDto toFileChunkDto(final FileChunk fileChunk) {
//		final FileChunkDto dto = new FileChunkDto();
//		dto.setLength(fileChunk.getLength());
//		dto.setOffset(fileChunk.getOffset());
//		dto.setSha1(fileChunk.getSha1());
//		return dto;
//	}
//	private List<RepoFileDto> toRepoFileDtos(final Collection<RepoFile> fileChunks) {
//		final long startTimestamp = System.currentTimeMillis();
//		final RepoFileDtoConverter converter = new RepoFileDtoConverter(transaction);
//		final List<RepoFileDto> result = new ArrayList<RepoFileDto>(AssertUtil.assertNotNull("fileChunks", fileChunks).size());
//		for (final RepoFile fileChunk : fileChunks) {
//			final RepoFileDto fileChunkDto = toRepoFileDto(fileChunk);
//			if (fileChunkDto != null)
//				result.add(fileChunkDto);
//		}
//		logger.debug("toFileChunkDtos: Creating {} FileChunkDtos took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);
//		return result;
//	}
//
//	private RepoFileDto toRepoFileDto(final RepoFile repoFile) {
//		final FileChunkDto dto = new FileChunkDto();
//		dto.setLength(repoFile.getLength());
//		dto.setOffset(repoFile.getOffset());
//		dto.setSha1(repoFile.getSha1());
//		return dto;
//	}


	protected void mkDir(final LocalRepoTransaction transaction, final UUID clientRepositoryId, final File file, final Date lastModified) {
		AssertUtil.assertNotNull(transaction, "transaction");
		AssertUtil.assertNotNull(file, "file");

		final File localRoot = getLocalRepoManager().getLocalRoot();
		final File parentFile = localRoot.equals(file) ? null : file.getParentFile();

		if (parentFile != null)
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);

		try {
			RepoFile parentRepoFile = parentFile == null ? null : transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, parentFile);

			if (parentFile != null) {
				if (!localRoot.equals(parentFile) && (!parentFile.isDirectory() || parentRepoFile == null))
					mkDir(transaction, clientRepositoryId, parentFile, null);

				if (parentRepoFile == null)
					parentRepoFile = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, parentFile);

				if (parentRepoFile == null) // now, it should definitely not be null anymore!
					throw new IllegalStateException("parentRepoFile == null");
			}

			if (file.existsNoFollow() && !file.isDirectory())
				handleFileTypeCollision(transaction, clientRepositoryId, file, DirectoryDto.class);

			if (file.existsNoFollow() && !file.isDirectory())
				throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

			if (!file.isDirectory())
				file.mkdir();

			if (!file.isDirectory())
				throw new IllegalStateException("Could not create directory (permissions?!): " + file);

//			RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, file);
//			if (repoFile != null && !(repoFile instanceof Directory)) {
//				transaction.getDao(RepoFileDao.class).deletePersistent(repoFile);
//				repoFile = null;
//			}

			if (lastModified != null)
				file.setLastModified(lastModified.getTime());

			RepoFile repoFile = syncRepoFile(transaction, file);
			if (repoFile == null)
				throw new IllegalStateException("Just created directory, but corresponding RepoFile still does not exist after local sync: " + file);

			if (!(repoFile instanceof Directory))
				throw new IllegalStateException("Just created directory, and even though the corresponding RepoFile now exists, it is not an instance of Directory! It is a " + repoFile.getClass().getName() + " instead! " + file);

			repoFile.setLastSyncFromRepositoryId(clientRepositoryId);
		} finally {
			if (parentFile != null)
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
		}
	}

	/**
	 * Syncs the single file/directory/symlink passed as {@code file} into the database non-recursively.
	 * @param transaction the current transaction. Must not be <code>null</code>.
	 * @param file the file (every type, i.e. might be a directory or symlink, too) to be synced.
	 * @return the {@link RepoFile} that was created/updated for the given {@code file}.
	 */
	protected RepoFile syncRepoFile(final LocalRepoTransaction transaction, final File file) {
		assertNotNull(transaction, "transaction");
		assertNotNull(file, "file");
		return LocalRepoSync.create(transaction)
				.sync(file, new NullProgressMonitor(), false); // recursiveChildren==false, because we only need this one single Directory object in the DB, and we MUST NOT consume time with its children.
	}

	/**
	 * @param path the prefixed path (relative to the real root).
	 * @return the file in the local repository. Never <code>null</code>.
	 */
	protected File getFile(String path) {
		path = AssertUtil.assertNotNull(path, "path").replace('/', FILE_SEPARATOR_CHAR);
		final File file = createFile(getLocalRepoManager().getLocalRoot(), path);
		return file;
	}

	@Override
	public byte[] getFileData(String path, final long offset, int length) {
		path = prefixPath(path);
		final File file = getFile(path);
		try {
			final RandomAccessFile raf = file.createRandomAccessFile("r");
			try {
				raf.seek(offset);
				if (length < 0) {
					final long l = raf.length() - offset;
					if (l > Integer.MAX_VALUE)
						throw new IllegalArgumentException(
								String.format("The data to be read from file '%s' is too large (offset=%s length=%s limit=%s). You must specify a length (and optionally an offset) to read it partially.",
										path, offset, length, Integer.MAX_VALUE));

					length = (int) l;
				}

				final byte[] bytes = new byte[length];
				int off = 0;
				int numRead = 0;
				while (off < bytes.length && (numRead = raf.read(bytes, off, bytes.length-off)) >= 0) {
					off += numRead;
				}

				if (off < bytes.length) // Read INCOMPLETELY => discarding
					return null;

				return bytes;
			} finally {
				raf.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void beginPutFile(String path) {
		path = prefixPath(path);
		final File file = getFile(path); // null-check already inside getFile(...) - no need for another check here
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final File parentFile = file.getParentFile();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				if (file.isSymbolicLink() || (file.exists() && !file.isFile())) // exists() and isFile() both resolve symlinks! Their result depends on where the symlink points to.
					handleFileTypeCollision(transaction, clientRepositoryId, file, NormalFileDto.class);

				if (file.isSymbolicLink() || (file.exists() && !file.isFile())) // the default implementation of handleFileTypeCollision(...) moves the file away.
					throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

				final File localRoot = getLocalRepoManager().getLocalRoot();
				assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);

				boolean newFile = false;
				if (!file.isFile()) {
					newFile = true;
					try {
						file.createNewFile();
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}

				if (!file.isFile())
					throw new IllegalStateException("Could not create file (permissions?!): " + file);

				// A complete sync run might take very long. Therefore, we better update our local meta-data
				// *immediately* before beginning the sync of this file and before detecting a collision.
				// Furthermore, maybe the file is new and there's no meta-data, yet, hence we must do this anyway.
//				final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
//				LocalRepoSync.create(transaction).sync(file, new NullProgressMonitor(), false); // recursiveChildren has no effect on simple files, anyway (it's no directory).

				tempChunkFileManager.deleteTempChunkFilesWithoutDtoFile(tempChunkFileManager.getOffset2TempChunkFileWithDtoFile(file).values());

				final RepoFile repoFile = syncRepoFile(transaction, file);
				if (repoFile == null)
					throw new IllegalStateException("LocalRepoSync.sync(...) did not create the RepoFile for file: " + file);

				if (!(repoFile instanceof NormalFile))
					throw new IllegalStateException("LocalRepoSync.sync(...) created an instance of " + repoFile.getClass().getName() + " instead  of a NormalFile for file: " + file);

				final NormalFile normalFile = (NormalFile) repoFile;

				if (!newFile && !normalFile.isInProgress())
					detectAndHandleFileCollision(transaction, clientRepositoryId, file, normalFile);

				normalFile.setLastSyncFromRepositoryId(clientRepositoryId);
				normalFile.setInProgress(true);
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		}
	}

	/**
	 * Handle a file-type-collision, which was already detected.
	 * <p>
	 * This method does not analyse whether there is a collision - this is already sure.
	 * It only handles the collision by logging and delegating to {@link #handleFileCollision(LocalRepoTransaction, UUID, File)}.
	 * @param transaction the DB transaction. Must not be <code>null</code>.
	 * @param fromRepositoryId the ID of the source repository from which the file is about to be copied. Must not be <code>null</code>.
	 * @param file the file that is to be copied (i.e. overwritten). Must not be <code>null</code>. This may be a directory or a symlink, too!
	 */
	protected void handleFileTypeCollision(final LocalRepoTransaction transaction, final UUID fromRepositoryId, final File file, final Class<? extends RepoFileDto> fromFileType) {
		assertNotNull(transaction, "transaction");
		assertNotNull(fromRepositoryId, "fromRepositoryId");
		assertNotNull(file, "file");
		assertNotNull(fromFileType, "fromFileType");

		Class<? extends RepoFileDto> toFileType;
		if (file.isSymbolicLink())
			toFileType = SymlinkDto.class;
		else if (file.isFile())
			toFileType = NormalFileDto.class;
		else if (file.isDirectory())
			toFileType = DirectoryDto.class;
		else
			throw new IllegalStateException("file has unknown type: " + file);

		logger.info("handleFileTypeCollision: Collision: Destination file already exists, is modified and has a different type! toFileType={} fromFileType={} file='{}'",
				toFileType.getSimpleName(), fromFileType.getSimpleName(), file.getAbsolutePath());

		final File collisionFile = handleFileCollision(transaction, fromRepositoryId, file);
		LocalRepoSync.create(transaction).sync(collisionFile, new NullProgressMonitor(), true); // recursiveChildren==true, because the colliding thing might be a directory.
	}

	/**
	 * Detect if the file to be copied has been modified locally (or copied from another repository) after the last
	 * sync from the repository identified by {@code fromRepositoryId}.
	 * <p>
	 * If there is a collision - i.e. the destination file has been modified, too - then the destination file is moved
	 * away by renaming it. The name to which it is renamed is created by {@link IOUtil#createCollisionFile(File)}.
	 * Afterwards the file is copied back to its original name.
	 * <p>
	 * The reason for renaming it first (instead of directly copying it) is that there might be open file handles.
	 * In GNU/Linux, the open file handles stay open and thus are then connected to the renamed file, thus continuing
	 * to modify the file which was moved away. In Windows, the renaming likely fails and we abort with an exception.
	 * In both cases, we do our best to avoid both processes from writing to the same file simultaneously without locking
	 * it.
	 * <p>
	 * In the future (this is NOT YET IMPLEMENTED), we might lock it in {@link #beginPutFile(String)} and
	 * keep the lock until {@link #endPutFile(String, Date, long, String)} or a timeout occurs - and refresh the lock
	 * (i.e. postpone the timeout) with every {@link #putFileData(String, long, byte[])}. The reason for this
	 * quite complicated strategy is that we cannot guarantee that the {@link #endPutFile(String, Date, long, String)}
	 * is ever invoked (the client might crash inbetween). We don't want a locked file to linger forever.
	 *
	 * @param transaction the DB transaction. Must not be <code>null</code>.
	 * @param fromRepositoryId the ID of the source repository from which the file is about to be copied. Must not be <code>null</code>.
	 * @param file the file that is to be copied (i.e. overwritten). Must not be <code>null</code>.
	 * @param normalFileOrSymlink the DB entity corresponding to {@code file}. Must not be <code>null</code>.
	 */
	protected void detectAndHandleFileCollision(final LocalRepoTransaction transaction, final UUID fromRepositoryId, final File file, final RepoFile normalFileOrSymlink) {
		assertNotNull(transaction, "transaction");
		assertNotNull(fromRepositoryId, "fromRepositoryId");
		assertNotNull(file, "file");
		assertNotNull(normalFileOrSymlink, "normalFileOrSymlink");
		if (detectFileCollision(transaction, fromRepositoryId, file, normalFileOrSymlink)) {
			final File collisionFile = handleFileCollision(transaction, fromRepositoryId, file);

			try {
				collisionFile.copyToCopyAttributes(file);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

			LocalRepoSync.create(transaction).sync(collisionFile, new NullProgressMonitor(), true); // TODO sub-progress-monitor!
		}
	}

	protected File handleFileCollision(final LocalRepoTransaction transaction, final UUID fromRepositoryId, final File file) {
		assertNotNull(transaction, "transaction");
		assertNotNull(fromRepositoryId, "fromRepositoryId");
		assertNotNull(file, "file");
		final File collisionFile = IOUtil.createCollisionFile(file);
		file.renameTo(collisionFile);
		if (file.existsNoFollow())
			throw new IllegalStateException("Could not rename file to resolve collision: " + file);

		return collisionFile;
	}

	protected boolean detectFileCollisionRecursively(final LocalRepoTransaction transaction, final UUID fromRepositoryId, final File fileOrDirectory) {
		AssertUtil.assertNotNull(transaction, "transaction");
		AssertUtil.assertNotNull(fromRepositoryId, "fromRepositoryId");
		AssertUtil.assertNotNull(fileOrDirectory, "fileOrDirectory");

		// we handle symlinks before invoking exists() below, because this method and most other File methods resolve symlinks!
		if (fileOrDirectory.isSymbolicLink()) {
			final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fileOrDirectory);
			if (!(repoFile instanceof Symlink))
				return true; // We had a change after the last local sync (symlink => directory or normal file)!

			return detectFileCollision(transaction, fromRepositoryId, fileOrDirectory, repoFile);
		}

		if (!fileOrDirectory.exists()) { // Is this correct? If it does not exist, then there is no collision? TODO what if it has been deleted locally and modified remotely and local is destination and that's our collision?!
			return false;
		}

		if (fileOrDirectory.isFile()) {
			final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fileOrDirectory);
			if (!(repoFile instanceof NormalFile))
				return true; // We had a change after the last local sync (normal file => directory or symlink)!

			return detectFileCollision(transaction, fromRepositoryId, fileOrDirectory, repoFile);
		}

		final File[] children = fileOrDirectory.listFiles();
		if (children == null)
			throw new IllegalStateException("listFiles() of directory returned null: " + fileOrDirectory);

		for (final File child : children) {
			if (detectFileCollisionRecursively(transaction, fromRepositoryId, child))
				return true;
		}

		return false;
	}

	/**
	 * Detect if the file to be copied or deleted has been modified locally (or copied from another repository) after the last
	 * sync from the repository identified by {@code fromRepositoryId}.
	 * @param transaction
	 * @param fromRepositoryId
	 * @param file
	 * @param normalFileOrSymlink
	 * @return <code>true</code>, if there is a collision; <code>false</code>, if there is none.
	 */
	protected boolean detectFileCollision(final LocalRepoTransaction transaction, final UUID fromRepositoryId, final File file, final RepoFile normalFileOrSymlink) {
		AssertUtil.assertNotNull(transaction, "transaction");
		AssertUtil.assertNotNull(fromRepositoryId, "fromRepositoryId");
		AssertUtil.assertNotNull(file, "file");
		AssertUtil.assertNotNull(normalFileOrSymlink, "normalFileOrSymlink");

		if (!file.existsNoFollow()) {
			logger.debug("detectFileCollision: path='{}': return false, because destination file does not exist.", normalFileOrSymlink.getPath());
			return false;
		}

		final RemoteRepository fromRemoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(fromRepositoryId);
		final long lastSyncFromRemoteRepositoryLocalRevision = fromRemoteRepository.getLocalRevision();
		if (normalFileOrSymlink.getLocalRevision() <= lastSyncFromRemoteRepositoryLocalRevision) {
			logger.debug("detectFileCollision: path='{}': return false, because: normalFileOrSymlink.localRevision <= lastSyncFromRemoteRepositoryLocalRevision :: {} <= {}", normalFileOrSymlink.getPath(), normalFileOrSymlink.getLocalRevision(), lastSyncFromRemoteRepositoryLocalRevision);
			return false;
		}

		// The file was transferred from the same repository before and was thus not changed locally nor in another repo.
		// This can only happen, if the sync was interrupted (otherwise the check for the localRevision above
		// would have already caused this method to abort).
		if (fromRepositoryId.equals(normalFileOrSymlink.getLastSyncFromRepositoryId())) {
			logger.debug("detectFileCollision: path='{}': return false, because: fromRepositoryId == normalFileOrSymlink.lastSyncFromRepositoryId :: fromRepositoryId='{}'", normalFileOrSymlink.getPath(), fromRemoteRepository);
			return false;
		}

		logger.debug("detectFileCollision: path='{}': return true! fromRepositoryId='{}' normalFileOrSymlink.localRevision={} lastSyncFromRemoteRepositoryLocalRevision={} normalFileOrSymlink.lastSyncFromRepositoryId='{}'",
				normalFileOrSymlink.getPath(), fromRemoteRepository, normalFileOrSymlink.getLocalRevision(), lastSyncFromRemoteRepositoryLocalRevision, normalFileOrSymlink.getLastSyncFromRepositoryId());
		return true;
	}

	@Override
	public void putFileData(String path, final long offset, final byte[] fileData) {
		path = prefixPath(path);
		final File file = getFile(path);
		final File parentFile = file.getParentFile();
		final File localRoot = getLocalRepoManager().getLocalRoot();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); ) {
			// READ tx: It writes into the file system, but it only reads from the DB.

			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, file);
				if (repoFile == null)
					throw new IllegalStateException("No RepoFile found for file: " + file);

				if (!(repoFile instanceof NormalFile))
					throw new IllegalStateException("RepoFile is not an instance of NormalFile for file: " + file);

				final NormalFile normalFile = (NormalFile) repoFile;
				if (!normalFile.isInProgress())
					throw new IllegalStateException(String.format("NormalFile.inProgress == false! beginPutFile(...) not called?! repoFile=%s file=%s",
							repoFile, file));

				final FileWriteStrategy fileWriteStrategy = getFileWriteStrategy(file);
				logger.debug("putFileData: fileWriteStrategy={}", fileWriteStrategy);
				switch (fileWriteStrategy) {
					case directDuringTransfer:
						writeFileDataToDestFile(file, offset, fileData);
						break;
					case directAfterTransfer:
					case replaceAfterTransfer:
						tempChunkFileManager.writeFileDataToTempChunkFile(file, offset, fileData);
						break;
					default:
						throw new IllegalStateException("Unknown fileWriteStrategy: " + fileWriteStrategy);
				}
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		}
	}

	private void writeTempChunkFileToDestFile(final File destFile, final File tempChunkFile, final TempChunkFileDto tempChunkFileDto) {
		AssertUtil.assertNotNull(destFile, "destFile");
		AssertUtil.assertNotNull(tempChunkFile, "tempChunkFile");
		AssertUtil.assertNotNull(tempChunkFileDto, "tempChunkFileDto");
		final long offset = AssertUtil.assertNotNull(tempChunkFileDto.getFileChunkDto(), "tempChunkFileDto.fileChunkDto").getOffset();
		final byte[] fileData = new byte[(int) tempChunkFile.length()];
		try {
			final InputStream in = castStream(tempChunkFile.createInputStream());
			try {
				int off = 0;
				while (off < fileData.length) {
					final int bytesRead = in.read(fileData, off, fileData.length - off);
					if (bytesRead > 0) {
						off += bytesRead;
					}
					else if (bytesRead < 0) {
						throw new IllegalStateException("InputStream ended before expected file length!");
					}
				}
				if (off > fileData.length || in.read() != -1)
					throw new IllegalStateException("InputStream contained more data than expected file length!");
			} finally {
				in.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final String sha1FromDtoFile = tempChunkFileDto.getFileChunkDto().getSha1();
		final String sha1FromFileData = sha1(fileData);

		logger.trace("writeTempChunkFileToDestFile: Read {} bytes with SHA1 '{}' from '{}'.", fileData.length, sha1FromFileData, tempChunkFile.getAbsolutePath());

		if (!sha1FromFileData.equals(sha1FromDtoFile))
			throw new IllegalStateException("SHA1 mismatch! Corrupt temporary chunk file or corresponding Dto file: " + tempChunkFile.getAbsolutePath());

		writeFileDataToDestFile(destFile, offset, fileData);
	}

	private void writeFileDataToDestFile(final File destFile, final long offset, final byte[] fileData) {
		AssertUtil.assertNotNull(destFile, "destFile");
		AssertUtil.assertNotNull(fileData, "fileData");
		try {
			final RandomAccessFile raf = destFile.createRandomAccessFile("rw");
			try {
				raf.seek(offset);
				raf.write(fileData);
			} finally {
				raf.close();
			}
			logger.trace("writeFileDataToDestFile: Wrote {} bytes at offset {} to '{}'.", fileData.length, offset, destFile.getAbsolutePath());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String sha1(final byte[] data) {
		AssertUtil.assertNotNull(data, "data");
		try {
			final byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(data));
			return HashUtil.encodeHexStr(hash);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private final Map<File, FileWriteStrategy> file2FileWriteStrategy = new WeakHashMap<>();

	private FileWriteStrategy getFileWriteStrategy(final File file) {
		AssertUtil.assertNotNull(file, "file");
		synchronized (file2FileWriteStrategy) {
			FileWriteStrategy fileWriteStrategy = file2FileWriteStrategy.get(file);
			if (fileWriteStrategy == null) {
				fileWriteStrategy = ConfigImpl.getInstanceForFile(file).getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.CONFIG_DEFAULT_VALUE);
				file2FileWriteStrategy.put(file, fileWriteStrategy);
			}
			return fileWriteStrategy;
		}
	}

	@Override
	public void endPutFile(String path, final Date lastModified, final long length, final String sha1) {
		path = prefixPath(path);
		AssertUtil.assertNotNull(lastModified, "lastModified");
		final File file = getFile(path);
		final File parentFile = file.getParentFile();
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				final RepoFile repoFile = transaction.getDao(RepoFileDao.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
				if (!(repoFile instanceof NormalFile)) {
					throw new IllegalStateException(String.format("RepoFile is not an instance of NormalFile! repoFile=%s file=%s",
							repoFile, file));
				}

				final NormalFile normalFile = (NormalFile) repoFile;
				if (!normalFile.isInProgress())
					throw new IllegalStateException(String.format("NormalFile.inProgress == false! beginPutFile(...) not called?! repoFile=%s file=%s",
							repoFile, file));

				final FileWriteStrategy fileWriteStrategy = getFileWriteStrategy(file);
				logger.debug("endPutFile: fileWriteStrategy={}", fileWriteStrategy);

				final File destFile = (fileWriteStrategy == FileWriteStrategy.replaceAfterTransfer
						? createFile(file.getParentFile(), LocalRepoManager.TEMP_NEW_FILE_PREFIX + file.getName()) : file);

				final InputStream fileIn;
				if (destFile != file) {
					try {
						fileIn = castStream(file.createInputStream());
						destFile.createNewFile();
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}
				else
					fileIn = null;

				// tempChunkFileWithDtoFiles are sorted by offset (ascending)
				final Collection<TempChunkFileWithDtoFile> tempChunkFileWithDtoFiles = tempChunkFileManager.getOffset2TempChunkFileWithDtoFile(file).values();
				try {
					final TempChunkFileDtoIo tempChunkFileDtoIo = new TempChunkFileDtoIo();
					long destFileWriteOffset = 0;
					logger.debug("endPutFile: #tempChunkFileWithDtoFiles={}", tempChunkFileWithDtoFiles.size());
					for (final TempChunkFileWithDtoFile tempChunkFileWithDtoFile : tempChunkFileWithDtoFiles) {
						final File tempChunkFile = tempChunkFileWithDtoFile.getTempChunkFile(); // tempChunkFile may be null!!!
						final File tempChunkFileDtoFile = tempChunkFileWithDtoFile.getTempChunkFileDtoFile();
						if (tempChunkFileDtoFile == null)
							throw new IllegalStateException("No meta-data (tempChunkFileDtoFile) for file: " + (tempChunkFile == null ? null : tempChunkFile.getAbsolutePath()));

						final TempChunkFileDto tempChunkFileDto = tempChunkFileDtoIo.deserialize(tempChunkFileDtoFile);
						final long offset = AssertUtil.assertNotNull(tempChunkFileDto.getFileChunkDto(), "tempChunkFileDto.fileChunkDto").getOffset();

						if (fileIn != null) {
							// The following might fail, if *file* was truncated during the transfer. In this case,
							// throwing an exception now is probably the best choice as the next sync run will
							// continue cleanly.
							logger.info("endPutFile: writing from fileIn into destFile {}", destFile.getName());
							writeFileDataToDestFile(destFile, destFileWriteOffset, fileIn, offset - destFileWriteOffset);
							final long tempChunkFileLength = tempChunkFileDto.getFileChunkDto().getLength();
							skipOrFail(fileIn, tempChunkFileLength); // skipping beyond the EOF is supported by the FileInputStream according to Javadoc.
							destFileWriteOffset = offset + tempChunkFileLength;
						}

						if (tempChunkFile != null && tempChunkFile.exists()) {
							logger.info("endPutFile: writing tempChunkFile {} into destFile {}", tempChunkFile.getName(), destFile.getName());
							writeTempChunkFileToDestFile(destFile, tempChunkFile, tempChunkFileDto);
							deleteOrFail(tempChunkFile);
						}
					}

					if (fileIn != null && destFileWriteOffset < length)
						writeFileDataToDestFile(destFile, destFileWriteOffset, fileIn, length - destFileWriteOffset);

				} finally {
					if (fileIn != null)
						fileIn.close();
				}

				try {
					final RandomAccessFile raf = destFile.createRandomAccessFile("rw");
					try {
						raf.setLength(length);
					} finally {
						raf.close();
					}
				} catch (final IOException e) {
					throw new RuntimeException(String.format("Setting file '%s' to length %d failed: %s",
							destFile.getAbsolutePath(), length, e), e);
				}

				if (destFile != file) {
					deleteOrFail(file);
					destFile.renameTo(file);
					if (!file.exists())
						throw new IllegalStateException(String.format("Renaming the file from '%s' to '%s' failed: The destination file does not exist.", destFile.getAbsolutePath(), file.getAbsolutePath()));

					if (destFile.exists())
						throw new IllegalStateException(String.format("Renaming the file from '%s' to '%s' failed: The source file still exists.", destFile.getAbsolutePath(), file.getAbsolutePath()));
				}

				tempChunkFileManager.deleteTempChunkFiles(tempChunkFileWithDtoFiles);
				tempChunkFileManager.deleteTempDirIfEmpty(file);

				final LocalRepoSync localRepoSync = LocalRepoSync.create(transaction);
				file.setLastModified(lastModified.getTime());
				localRepoSync.updateRepoFile(normalFile, file, new NullProgressMonitor());
				normalFile.setLastSyncFromRepositoryId(clientRepositoryId);
				normalFile.setInProgress(false);

				logger.trace("endPutFile: Committing: sha1='{}' file='{}'", normalFile.getSha1(), file);
				if (sha1 != null && !sha1.equals(normalFile.getSha1())) {
					logger.warn("endPutFile: File was modified during transport (either on source or destination side): expectedSha1='{}' foundSha1='{}' file='{}'",
							sha1, normalFile.getSha1(), file);
				}

			} catch (IOException x) {
				throw new RuntimeException(x);
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		}
	}

	/**
	 * Skip the given {@code length} number of bytes.
	 * <p>
	 * Because {@link InputStream#skip(long)} and {@link FileInputStream#skip(long)} are both documented to skip
	 * over less than the requested number of bytes "for a number of reasons", this method invokes the underlying
	 * skip(...) method multiple times until either EOF is reached or the requested number of bytes was skipped.
	 * In case of EOF, an
	 * @param in the {@link InputStream} to be skipped. Must not be <code>null</code>.
	 * @param length the number of bytes to be skipped. Must not be negative (i.e. <code>length &gt;= 0</code>).
	 */
	private void skipOrFail(final InputStream in, final long length) {
		AssertUtil.assertNotNull(in, "in");
		if (length < 0)
			throw new IllegalArgumentException("length < 0");

		long skipped = 0;
		int skippedNowWas0Counter = 0;
		while (skipped < length) {
			final long toSkip = length - skipped;
			try {
				final long skippedNow = in.skip(toSkip);
				if (skippedNow < 0)
					throw new IOException("in.skip(" + toSkip + ") returned " + skippedNow);

				if (skippedNow == 0) {
					if (++skippedNowWas0Counter >= 5) {
						throw new IOException(String.format(
								"Could not skip %s consecutive times!", skippedNowWas0Counter));
					}
				}
				else
					skippedNowWas0Counter = 0;

				skipped += skippedNow;
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void writeFileDataToDestFile(final File destFile, final long offset, final InputStream in, final long length) {
		AssertUtil.assertNotNull(destFile, "destFile");
		AssertUtil.assertNotNull(in, "in");
		if (offset < 0)
			throw new IllegalArgumentException("offset < 0");

		if (length == 0)
			return;

		if (length < 0)
			throw new IllegalArgumentException("length < 0");

		long lengthDone = 0;

		try {
			final RandomAccessFile raf = destFile.createRandomAccessFile("rw");
			try {
				raf.seek(offset);

				final byte[] buf = new byte[200 * 1024];

				while (lengthDone < length) {
					final long len = Math.min(length - lengthDone, buf.length);
					final int bytesRead = in.read(buf, 0, (int)len);
					if (bytesRead > 0) {
						raf.write(buf, 0, bytesRead);
						lengthDone += bytesRead;
					}
					else if (bytesRead < 0)
						throw new IOException("Premature end of stream!");
				}
			} finally {
				raf.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void endSyncFromRepository() {
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final PersistenceManager pm = ((co.codewizards.cloudstore.local.LocalRepoTransactionImpl)transaction).getPersistenceManager();
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final LastSyncToRemoteRepoDao lastSyncToRemoteRepoDao = transaction.getDao(LastSyncToRemoteRepoDao.class);
			final ModificationDao modificationDao = transaction.getDao(ModificationDao.class);
			final TransferDoneMarkerDao transferDoneMarkerDao = transaction.getDao(TransferDoneMarkerDao.class);

			final RemoteRepository toRemoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(clientRepositoryId);

			final LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.getLastSyncToRemoteRepoOrFail(toRemoteRepository);
			if (lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress() < 0)
				throw new IllegalStateException(String.format("lastSyncToRemoteRepo.localRepositoryRevisionInProgress < 0 :: There is no sync in progress for the RemoteRepository with entityID=%s", clientRepositoryId));

			lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress());
			lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(-1);
			lastSyncToRemoteRepo.setResyncMode(false);

			pm.flush(); // prevent problems caused by batching, deletion and foreign keys
			final Collection<Modification> modifications = modificationDao.getModificationsBeforeOrEqual(
					toRemoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			modificationDao.deletePersistentAll(modifications);
			pm.flush();

			transferDoneMarkerDao.deleteRepoFileTransferDones(getRepositoryId(), clientRepositoryId);

			final FileInProgressMarkerDao fileInProgressMarkerDao = transaction.getDao(FileInProgressMarkerDao.class);
			fileInProgressMarkerDao.deleteFileInProgressMarkers(getRepositoryId(), clientRepositoryId);

			logger.info("endSyncFromRepository: localRepositoryId={} remoteRepositoryId={} localRepositoryRevisionSynced={}",
					getRepositoryId(), toRemoteRepository.getRepositoryId(),
					lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());

			transaction.commit();
		}
	}

	@Override
	public void endSyncToRepository(final long fromLocalRevision) {
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final TransferDoneMarkerDao transferDoneMarkerDao = transaction.getDao(TransferDoneMarkerDao.class);

			final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(clientRepositoryId);
			remoteRepository.setRevision(fromLocalRevision);

			transferDoneMarkerDao.deleteRepoFileTransferDones(clientRepositoryId, getRepositoryId());

			final FileInProgressMarkerDao fileInProgressMarkerDao = transaction.getDao(FileInProgressMarkerDao.class);
			fileInProgressMarkerDao.deleteFileInProgressMarkers(clientRepositoryId, getRepositoryId());

			logger.info("endSyncToRepository: localRepositoryId={} remoteRepositoryId={} transaction.localRevision={} remoteFromLocalRevision={}",
					getRepositoryId(), clientRepositoryId,
					transaction.getLocalRevision(), fromLocalRevision);

			transaction.commit();
		}
	}

	@Override
	public boolean isTransferDone(final UUID fromRepositoryId, final UUID toRepositoryId, final TransferDoneMarkerType transferDoneMarkerType, final long fromEntityId, final long fromLocalRevision) {
		boolean result = false;
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); ) {
			final TransferDoneMarkerDao dao = transaction.getDao(TransferDoneMarkerDao.class);
			final TransferDoneMarker transferDoneMarker = dao.getTransferDoneMarker(
					fromRepositoryId, toRepositoryId, transferDoneMarkerType, fromEntityId);
			if (transferDoneMarker != null)
				result = fromLocalRevision == transferDoneMarker.getFromLocalRevision();

			transaction.commit();
		}
		return result;
	}

	@Override
	public void markTransferDone(final UUID fromRepositoryId, final UUID toRepositoryId, final TransferDoneMarkerType transferDoneMarkerType, final long fromEntityId, final long fromLocalRevision) {
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final TransferDoneMarkerDao dao = transaction.getDao(TransferDoneMarkerDao.class);
			TransferDoneMarker transferDoneMarker = dao.getTransferDoneMarker(
					fromRepositoryId, toRepositoryId, transferDoneMarkerType, fromEntityId);
			if (transferDoneMarker == null) {
				transferDoneMarker = new TransferDoneMarker();
				transferDoneMarker.setFromRepositoryId(fromRepositoryId);
				transferDoneMarker.setToRepositoryId(toRepositoryId);
				transferDoneMarker.setTransferDoneMarkerType(transferDoneMarkerType);
				transferDoneMarker.setFromEntityId(fromEntityId);
			}
			transferDoneMarker.setFromLocalRevision(fromLocalRevision);
			dao.makePersistent(transferDoneMarker);

			transaction.commit();
		}
	}

	@Override
	public Set<String> getFileInProgressPaths(final UUID fromRepository, final UUID toRepository) {
		try (final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction();) {
			final FileInProgressMarkerDao dao = transaction.getDao(FileInProgressMarkerDao.class);
			final Collection<FileInProgressMarker> fileInProgressMarkers = dao.getFileInProgressMarkers(fromRepository, toRepository);
			final Set<String> paths = new HashSet<String>(fileInProgressMarkers.size());
			for (final FileInProgressMarker fileInProgressMarker : fileInProgressMarkers)
				paths.add(fileInProgressMarker.getPath());

			transaction.commit();
			return paths;
		}
	}

	@Override
	public void markFileInProgress(final UUID fromRepository, final UUID toRepository, final String path, final boolean inProgress) {
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) {
			final FileInProgressMarkerDao dao = transaction.getDao(FileInProgressMarkerDao.class);
			FileInProgressMarker fileInProgressMarker = dao.getFileInProgressMarker(fromRepository, toRepository, path);

			if (fileInProgressMarker == null && inProgress) {
				fileInProgressMarker = new FileInProgressMarker();
				fileInProgressMarker.setFromRepositoryId(fromRepository);
				fileInProgressMarker.setToRepositoryId(toRepository);
				fileInProgressMarker.setPath(path);
				dao.makePersistent(fileInProgressMarker);
				logger.info("Storing fileInProgressMarker: {} on repo={}", fileInProgressMarker, getRepositoryId());
			} else if (fileInProgressMarker != null && !inProgress) {
				logger.info("Removing fileInProgressMarker: {} on repo={}", fileInProgressMarker, getRepositoryId());
				dao.deletePersistent(fileInProgressMarker);
			}  else
				logger.warn("Unexpected state: markFileInProgress==null='{}', inProgress='{}' on repo={}", fileInProgressMarker == null, inProgress, getRepositoryId());

			transaction.commit();
		}
	}

	@Override
	public void putParentConfigPropSetDto(ConfigPropSetDto parentConfigPropSetDto) {
		try ( final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); ) { // we open a write-transaction merely for the exclusive lock
			final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(getClientRepositoryIdOrFail());
			if (! remoteRepository.getLocalPathPrefix().isEmpty()) {
				logger.warn("putParentConfigPropSetDto: IGNORING unsupported situation! See: https://github.com/cloudstore/cloudstore/issues/58");
				return;
			}

			final File metaDir = getLocalRepoManager().getLocalRoot().createFile(LocalRepoManager.META_DIR_NAME);
			if (! metaDir.isDirectory())
				throw new IOException("Directory does not exist: " + metaDir);

			final File repoParentConfigFile = metaDir.createFile(Config.PROPERTIES_FILE_NAME_PARENT_PREFIX + getClientRepositoryIdOrFail() + Config.PROPERTIES_FILE_NAME_SUFFIX);

			if (parentConfigPropSetDto.getConfigPropDtos().isEmpty()) {
				repoParentConfigFile.delete();
				if (repoParentConfigFile.isFile())
					throw new IOException("Deleting file failed: " + repoParentConfigFile);
			}
			else {
				Properties properties = parentConfigPropSetDto.toProperties();
				PropertiesUtil.store(repoParentConfigFile, properties, null);
			}

			mergeRepoParentConfigFiles();

			transaction.commit();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void mergeRepoParentConfigFiles() throws IOException {
		final File metaDir = getLocalRepoManager().getLocalRoot().createFile(LocalRepoManager.META_DIR_NAME);

		final Properties properties = new Properties();
		for (File configFile : getRepoParentConfigFiles()) {
			try (InputStream in = castStream(configFile.createInputStream())) {
				properties.load(in);
			}
		}

		final File parentConfigFile = metaDir.createFile(Config.PROPERTIES_FILE_NAME_PARENT);
		if (properties.isEmpty()) {
			parentConfigFile.delete();
			if (parentConfigFile.isFile())
				throw new IOException("Deleting file failed: " + parentConfigFile);
		}
		else
			PropertiesUtil.store(parentConfigFile, properties, null);
	}

	private List<File> getRepoParentConfigFiles() {
		final List<File> result = new ArrayList<>();
		final File metaDir = getLocalRepoManager().getLocalRoot().createFile(LocalRepoManager.META_DIR_NAME);

		final Pattern repoParentConfigPattern = Pattern.compile(
				Pattern.quote(Config.PROPERTIES_FILE_NAME_PARENT_PREFIX) + "[^.]*" + Pattern.quote(Config.PROPERTIES_FILE_NAME_SUFFIX));

		Matcher repoParentConfigMatcher = null;
		for (File file : metaDir.listFiles()) {
			if (repoParentConfigMatcher == null)
				repoParentConfigMatcher = repoParentConfigPattern.matcher(file.getName());
			else
				repoParentConfigMatcher.reset(file.getName());

			if (repoParentConfigMatcher.matches() && file.isFile())
				result.add(file);
		}

		Collections.sort(result, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		return result;
	}

	@Override
	public VersionInfoDto getVersionInfoDto() {
		return VersionInfoProvider.getInstance().getVersionInfoDto();
	}
}
