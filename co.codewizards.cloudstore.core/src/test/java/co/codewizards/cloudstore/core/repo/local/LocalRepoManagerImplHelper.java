package co.codewizards.cloudstore.core.repo.local;

public class LocalRepoManagerImplHelper {
	public static void setCloseDeferredMillis(long millis) {
		LocalRepoManagerImpl.closeDeferredMillis = millis;
	}

	public static long getCloseDeferredMillis() {
		return LocalRepoManagerImpl.closeDeferredMillis;
	}
}
