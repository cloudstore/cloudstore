package co.codewizards.cloudstore.core.repo.sync;

import static java.util.Objects.*;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

public class DoneMarker implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(DoneMarker.class);

	private final File doneFile;
	private final DataOutputStream doneOutputStream;
	private final Map<Long, Long> doneEntityId2LocalRevision;

	public DoneMarker(File doneFile) {
		this.doneFile = requireNonNull(doneFile);
		doneEntityId2LocalRevision = readDoneEntityId2LocalRevision();
		try {
			doneOutputStream = new DataOutputStream(new FileOutputStream(doneFile.getIoFile(), true));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public File getDoneFile() {
		return doneFile;
	}

	private Map<Long, Long> readDoneEntityId2LocalRevision() {
		Map<Long, Long> doneEntityId2LocalRevision = new HashMap<Long, Long>();
		if (doneFile.exists()) {
			try {
				try (FileInputStream fin = new FileInputStream(doneFile.getIoFile())) {
					DataInputStream in = new DataInputStream(new BufferedInputStream(fin));
					while (true) {
						in.mark(3);
						if (in.read() < 0)
							break;

						in.reset();
						long entityId = in.readLong();
						long localRevision = in.readLong();
						Long oldLocalRevision = doneEntityId2LocalRevision.put(entityId, localRevision);
						if (oldLocalRevision != null)
							logger.warn("readDoneEntityId2LocalRevision: Multiple entries of entityId={} in doneFile '{}'. Replacing localRevision {} by {}.",
									entityId, doneFile.getAbsolutePath(), oldLocalRevision, localRevision);
					}
				}
			} catch (IOException e) {
				logger.error("readDoneEntityId2LocalRevision: Reading doneFile '" + doneFile.getAbsolutePath() + "' failed! Deleting this file, now.", e);
				doneFile.deleteRecursively(); // maybe it was a directory
			}
		}
		return doneEntityId2LocalRevision;
	}

	@Override
	public void close() {
		try {
			doneOutputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void markDone(long entityId, long localRevision) {
		final Long oldLocalRevision = doneEntityId2LocalRevision.put(entityId, localRevision);
		if (oldLocalRevision != null && localRevision == oldLocalRevision.longValue()) {
			logger.warn("markDone: Entity with id={} was already marked as done for localRevision={}! Skipping.",
					entityId, localRevision);
			return;
		}
		try {
			doneOutputStream.writeLong(entityId);
			doneOutputStream.writeLong(localRevision);
			doneOutputStream.flush();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
		if (oldLocalRevision != null)
			logger.warn("markDone: Entity with id={} was already marked as done for localRevision={}, but marking it done again for localRevision={}, now.",
					entityId, oldLocalRevision, localRevision);
	}

	public boolean isDone(long entityId, long localRevision) {
		final Long doneLocalRevision = doneEntityId2LocalRevision.get(entityId);
		if (doneLocalRevision == null)
			return false;

		if (localRevision == doneLocalRevision.longValue())
			return true;

		logger.warn("isDone: Entity with id={} is already marked as done for localRevision={}, but not for localRevision={}, thus returning false.",
				entityId, doneLocalRevision, localRevision);

		return false;
	}
}
