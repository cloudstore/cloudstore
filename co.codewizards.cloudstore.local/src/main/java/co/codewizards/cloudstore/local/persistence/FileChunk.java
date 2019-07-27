package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.LoadCallback;
import javax.jdo.listener.StoreCallback;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="FileChunk_normalFile_offset", members={"normalFile", "offset"})
public class FileChunk extends Entity implements Comparable<FileChunk>, StoreCallback, LoadCallback {

	@NotPersistent
	private boolean writable = true;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private NormalFile normalFile;

	private long offset;

	private int length;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String sha1;

	public NormalFile getNormalFile() {
		return normalFile;
	}
	public void setNormalFile(final NormalFile normalFile) {
		assertWritable();

		if (! equal(this.normalFile, normalFile))
			this.normalFile = normalFile;
	}
	public long getOffset() {
		return offset;
	}
	public void setOffset(final long offset) {
		assertWritable();

		if (! equal(this.offset, offset))
			this.offset = offset;
	}
	public int getLength() {
		return length;
	}
	public void setLength(final int length) {
		assertWritable();

		if (! equal(this.length, length))
			this.length = length;
	}
	public String getSha1() {
		return sha1;
	}
	public void setSha1(final String sha1) {
		assertWritable();

		if (! equal(this.sha1, sha1))
			this.sha1 = sha1;
	}

	protected void assertWritable() {
		if (!writable)
			throw new IllegalStateException("This instance is read-only!");
	}

	public void makeReadOnly() {
		writable = false;
	}

	protected void makeWritable() {
		writable = true;
	}

	@Override
	public void jdoPreStore() {
		makeReadOnly();
	}
	@Override
	public void jdoPostLoad() {
		makeReadOnly();
	}

	@Override
	public int compareTo(final FileChunk other) {
		requireNonNull(other, "other");

		if (this.normalFile != other.normalFile) {
			final long thisRepoFileId = this.normalFile == null ? 0 : this.normalFile.getId();
			final long otherRepoFileId = other.normalFile == null ? 0 : other.normalFile.getId();

			final int result = compare(thisRepoFileId, otherRepoFileId);
			if (result != 0)
				return result;
		}

		final int result = compare(this.offset, other.offset);
		if (result != 0)
			return result;

		final long thisId = this.getId();
		final long otherId = other.getId();

		return compare(thisId, otherId);
	}

	private static int compare(final long x, final long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
