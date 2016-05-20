package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.Column;
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
 * While a file is in progress of a sync, an instance of FileInProgressMarker is stored.
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
@Unique(name = "FileInProgressMark_fromRepositoryId_toRepositoryId_pathSha1", members = { "fromRepositoryId", "toRepositoryId", "pathSha1" })
@Queries({
	@Query(name = "getFileInProgressMarkers_fromRepositoryId_toRepositoryId",
			value = "SELECT WHERE this.fromRepositoryId == :fromRepositoryId && this.toRepositoryId == :toRepositoryId"),
	@Query(name = "getFileInProgressMarker_fromRepositoryId_toRepositoryId_pathSha1",
			value = "SELECT UNIQUE WHERE this.fromRepositoryId == :fromRepositoryId && this.toRepositoryId == :toRepositoryId && this.pathSha1 == :pathSha1")
})
public class FileInProgressMarker extends Entity {

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String fromRepositoryId;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String toRepositoryId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String pathSha1;

	@Persistent(nullValue = NullValue.EXCEPTION, defaultFetchGroup = "true")
	@Column(jdbcType="CLOB")
	private String path;

	public UUID getFromRepositoryId() {
		return FileInProgressMarkerDao.convertToUuid(fromRepositoryId);
	}

	public void setFromRepositoryId(final UUID fromRepositoryId) {
		if (! equal(this.getFromRepositoryId(), fromRepositoryId))
			this.fromRepositoryId = FileInProgressMarkerDao.convertToString(fromRepositoryId);
	}

	public UUID getToRepositoryId() {
		return FileInProgressMarkerDao.convertToUuid(toRepositoryId);
	}

	public void setToRepositoryId(final UUID toRepositoryId) {
		if (! equal(this.getToRepositoryId(), toRepositoryId))
			this.toRepositoryId = FileInProgressMarkerDao.convertToString(toRepositoryId);
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.pathSha1 = sha1(path);
		this.path = path;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]{fromRepositoryId=\"%s\", toRepositoryId=\"%s\", path=\"%s\"}",
				getClass().getSimpleName(), JDOHelper.getObjectId(this),
				fromRepositoryId, toRepositoryId, path);
	}

}
