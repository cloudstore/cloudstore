package co.codewizards.cloudstore.core.repo.local;

public class LocalRepoManagerImplHelper {
	public static void disableDeferredClose() {
		LocalRepoManagerImpl.closeDeferredMillis = 0;
	}
}
