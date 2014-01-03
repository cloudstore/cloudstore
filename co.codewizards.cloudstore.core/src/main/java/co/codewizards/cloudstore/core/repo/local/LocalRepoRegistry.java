package co.codewizards.cloudstore.core.repo.local;


public class LocalRepoRegistry 
{
	public static final String META_CONFIG_DIR = ".cloudstore";
	
	private LocalRepoRegistry() {}
	
	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}
	
	
}
