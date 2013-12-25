package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RepositoryManagerRegistry
{
	private Map<File, RepositoryManager> localRoot2RepositoryManager = new HashMap<File, RepositoryManager>();

	private RepositoryManagerRegistry() {}

	private static class RepositoryManagerRegistryHolder {
		public static final RepositoryManagerRegistry INSTANCE = new RepositoryManagerRegistry();
	}

	public static RepositoryManagerRegistry getInstance() {
		return RepositoryManagerRegistryHolder.INSTANCE;
	}

	public RepositoryManager getRepositoryManager(File localRoot) {
		localRoot = canonicalize(localRoot);

		RepositoryManager repositoryManager = localRoot2RepositoryManager.get(localRoot);
		if (repositoryManager == null) {
			repositoryManager = new RepositoryManager(localRoot, false);
			localRoot2RepositoryManager.put(localRoot, repositoryManager);
		}
		return repositoryManager;
	}

	public RepositoryManager createRepositoryManager(File localRoot, URL url) {
		localRoot = canonicalize(localRoot);

		RepositoryManager repositoryManager = localRoot2RepositoryManager.get(localRoot);
		if (repositoryManager != null) {
			throw new IllegalArgumentException(String.format("Repository already exists: %s", localRoot));
		}
		repositoryManager = new RepositoryManager(localRoot, true);
		localRoot2RepositoryManager.put(localRoot, repositoryManager);
		return repositoryManager;
	}

	private File canonicalize(File localRoot) {
		assertNotNull("localRoot", localRoot);
		try {
			localRoot = localRoot.getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return localRoot;
	}
}
