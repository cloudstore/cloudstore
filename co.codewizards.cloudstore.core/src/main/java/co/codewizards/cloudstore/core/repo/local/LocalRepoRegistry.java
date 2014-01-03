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

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;


public class LocalRepoRegistry 
{
	public static final String REPOSITORY_REGISTRY_DIR = ".cloudstore";
	public static final String LOCAL_REPO_REGISTRY_FILE = "repositoryList.properties";


	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}

	private LocalRepoRegistry() {
		initConfigDir();
	}

	public File getRegistryDir() {
		return new File(IOUtil.getUserHome(), REPOSITORY_REGISTRY_DIR);
	}
	
	private File getRegistryFile() {
		return new File(getRegistryDir(), LOCAL_REPO_REGISTRY_FILE);
	}

	public Map<EntityID, File> repositoryID2FileMap;
	
	public Map<EntityID, File> getRepositoryID2FileMap() {
		if (repositoryID2FileMap == null) {
			repositoryID2FileMap = new HashMap<EntityID, File>();
		}

		try {
			Properties props = PropertiesUtil.load(new File(getRegistryDir(), LOCAL_REPO_REGISTRY_FILE));
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
		return repositoryID2FileMap;
	}
	
	public void registerRepository(EntityID repositoryID, File file) {
		File registryFile = getRegistryFile();
		if (!registryFile.exists()) {
			try {
				Files.createFile(Paths.get(registryFile.toURI()));
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
		
		props.put(repositoryID.toString(), file.toURI().toString());
		
		try {
			PropertiesUtil.store(registryFile, props, "Repository List");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void initConfigDir() {
		File configDir = new File(IOUtil.getUserHome(), REPOSITORY_REGISTRY_DIR);
		if (!configDir.exists()) {
			configDir.mkdir();
		}
	}
}