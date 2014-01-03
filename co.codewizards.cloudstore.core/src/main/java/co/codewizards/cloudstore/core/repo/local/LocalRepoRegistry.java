package co.codewizards.cloudstore.core.repo.local;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.RepoFileDAO;
import co.codewizards.cloudstore.core.util.IOUtil;


public class LocalRepoRegistry 
{
	public static final String META_CONFIG_DIR = ".cloudstore";
	public static final String LOCAL_REPO_REGISTRY_FILE = "registry";
	
	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}
	
	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}
	
	private LocalRepoRegistry() {
		initConfigDir();
		createEntityID2FileMapPropertiesFile();
	}

	private void createEntityID2FileMapPropertiesFile() {
		RepoFileDAO repoFileDAO = new RepoFileDAO();
		Collection<RepoFile> repoFiles = repoFileDAO.getObjects();
		Properties props = new Properties();
		for (RepoFile repoFile : repoFiles) {
			List<RepoFile> repoFilePath = repoFile.getRepoFilePath();
			
			
//			props.put(repoFile.getEntityID(), repoFile.get)
		}
	}

	private void initConfigDir() {
		File configDir = new File(IOUtil.getUserHome(), META_CONFIG_DIR);
		if (!configDir.exists()) {
			try {
				configDir.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private Map<EntityID, File> entityID2FileMap;
	public Map<EntityID, File> getEntityID2URLMap() {
		if (entityID2FileMap == null) {
			entityID2FileMap = new HashMap<EntityID, File>();
		}

		return entityID2FileMap;
	}
}
