package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;


/**
 * @author Sebastian Schefczyk
 *
 */
public abstract class AbstractRepoAwareIT extends AbstractIT {

	public static final int BUF_LENGTH = 32 * 1024;
	public static final int CHUNK_SIZE = 32 * BUF_LENGTH; // 1,048,576 bytes = 1 MiB chunk size

	protected File localRoot;
	protected File remoteRoot;

	protected String localPathPrefix;
	protected String remotePathPrefix;
	protected URL remoteRootURLWithPathPrefix;

	@Override
	@Before
	public void before() throws Exception {
		super.before();
		localPathPrefix = "";
		remotePathPrefix = "";
	}

	protected File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localRoot;

		return createFile(localRoot, localPathPrefix);
	}

	protected File getRemoteRootWithPathPrefix() {
		if (remotePathPrefix.isEmpty())
			return remoteRoot;

		final File file = createFile(remoteRoot, remotePathPrefix);
		return file;
	}

	protected List<File> searchCollisions(final File localRoot) {
		final List<File> collisions = new ArrayList<File>();
		searchCollisions_populate(localRoot, localRoot, collisions);
		return collisions;
	}

	protected void searchCollisions_populate(final File localRoot, final File file, final Collection<File> collisions) {
		final File[] children = file.listFiles();
		if (children != null) {
			for (final File f : children) {
				if (f.getName().contains(IOUtil.COLLISION_FILE_NAME_INFIX))
					collisions.add(f);

				searchCollisions_populate(localRoot, f, collisions);
			}
		}
	}

	protected File createFileWithChunks(final File localRoot, final File parent, final String name, final int chunks) throws IOException {
		assertThat(chunks).isGreaterThanOrEqualTo(1);
		final File file = createFile(parent, name);
		final OutputStream out = file.createOutputStream();
		// fill chunks, not at the end of file:
		final byte[] buf = new byte[CHUNK_SIZE];
		for (int i = 0; i < chunks - 1; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		// fill last chunk with random length:
		final byte[] lastChunkBuf = new byte[1 + random.nextInt(BUF_LENGTH)];
		random.nextBytes(lastChunkBuf);
		out.write(lastChunkBuf);

		out.close();
		assertThat(file.isFile()).isTrue();
		addToFilesInRepo(localRoot, file);
		return file;
	}

	protected URL getRemoteRootURLWithPathPrefix(final UUID remoteRepositoryId) throws MalformedURLException {
		final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId), remotePathPrefix);
		return remoteRootURL;
	}

	protected void assertThatNoCollisionInRepo(final File localRoot) {
		final List<File> collisions = searchCollisions(localRoot);
		if (!collisions.isEmpty())
			Assert.fail("Collision: " + collisions.get(0));
	}

	protected void createLocalAndRemoteRepo() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath()).execute();

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();
	}
}
