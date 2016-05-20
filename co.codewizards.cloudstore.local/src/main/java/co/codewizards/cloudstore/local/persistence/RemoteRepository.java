package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;
import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.UrlUtil;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="RemoteRepository")
//@Index(name="RemoteRepository_remoteRoot", members="remoteRoot") // Indexing a CLOB with Derby throws an exception :-( [should be a warning, IMHO for portability reasons]
@Index(name="RemoteRepository_remoteRootSha1", members="remoteRootSha1")
@Queries({
	@Query(name="getRemoteRepository_repositoryId", value="SELECT UNIQUE WHERE this.repositoryId == :repositoryId"),
	@Query(name="getRemoteRepository_remoteRootSha1", value="SELECT UNIQUE WHERE this.remoteRootSha1 == :remoteRootSha1")
})
public class RemoteRepository extends Repository implements AutoTrackLocalRevision {
	private static final Logger logger = LoggerFactory.getLogger(RemoteRepository.class);

	@Column(jdbcType="CLOB")
	private URL remoteRoot;

	private String remoteRootSha1;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String localPathPrefix;

	public RemoteRepository() { }

	public RemoteRepository(final UUID repositoryId) {
		super(repositoryId);
	}

	public URL getRemoteRoot() {
		return remoteRoot;
	}

	public void setRemoteRoot(URL remoteRoot) {
		if (equal(this.getRemoteRoot(), remoteRoot))
			return;

		remoteRoot = UrlUtil.canonicalizeURL(remoteRoot);
		this.remoteRoot = remoteRoot;
		this.remoteRootSha1 = remoteRoot == null ? null : sha1(remoteRoot.toExternalForm());
	}

	public String getRemoteRootSha1() {
		return remoteRootSha1;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision)) {
			if (logger.isDebugEnabled())
				logger.debug("setLocalRevision: repositoryId={} old={} new={}", getRepositoryId(), this.localRevision, localRevision);

			this.localRevision = localRevision;
		}
	}

	public String getLocalPathPrefix() {
		return localPathPrefix;
	}
	public void setLocalPathPrefix(final String localPathPrefix) {
		assertNotNull("localPathPrefix", localPathPrefix);

		if (!localPathPrefix.isEmpty() && !localPathPrefix.startsWith("/"))
			throw new IllegalArgumentException("localPathPrefix must start with '/' but does not: " + localPathPrefix);

		if (! equal(this.localPathPrefix, localPathPrefix))
			this.localPathPrefix = localPathPrefix;
	}
}
