package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.oio.api.File;

@PersistenceCapable
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP)
@Unique(name="RepoFile_parent_name", members={"parent", "name"})
@Indices({
	@Index(name="RepoFile_parent", members={"parent"}),
	@Index(name="RepoFile_localRevision", members={"localRevision"})
})
@Queries({
	@Query(name="getChildRepoFile_parent_name", value="SELECT UNIQUE WHERE this.parent == :parent && this.name == :name"),
	@Query(name="getChildRepoFiles_parent", value="SELECT WHERE this.parent == :parent"),
	@Query(
			name="getRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)") // TODO this necessary == null is IMHO a DN bug!
})
public abstract class RepoFile extends Entity implements AutoTrackLocalRevision {
	private static final Logger logger = LoggerFactory.getLogger(RepoFile.class);

	private RepoFile parent;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String name;

	private long localRevision;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private Date lastModified;

	// TODO 1: The direct partner-repository from which this was synced, should be a real relation to the RemoteRepository,
	// because this is more efficient (not a String, but a long id).
	// TODO 2: We should additionally store (and forward) the origin repositoryId (UUID/String) to use this feature during
	// circular syncs over multiple repos - e.g. repoA ---> repoB ---> repoC ---> repoA (again) - this circle would currently
	// cause https://github.com/cloudstore/cloudstore/issues/25 again (because issue 25 is only solved for direct partners - not indirect).
	// TODO 3: We should switch from UUID to Uid everywhere (most importantly the repositoryId).
	// Careful, though: Uid's String-representation is case-sensitive! Due to Windows, it must thus not be used for file names!
	private String lastSyncFromRepositoryId;

	public RepoFile getParent() {
		return parent;
	}
	public void setParent(final RepoFile parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this does not include modifications of children (in case this is a directory).
	 * If a child is modified, solely this child's localRevision is updated.
	 */
	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (this.localRevision != localRevision) {
			if (logger.isDebugEnabled()) {
				final LocalRepository localRepository = new LocalRepositoryDao().persistenceManager(JDOHelper.getPersistenceManager(this)).getLocalRepositoryOrFail();
				logger.debug("setLocalRevision: localRepositoryId={} path='{}' old={} new={}", localRepository.getRepositoryId(), getPath(), this.localRevision, localRevision);
			}
			this.localRevision = localRevision;
		}
	}

	/**
	 * Gets the path within the repository from the {@link LocalRepository#getRoot() root} (including) to <code>this</code> (including).
	 * <p>
	 * The first element in the list is the {@code root}. The last element is <code>this</code>.
	 * <p>
	 * If this method is called on the {@code root} itself, the result will be a list with one single element (the root itself).
	 * @return the path within the repository from the {@link LocalRepository#getRoot() root} (including) to <code>this</code> (including). Never <code>null</code>.
	 */
	public List<RepoFile> getPathList() {
		final LinkedList<RepoFile> path = new LinkedList<RepoFile>();
		RepoFile rf = this;
		while (rf != null) {
			path.addFirst(rf);
			rf = rf.getParent();
		}
		return Collections.unmodifiableList(path);
	}

	/**
	 * Gets the path from the root to <code>this</code>.
	 * <p>
	 * The path's elements are separated by a slash ("/"). The path starts with a slash (like an absolute path), but
	 * is relative to the repository's local root.
	 * @return the path from the root to <code>this</code>. Never <code>null</code>. The repository's root itself has the path "/".
	 */
	public String getPath() {
		final StringBuilder sb = new StringBuilder();
		for (final RepoFile repoFile : getPathList()) {
			if (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')
				sb.append('/');

			sb.append(repoFile.getName());
		}
		return sb.toString();
	}

	/**
	 * Gets the {@link File} represented by this {@link RepoFile} inside the given repository's {@code localRoot} directory.
	 * @param localRoot the repository's root directory.
	 * @return the {@link File} represented by this {@link RepoFile} inside the given repository's {@code localRoot} directory.
	 */
	public File getFile(final File localRoot) {
		assertNotNull("localRoot", localRoot);
		File result = localRoot;
		for (final RepoFile repoFile : getPathList()) {
			if (repoFile.getParent() == null) // skip the root
				continue;

			result = createFile(result, repoFile.getName());
		}
		return result;
	}

	/**
	 * Gets the timestamp of the file's last modification.
	 * <p>
	 * It reflects the {@link File#lastModified() File.lastModified} property.
	 * @return the timestamp of the file's last modification.
	 */
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(final Date lastModified) {
		this.lastModified = lastModified;
	}

	public UUID getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId == null ? null : UUID.fromString(lastSyncFromRepositoryId);
	}
	public void setLastSyncFromRepositoryId(final UUID repositoryId) {
		this.lastSyncFromRepositoryId = repositoryId == null ? null : repositoryId.toString();
	}
}
