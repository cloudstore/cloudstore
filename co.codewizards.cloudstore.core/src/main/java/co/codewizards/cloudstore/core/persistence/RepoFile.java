package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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

@PersistenceCapable
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP)
@Unique(name="RepoFile_parent_name", members={"parent", "name"})
@Indices({
	@Index(name="RepoFile_parent", members={"parent"}),
	@Index(name="RepoFile_localRevision", members={"localRevision"}),
})
@Queries({
	@Query(name="getChildRepoFile_parent_name", value="SELECT UNIQUE WHERE this.parent == :parent && this.name == :name"),
	@Query(name="getChildRepoFiles_parent", value="SELECT WHERE this.parent == :parent"),
	@Query(name="getRepoFilesChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision"),
})
public abstract class RepoFile extends Entity implements AutoTrackLocalRevision {
	private static final Logger logger = LoggerFactory.getLogger(RepoFile.class);

	private RepoFile parent;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String name;

	private long localRevision;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private Date lastModified;

	public RepoFile getParent() {
		return parent;
	}
	public void setParent(RepoFile parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
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
	public void setLocalRevision(long localRevision) {
		if (this.localRevision != localRevision) {
			if (logger.isDebugEnabled()) {
				LocalRepository localRepository = new LocalRepositoryDAO().persistenceManager(JDOHelper.getPersistenceManager(this)).getLocalRepositoryOrFail();
				logger.debug("setLocalRevision: localRepositoryID={} path='{}' old={} new={}", localRepository.getEntityID(), getPath(), this.localRevision, localRevision);
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
		LinkedList<RepoFile> path = new LinkedList<RepoFile>();
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
	 * The path's elements are separated by a slash ("/").
	 * @return the path from the root to <code>this</code>. Never <code>null</code>. The repository's root itself has the path "/".
	 */
	public String getPath() {
		StringBuilder sb = new StringBuilder();
		for (RepoFile repoFile : getPathList()) {
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
	public File getFile(File localRoot) {
		assertNotNull("localRoot", localRoot);
		File result = localRoot;
		for (RepoFile repoFile : getPathList()) {
			if (repoFile.getParent() == null) // skip the root
				continue;

			result = new File(result, repoFile.getName());
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
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
}
