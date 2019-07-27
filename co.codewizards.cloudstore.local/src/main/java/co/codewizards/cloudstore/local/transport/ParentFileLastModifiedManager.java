package co.codewizards.cloudstore.local.transport;

import static java.util.Objects.*;

import java.util.HashMap;
import java.util.Map;

import co.codewizards.cloudstore.core.oio.File;

public class ParentFileLastModifiedManager {
	private static class ParentFileEntry {
		public final File parentFile;
		public final long lastModified;
		public int refCount = 0;

		public ParentFileEntry(final File parentFile) {
			this.parentFile = requireNonNull(parentFile, "parentFile");
			this.lastModified = parentFile.exists() ? parentFile.lastModified() : Long.MIN_VALUE;
		}
	}

	private static final class ParentFileLastModifiedManagerHolder {
		public static final ParentFileLastModifiedManager instance = new ParentFileLastModifiedManager();
	}

	private final Map<File, ParentFileEntry> parentFile2ParentFileEntry = new HashMap<File, ParentFileEntry>();

	private ParentFileLastModifiedManager() { }

	public static ParentFileLastModifiedManager getInstance() {
		return ParentFileLastModifiedManagerHolder.instance;
	}

	public synchronized void backupParentFileLastModified(final File parentFile) {
		requireNonNull(parentFile, "parentFile");
		ParentFileEntry parentFileEntry = parentFile2ParentFileEntry.get(parentFile);
		if (parentFileEntry == null) {
			parentFileEntry = new ParentFileEntry(parentFile);
			parentFile2ParentFileEntry.put(parentFile, parentFileEntry);
		}
		++parentFileEntry.refCount;
	}

	public synchronized void restoreParentFileLastModified(final File parentFile) {
		requireNonNull(parentFile, "parentFile");
		final ParentFileEntry parentFileEntry = parentFile2ParentFileEntry.get(parentFile);
		if (parentFileEntry == null)
			throw new IllegalStateException("parentFileEntry == null :: less invocations of restore... than of backup...!!! :: parentFile=" + parentFile);

		if (--parentFileEntry.refCount == 0) {
			if (parentFileEntry.lastModified != Long.MIN_VALUE)
				parentFileEntry.parentFile.setLastModified(parentFileEntry.lastModified);

			parentFile2ParentFileEntry.remove(parentFile);
		}
	}
}
