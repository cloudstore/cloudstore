package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;

/**
 * Helper for testing behaviour of writing chunks, before appending/moving them on completion to destination file.
 *
 * @author Sebastian Schefczyk
 */
public class FileWatcher {

	/**
	 * <b>This is a hack!</b> If set to true, its a tiny hack which influences the environment. There will be a
	 * temporary directory to gather transfered chunks. This will get created at the beginning of the sync. If we watch
	 * for that creation and register a second file watcher in that tmpDir, it could happen, that the first file(s) were
	 * already created/modified/deleted in that tempDir, before the watcher starts to get notified and the test will
	 * fail (in more detail: the watcher will not return on {@link WatchService#take()} and the
	 * {@link Future#get(long, TimeUnit)} call will cause its {@link TimeoutException}). The simple creation of that
	 * tmpDir should be tested in another test.
	 * <p>
	 * Setting this to true is much safer for the test. Setting to false is maximal realistic, but suffers under the
	 * thread race.
	 */
	private static final boolean HACKY_CREATION_OF_TEMP_DIR = true;
	private static final String CLOUDSTORE_TMP = ".cloudstore-tmp";
	/**
	 * 1,048,576 bytes = 1 MiB chunk size
	 */
	private static final int CHUNK_SIZE = 1024 * 1024;
	/**
	 * Observation on desktop PC: If executed iteratively, 10s was enough; if executed in parallel with mvn 15s was
	 * needed.
	 */
	private static final int TIMEOUT_SYNC = 20;

	private WatchService watcherParentDir;
	private WatchService watcherTempDir;
	private Path parentDir;
	private Path tempDir = null;
	private long cumLength = 0;
	private String fileName;

	public FileWatcher(final File parentDirFile, final String fileName, final long fileLength) throws IOException {
		this.fileName = fileName;
		this.cumLength = fileLength;
		this.parentDir = parentDirFile.getIoFile().toPath();
		this.watcherParentDir = parentDir.getFileSystem().newWatchService();
		this.parentDir.register(watcherParentDir, StandardWatchEventKinds.ENTRY_CREATE);
	}

	/** Will throw Exception after one chunk is written! */
	public void syncOneChunk(final RepoToRepoSync repoToRepoSync, final LoggerProgressMonitor monitor)
			throws TimeoutException, ExecutionException {
		final SyncTask syncTask = new SyncTask(repoToRepoSync, monitor);
		final ExecutorService threadExecutor = Executors.newFixedThreadPool(3);

		try {
			final Future<Void> watchTaskTempDirCreationFuture = threadExecutor.submit(new WatchTaskTempDirCreation());
			Thread.sleep(200);
			// Thread.sleep: before syncing, enforce the
			// watchTaskFirstWrittenChunk is prepared! I have often seen
			// TimeOutExceptions, because the sync does create the tmpDir
			// folder, inside there creates chunk files, modify and delete them
			// and the watchTaskFirstWrittenChunk missed the events. Most
			// probably this will occur more often on fast file systems on SSDs.
			threadExecutor.submit(syncTask);
			watchTaskTempDirCreationFuture.get(TIMEOUT_SYNC, TimeUnit.SECONDS);
			final Future<Void> watchTaskFirstWrittenFuture = threadExecutor.submit(new WatchTaskFirstWrittenChunk());
			watchTaskFirstWrittenFuture.get(TIMEOUT_SYNC, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			// This exception is thrown if the child thread is interrupted.
			throw new RuntimeException(e);
		} finally {
			threadExecutor.shutdown();
		}
	}

	public void createDeleteChunks(final RepoToRepoSync repoToRepoSync, final LocalRepoManager localRepoManager,
			final LoggerProgressMonitor monitor, final int toCreate, final int toDelete, final long newFileLength)
			throws TimeoutException, ExecutionException, IOException, InterruptedException {
		this.cumLength = newFileLength;
		createDeleteChunks(repoToRepoSync, localRepoManager, monitor, toCreate, toDelete);
	}

	public void createDeleteChunks(final RepoToRepoSync repoToRepoSync, final LocalRepoManager localRepoManager,
			final LoggerProgressMonitor monitor, final int toCreate, final int toDelete, final String newFileName)
			throws TimeoutException, ExecutionException, IOException, InterruptedException {
		this.fileName = newFileName;
		createDeleteChunks(repoToRepoSync, localRepoManager, monitor, toCreate, toDelete);
	}

	/**
	 * Return after a the defined number of chunk creations and deletions (used after
	 * {@link #syncOneChunk(RepoToRepoSync, LoggerProgressMonitor)}, where one chunk was already created) and the setup
	 * of the tempDir already happened!
	 *
	 * @throws FileWatcherException
	 */
	public void createDeleteChunks(final RepoToRepoSync repoToRepoSync, final LocalRepoManager localRepoManager,
			final LoggerProgressMonitor monitor, final int toCreate, final int toDelete) throws TimeoutException,
			ExecutionException, IOException, InterruptedException {
		assertThat(tempDir).isNotNull();
		assertThat(watcherTempDir).isNotNull();
		// redefine registration for parentDir: add MODIFY for watching
		// completion of the destination file
		this.parentDir.register(watcherParentDir, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY);

		final WatchTaskChunksCreatedDeleted watchTaskCreatedDeleted = new WatchTaskChunksCreatedDeleted(toCreate,
				toDelete);
		final WatchTaskChunksToFile watchTaskChunksToFile = new WatchTaskChunksToFile();
		final SyncTask syncTask = new SyncTask(repoToRepoSync, monitor);
		final ExecutorService threadExecutor = Executors.newFixedThreadPool(3);

		Future<Void> syncTaskFuture = null;
		Future<Void> fileCompleteFuture = null;
		try {
			threadExecutor.submit(watchTaskCreatedDeleted);
			fileCompleteFuture = threadExecutor.submit(watchTaskChunksToFile);
			syncTaskFuture = threadExecutor.submit(syncTask);
			// If Future.get returns, the file is complete, so the handling of
			// the chunks must also be finished!
			fileCompleteFuture.get(TIMEOUT_SYNC, TimeUnit.SECONDS);
			// Let the sync go on and complete, so the file watcher can look for
			// further chunks, which must not be created, and assert the correct
			// amount at the end.
			syncTaskFuture.get(TIMEOUT_SYNC, TimeUnit.SECONDS);
			Thread.sleep(500); // wait for the last I/O operations from syncTask (sometimes deletion of chunks was not
								// yet counted)
			final boolean hasCorrectAmountOfCreationsDeletions = watchTaskCreatedDeleted
					.hasCorrectAmountOfCreationsDeletions();
			assertThat(hasCorrectAmountOfCreationsDeletions).isTrue();
		} finally {
			threadExecutor.shutdown();
		}
	}

	public void watchSyncOrder(final RepoToRepoSync repoToRepoSync, final LocalRepoManager localRepoManagerLocal,
			final LoggerProgressMonitor monitor, final String fileName1, final long length1, final String fileName0,
			final long length0, final String fileName2, final long length2) throws TimeoutException,
			ExecutionException, IOException, InterruptedException {
		// assertThat(tempDir).isNotNull();
		// assertThat(watcherTempDir).isNotNull();
		// redefine registration for parentDir: add MODIFY for watching
		// completion of the destination file
		this.parentDir.register(watcherParentDir, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY);

		final WatchTaskSyncOrder watchTaskSyncOrder = new WatchTaskSyncOrder(fileName1, length1, fileName0, length0,
				fileName2, length2);
		final SyncTask syncTask = new SyncTask(repoToRepoSync, monitor);
		final ExecutorService threadExecutor = Executors.newFixedThreadPool(3);

		Future<Void> syncTaskFuture = null;
		Future<Boolean> watchTaskSyncOrderFuture = null;
		try {
			watchTaskSyncOrderFuture = threadExecutor.submit(watchTaskSyncOrder);
			syncTaskFuture = threadExecutor.submit(syncTask);
			syncTaskFuture.get(TIMEOUT_SYNC, TimeUnit.SECONDS);
			final boolean isSyncOrderCorrect = watchTaskSyncOrderFuture.get(TIMEOUT_SYNC, TimeUnit.SECONDS);
			assertThat(isSyncOrderCorrect).isTrue();
		} finally {
			threadExecutor.shutdown();
		}
	}

	/** Manually delete the tempDir recursively and resets watchers. */
	public void deleteTempDir() throws TimeoutException, ExecutionException, IOException {
		assertThat(tempDir).isNotNull();
		createFile(tempDir.toFile()).deleteRecursively();
		// as there is no sync ongoing we create it directly
		doHackyCreationOfTempDir();
	}

	private void doHackyCreationOfTempDir() throws IOException {
		assertThat(tempDir.toFile()).doesNotExist();
		tempDir.toFile().mkdir();
		tempDir.register(watcherTempDir, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_CREATE);
	}

	/**
	 * Calling repoToRepoSync.sync(monitor); call this if all FileWatcher are prepared.
	 */
	class SyncTask implements Callable<Void> {

		private RepoToRepoSync repoToRepoSync;
		private LoggerProgressMonitor monitor;

		public SyncTask(final RepoToRepoSync repoToRepoSync, final LoggerProgressMonitor monitor) {
			this.repoToRepoSync = repoToRepoSync;
			this.monitor = monitor;
		}

		@Override
		public Void call() throws Exception {
			repoToRepoSync.sync(monitor);
			return null;
		}
	}

	/**
	 * This either is adding a real watcher, waits for the creation of the tmpDir OR it is creating this directly, as
	 * this more robust to thread races.
	 */
	class WatchTaskTempDirCreation implements Callable<Void> {

		@Override
		public Void call() throws IOException {
			assertThat(parentDir).isNotNull();
			tempDir = parentDir.resolve(CLOUDSTORE_TMP);
			watcherTempDir = tempDir.getFileSystem().newWatchService();

			// plz see jdoc on HACKY_CREATION_OF_TEMP_DIR ;
			if (HACKY_CREATION_OF_TEMP_DIR) {
				doHackyCreationOfTempDir();
				return null;
			}

			for (;;) {
				// wait for key to be signalled
				WatchKey key;
				try {
					key = watcherParentDir.take();
				} catch (final InterruptedException x) {
					throw new RuntimeException(x);
				}

				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					// Context for directory entry event is the file name of
					// entry
					final WatchEvent<Path> ev = cast(event);
					final Path name = ev.context();
					final Path child = parentDir.resolve(name);
					// print out event
					// System.err.format("_processEvents: %s: %s file \n",
					// kind.name(), child);
					if (name.endsWith(CLOUDSTORE_TMP)) {
						// CRITICAL MOMENT: register directly, before the first
						child.register(watcherTempDir, StandardWatchEventKinds.ENTRY_MODIFY,
								StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);
						// assertThat(child.toFile().listFiles().length).isEqualTo(0);
						System.err.println("watchForTempDirCreation: " + child.getFileName() + ", name=" + name);
						key.reset();
						return null;
					}
				}
				// reset key and remove from set if directory no longer
				// accessible
				key.reset();
			}
		}
	}

	class WatchTaskFirstWrittenChunk implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			System.err.println("WatchTaskFirstWrittenChunk started");
			for (;;) {
				// wait for key to be signalled
				WatchKey key;
				try {
					key = watcherTempDir.take();
				} catch (final InterruptedException x) {
					throw new RuntimeException(x);
				}

				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					if (kind == StandardWatchEventKinds.OVERFLOW) {
						System.err
								.println("watchForFirstWrittenChunkFileWrittenAndThrow: StandardWatchEventKinds.OVERFLOW");
						continue;
					}
					// Context for directory entry event is the file name of
					// entry
					final WatchEvent<Path> ev = cast(event);
					final Path name = ev.context();
					final Path child = tempDir.resolve(name);

					if (child.toFile().getAbsoluteFile().getName().endsWith(".xml")) {
						break; // skip xml files!
					}
					// print out event (uncomment for faster usage)
					// System.err.format("_processEvents: %s: %s file \n",
					// event.kind().name(), child);
					// System.err.println("FileWatcher: child.length: " +
					// child.toFile().getAbsoluteFile().length());
					// System.err.println("FileWatcher: child.name: " +
					// child.toFile().getAbsoluteFile().getName());
					if (child.toFile().getAbsoluteFile().length() >= CHUNK_SIZE) {
						System.err.println("FileWatcher: CHUNK_SIZE reached! "
								+ child.toFile().getAbsoluteFile().getName());
						key.reset();
						return null;
					}
				}
				// reset key and remove from set if directory no longer
				key.reset();
			}
		}
	}

	class WatchTaskChunksCreatedDeleted implements Callable<Boolean> {

		// private WatchService watcherTempDir;
		private int toBeCreated;
		private int toBeDeleted;
		private int chunksCreated = 0;
		private int chunksDeleted = 0;

		public WatchTaskChunksCreatedDeleted(final int toBeCreated, final int toBeDeleted) {
			this.toBeCreated = toBeCreated;
			this.toBeDeleted = toBeDeleted;
		}

		@Override
		public Boolean call() throws Exception {
			assertThat(tempDir).isNotNull();

			watcherTempDir = tempDir.getFileSystem().newWatchService();
			tempDir.register(watcherTempDir, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
			watchForCreationsDeletions();
			return hasCorrectAmountOfCreationsDeletions();
		}

		private boolean hasCorrectAmountOfCreationsDeletions() {
			System.err.println("hasCorrectAmountOfCreationsDeletions: " + "toBeCreated=" + toBeCreated
					+ ", chunksCreated=" + chunksCreated + ", toBeDeleted=" + toBeDeleted + ", chunksDeleted="
					+ chunksDeleted);
			return toBeCreated == chunksCreated && toBeDeleted == chunksDeleted;
		}

		private void watchForCreationsDeletions() throws IOException {
			for (;;) {
				// wait for key to be signalled
				WatchKey key;
				try {
					key = watcherTempDir.take();
				} catch (final InterruptedException x) {
					throw new RuntimeException(x);
				}

				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					// Context for directory entry event is the file name of
					// entry
					final WatchEvent<Path> ev = cast(event);
					final Path name = ev.context();
//					final Path child = tempDir.resolve(name);
					if (name.toString().endsWith(".xml")) {
						break; // skip xml files!
					}
//					System.err.format("_processEvents: %s: %s file \n", kind.name(), child);
					if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
						chunksCreated++;
						System.err.println("watchForCreationsDeletions: ENTRY_CREATE=" + name + ", chunksCreated=" + chunksCreated);
					} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
						chunksDeleted++;
						System.err.println("watchForCreationsDeletions: ENTRY_DELETE=" + name + ", chunksDeleted=" + chunksDeleted);
					} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
//						System.err.println("watchForCreationsDeletions: ENTRY_MODIFY=" + name + ", length="
//								+ child.toFile().length());
					}
					// the "too much" check:
					assertThat(chunksCreated).isLessThanOrEqualTo(toBeCreated);
					assertThat(chunksDeleted).isLessThanOrEqualTo(toBeDeleted);
				}
				// reset key and remove from set if directory no longer
				// accessible
				key.reset();
			}
		}
	}

	/**
	 * Scope of this task: The sync will create chunks with a max-size of CHUNK_SIZE. This watch task focuses on the
	 * moment when they get appended.
	 */
	class WatchTaskChunksToFile implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			assertThat(parentDir).isNotNull();
			assertThat(watcherParentDir).isNotNull();
			System.err.println("WatchTaskChunksToFile: ready");

			for (;;) {
				// wait for key to be signalled
				WatchKey key;
				try {
					key = watcherParentDir.take();
				} catch (final InterruptedException x) {
					throw new RuntimeException(x);
				}

				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					// Context for directory entry event is the file name of
					// entry
					final WatchEvent<Path> ev = cast(event);
					final Path name = ev.context();
					final Path child = parentDir.resolve(name);
					if (!child.toFile().getName().equals(fileName)) {
						break;
					}
					if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
						System.err.println("WatchTaskChunksToFile: ENTRY_CREATE=" + name);
						continue;
					}
					if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
						final long length = child.toFile().length();
//						System.err.println("WatchTaskChunksToFile: ENTRY_MODIFY=" + name + ", length=" + length);
						if (length == cumLength) {
							System.err.println("WatchTaskChunksToFile: length reached! length=" + length);
							key.reset();
							return null;
						}
						continue;
					}
				}
				// reset key and remove from set if directory no longer
				// accessible
				key.reset();
			}
		}
	}

	class WatchTaskSyncOrder implements Callable<Boolean> {

		private class FileDescriptor {

			String fileName;
			long length;

			FileDescriptor(final String fileName, final long length) {
				this.fileName = fileName;
				this.length = length;
			}
		}

		private ArrayList<FileDescriptor> fileList;

		public WatchTaskSyncOrder(final String fileName0, final long length0, final String fileName1,
				final long length1, final String fileName2, final long length2) {
			fileList = new ArrayList<FileDescriptor>();
			fileList.add(new FileDescriptor(fileName0, length0));
			fileList.add(new FileDescriptor(fileName1, length1));
			fileList.add(new FileDescriptor(fileName2, length2));
		}

		@Override
		public Boolean call() throws Exception {
			assertThat(parentDir).isNotNull();
			assertThat(watcherParentDir).isNotNull();
			final ListIterator<FileDescriptor> it = fileList.listIterator();
			FileDescriptor fd = it.next();
			System.err.println("WatchTaskSyncOrder: checking for name=" + fd.fileName + " and length=" + fd.length);
			for (;;) {
				// wait for key to be signalled
				WatchKey key;
				try {
					key = watcherParentDir.take();
				} catch (final InterruptedException x) {
					throw new RuntimeException(x);
				}

				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					// Context for directory entry event is the file name of
					// entry
					final WatchEvent<Path> ev = cast(event);
					final Path name = ev.context();
					final Path child = parentDir.resolve(name);
					if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
						final String currentChildFileName = child.toFile().getName();

						// has the sync switched to next file?
						if (!fd.fileName.equals(currentChildFileName)) {
							if (it.hasNext()) {
								fd = it.next();
								System.err.println("WatchTaskSyncOrder: checking for name=" + fd.fileName
										+ " and length=" + fd.length);
							} else
								return false;
						}
						if (!fd.fileName.equals(currentChildFileName)) {
							System.err.println("WatchTaskSyncOrder: wrong currentChildFileName=" + currentChildFileName
									+ ", expected=" + fd.fileName);
							key.reset();
							return false;
						}

						final long currentChildFileLength = child.toFile().length();
						System.err.println("WatchTaskSyncOrder: modified=" + currentChildFileName + ", length="
								+ currentChildFileLength);
						// as observed, even if the length is reached, there
						// will be 2 more modified-events; so we will not switch
						// the expected immediatly, but lazy;
						if (currentChildFileLength == fd.length) {
							System.err.println("WatchTaskSyncOrder: length reached! currentChildFileLength="
									+ currentChildFileLength + ", name=" + currentChildFileName);
							if (!it.hasNext()) {
								System.err.println("WatchTaskSyncOrder: finished all");
								return true;
							}
						}
					}
				}
				// reset key and remove from set if directory no longer
				// accessible
				key.reset();
			}
		}
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(final WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

}
