package co.codewizards.cloudstore.core.repo.local;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.util.PropertiesUtil;


public class LocalRepoRegistry
{
	public static final String LOCAL_REPO_REGISTRY_FILE = "repositoryList.properties";


	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}

	private LocalRepoRegistry() { }

	private File getRegistryFile() {
		return new File(ConfigDir.getInstance().getFile(), LOCAL_REPO_REGISTRY_FILE);
	}

	public Map<EntityID, File> repositoryID2FileMap; // TODO why is this public?!?

	public Map<EntityID, File> getRepositoryID2FileMap() { // TODO what about multiple threads? is this thread-safe?!?
		if (repositoryID2FileMap == null) {
			repositoryID2FileMap = new HashMap<EntityID, File>();
		}

		try {
//			Properties props = PropertiesUtil.load(new File(getRegistryDir(), LOCAL_REPO_REGISTRY_FILE)); // TODO why does this line create the same file that is returned by getRegistryFile()?!
			Properties props = PropertiesUtil.load(getRegistryFile());
			Set<Entry<Object, Object>> entrySet = props.entrySet();
			for (Entry<Object, Object> entry : entrySet) {
				EntityID entityID = new EntityID(entry.getKey().toString());
				URI	uri = new URI(entry.getValue().toString());
				File file = new File(uri);
				repositoryID2FileMap.put(entityID, file);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return repositoryID2FileMap; // TODO why does this expose the internal data structure in a *writable* way?!?
	}

	public void registerRepository(EntityID repositoryID, File file) { // TODO what about multiple threads? is this thread-safe?!? what about multiple *PROCESSES*?!
		// TODO This method does not update this.repositoryID2FileMap!
		File registryFile = getRegistryFile();
		if (!registryFile.exists()) {
			try {
				Files.createFile(Paths.get(registryFile.toURI())); // TODO why do you create this file? it's created by PropertiesUtil.store(...), anyway (see below)!
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		Properties props;
		try {
			props = PropertiesUtil.load(registryFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		props.put(repositoryID.toString(), file.toURI().toString()); // TODO why do you store an URI? we manage *local* repositories only.

		try {
			PropertiesUtil.store(registryFile, props, "Repository List");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}