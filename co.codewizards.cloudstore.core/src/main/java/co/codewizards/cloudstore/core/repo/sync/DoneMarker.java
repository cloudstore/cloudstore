package co.codewizards.cloudstore.core.repo.sync;

import static java.util.Objects.*;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.LongUtil;

public class DoneMarker implements Closeable {

	private static final String LOCAL_REVISION_FILE_NAME = "localRevision.bin";

	private static final Logger logger = LoggerFactory.getLogger(DoneMarker.class);

	private final File doneDir;

	public DoneMarker(File doneDir) {
		this.doneDir = requireNonNull(doneDir, "doneDir");

		if (doneDir.isFile()) {
			logger.error("doneDir '{}' is a normal file, but should be a directory. Deleting it now.", doneDir.getAbsolutePath());
			doneDir.deleteRecursively();
		}

		if (! doneDir.isDirectory()) {
			doneDir.mkdir();

			if (! doneDir.isDirectory())
				throw new RuntimeException(
						new IOException(
								String.format("Directory '%s' could not be created! Check permissions and available space/inodes.", doneDir.getAbsolutePath())));
		}
	}

	public File getDoneDir() {
		return doneDir;
	}

	@Override
	public void close() {
	}

	public void markDone(final long entityId, final long localRevision) {
		final String[] entityIdHexSegments = LongUtil.toBytesHex(entityId, true);

		File entityIdDir = doneDir;
		for (String segment : entityIdHexSegments) {
			entityIdDir = entityIdDir.createFile(segment);

			if (! entityIdDir.isDirectory()) {
				entityIdDir.mkdir();

				if (! entityIdDir.isDirectory())
					throw new RuntimeException(
							new IOException(
									String.format("Directory '%s' could not be created! Check permissions and available space/inodes.", entityIdDir.getAbsolutePath())));
			}
		}

		final File localRevisionFile = entityIdDir.createFile(LOCAL_REVISION_FILE_NAME);

		try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(localRevisionFile.getIoFile()))) {
			out.writeLong(localRevision);
		} catch (IOException x) {
			throw new RuntimeException(String.format("Failed writing file '%s'!", localRevisionFile.getAbsolutePath()), x);
		}
	}

	public boolean isDone(final long entityId, final long localRevision) {
		final String[] entityIdHexSegments = LongUtil.toBytesHex(entityId, true);

		File entityIdDir = doneDir;
		for (String segment : entityIdHexSegments)
			entityIdDir = entityIdDir.createFile(segment);

		if (! entityIdDir.isDirectory())
			return false;

		final File localRevisionFile = entityIdDir.createFile(LOCAL_REVISION_FILE_NAME);
		if (! localRevisionFile.isFile())
			return false;

		if (localRevisionFile.length() < 8) {
			logger.warn("isDone: File '{}' exists, but contains less than 8 bytes!",
					localRevisionFile.getAbsolutePath());

			return false;
		}

		try (final DataInputStream in = new DataInputStream(new FileInputStream(localRevisionFile.getIoFile()))) {
			final long oldLocalRevision = in.readLong();
			if (oldLocalRevision == localRevision)
				return true;

			logger.warn("isDone: Entity with id={} is already marked as done for localRevision={}, but not for localRevision={}, thus returning false.",
					entityId, oldLocalRevision, localRevision);
		} catch (IOException x) {
			throw new RuntimeException(String.format("Failed reading file '%s'!", localRevisionFile.getAbsolutePath()), x);
		}
		return false;
	}

}
