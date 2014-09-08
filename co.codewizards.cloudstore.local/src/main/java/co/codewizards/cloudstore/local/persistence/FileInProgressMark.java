package co.codewizards.cloudstore.local.persistence;

import java.util.UUID;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

/**
 * While a file is in progress of a sync, an instance of FileInProgressMark is stored.
 * <p/>
 * An db entry of this class is describing the connection (from, to) and the path of one file synchronisation, that is
 * just in progress. If this file is transferred successfully, this instance should be removed.
 * <p>
 * After a successful sync run in the repository, there should be no entry for this connection (fromRepositoryId to
 * toRepositoryId) all. If there is an entity left, it is assumed, that this connection got interrupted while syncing
 * this file.
 *
 * @author Sebastian Schefczyk
 */
@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Index(
		name="FileInProgressMark_fromRepositoryId_toRepositoryId",
		members={"fromRepositoryId", "toRepositoryId"})
@Unique(name = "FileInProgressMark_fromRepositoryId_toRepositoryId_path", members = { "fromRepositoryId", "toRepositoryId",
		"path" })
@Queries({
		@Query(name = "getFileInProgressMarks", value = "SELECT WHERE this.fromRepositoryId == :fromRepositoryId && this.toRepositoryId == :toRepositoryId"),
		@Query(name = "getFileInProgressMark", value = "SELECT UNIQUE WHERE this.fromRepositoryId == :fromRepositoryId && this.toRepositoryId == :toRepositoryId && this.path == :path") })
public class FileInProgressMark extends Entity {

	@Persistent(nullValue = NullValue.EXCEPTION, defaultFetchGroup = "true")
	private String fromRepositoryId;

	@Persistent(nullValue = NullValue.EXCEPTION, defaultFetchGroup = "true")
	private String toRepositoryId;

	@Persistent(nullValue = NullValue.EXCEPTION, defaultFetchGroup = "true")
	private String path;

	public UUID getFromRepositoryId() {
		return FileInProgressMarkDao.convertToUuid(fromRepositoryId);
	}

	public void setFromRepositoryId(final UUID fromRepositoryId) {
		this.fromRepositoryId = FileInProgressMarkDao.convertToString(fromRepositoryId);
	}

	public UUID getToRepositoryId() {
		return FileInProgressMarkDao.convertToUuid(toRepositoryId);
	}

	public void setToRepositoryId(final UUID toRepositoryId) {
		this.toRepositoryId = FileInProgressMarkDao.convertToString(toRepositoryId);
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return "fromRepositoryId=\"" + fromRepositoryId + "\", " + "toRepositoryId=\"" + toRepositoryId + "\", "
				+ "path=\"" + path + "\"";
	}

}
