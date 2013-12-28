package co.codewizards.cloudstore.shared.persistence;

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
}
