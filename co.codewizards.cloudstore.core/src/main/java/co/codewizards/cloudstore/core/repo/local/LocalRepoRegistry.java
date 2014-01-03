package co.codewizards.cloudstore.core.repo.local;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;


public class LocalRepoRegistry 
{
	public static final String META_CONFIG_DIR = ".cloudstore";
	public static final String LOCAL_REPO_REGISTRY_FILE = "repositories";

	public static final String DEFAULT_REPOSITORY_URL = IOUtil.getUserHome().getAbsolutePath();

	private static final File CONFIG_DIR = new File(IOUtil.getUserHome(), META_CONFIG_DIR);
	private static final File CONFIG_FILE = new File(CONFIG_DIR, LOCAL_REPO_REGISTRY_FILE);

	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}

	private LocalRepoRegistry() {
		initConfigDir();
		createRepositoryID2URLMapPropertiesFile();
	}

	private void createRepositoryID2URLMapPropertiesFile() {
		LocalRepositoryDAO localRepoDAO = new LocalRepositoryDAO();
		LocalRepository localRepo = localRepoDAO.getLocalRepositoryOrFail();
		Properties props = new Properties();
		props.put(localRepo.getEntityID(), DEFAULT_REPOSITORY_URL);

		try {
			PropertiesUtil.store(CONFIG_FILE, props, "Repository Location");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void initConfigDir() {
		if (!CONFIG_DIR.exists()) {
			try {
				CONFIG_DIR.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private Map<EntityID, URL> repositoryID2URLMap;
	public Map<EntityID, URL> getEntityID2URLMap(EntityID entityID) {
		if (repositoryID2URLMap == null) {
			repositoryID2URLMap = new HashMap<EntityID, URL>();
			try {
				Properties props = PropertiesUtil.load(CONFIG_FILE);
				repositoryID2URLMap.put(entityID, (URL)props.get(entityID));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return repositoryID2URLMap;
	}
}
