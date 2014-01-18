package co.codewizards.cloudstore.core.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="RemoteRepository")
//@Index(name="RemoteRepository_remoteRoot", members="remoteRoot") // Indexing a CLOB with Derby throws an exception :-( [should be a warning, IMHO for portability reasons]
@Index(name="RemoteRepository_remoteRootSha1", members="remoteRootSha1")
@Query(name="getRemoteRepository_remoteRootSha1", value="SELECT UNIQUE WHERE this.remoteRootSha1 == :remoteRootSha1")
public class RemoteRepository extends Repository implements AutoTrackLocalRevision {
	private static final Logger logger = LoggerFactory.getLogger(RemoteRepository.class);

	@Column(jdbcType="CLOB")
	private URL remoteRoot;

	private String remoteRootSha1;

	private long localRevision;

	public RemoteRepository() { }

	public RemoteRepository(EntityID entityID) {
		super(entityID);
	}

	public URL getRemoteRoot() {
		return remoteRoot;
	}

	public void setRemoteRoot(URL remoteRoot) {
		this.remoteRoot = remoteRoot;
		this.remoteRootSha1 = sha1(remoteRoot);
	}

	public static String sha1(URL remoteRoot) {
		if (remoteRoot == null)
			return null;

		byte[] remoteRootBytes = remoteRoot.toExternalForm().getBytes(IOUtil.CHARSET_UTF_8);
		byte[] hash;
		try {
			hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(remoteRootBytes));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return HashUtil.encodeHexStr(hash);
	}

	public String getRemoteRootSha1() {
		return remoteRootSha1;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(long localRevision) {
		if (this.localRevision != localRevision) {
			if (logger.isDebugEnabled())
				logger.debug("setLocalRevision: repositoryID={} old={} new={}", getEntityID(), this.localRevision, localRevision);

			this.localRevision = localRevision;
		}
	}
}
