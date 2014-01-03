package co.codewizards.cloudstore.shared.persistence;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

	private RepoFile parent;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String name;

	private long localRevision;

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
	 * If a child is modified, solely this child's localRevision is updated. However, when a child is
	 * added or removed, the local-revision of the parent directory is modified, too!
	 */
	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(long revision) {
		this.localRevision = revision;
	}

	/**
	 * Gets the path within the repository from the {@link LocalRepository#getRoot() root} (excluding) to <code>this</code> (including).
	 * <p>
	 * The first element in the list is the RepoFile having the {@code root} as parent (not the root itself!).
	 * The last element is <code>this</code>.
	 * <p>
	 * If this method is called on the {@code root} itself, the result will be an empty list.
	 * @return the path within the repository from the {@link LocalRepository#getRoot() root} (excluding) to <code>this</code> (including).
	 */
	public List<RepoFile> getRepoFilePath() {
		LinkedList<RepoFile> path = new LinkedList<RepoFile>();
		RepoFile rf = this;
		while (rf.getParent() != null) { // ignore the root
			path.addFirst(rf);
			rf = rf.getParent();
		}
		return Collections.unmodifiableList(path);
	}

	/**
	 * Gets the {@link File} represented by this {@link RepoFile} inside the given repository's {@code localRoot} directory.
	 * @param localRoot the repository's root directory.
	 * @return the {@link File} represented by this {@link RepoFile} inside the given repository's {@code localRoot} directory.
	 */
	public File getFile(File localRoot) {
		assertNotNull("localRoot", localRoot);
		File result = localRoot;
		for (RepoFile repoFile : getRepoFilePath()) {
			result = new File(result, repoFile.getName());
		}
		return result;
	}
}
