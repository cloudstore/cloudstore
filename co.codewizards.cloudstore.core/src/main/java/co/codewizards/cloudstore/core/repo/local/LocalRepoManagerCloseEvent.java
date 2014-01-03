package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.EventObject;

public class LocalRepoManagerCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final LocalRepoManager localRepoManager;
	private final boolean backend;

	public LocalRepoManagerCloseEvent(Object source, LocalRepoManager localRepoManager, boolean backend) {
		super(assertNotNull("source", source));
		this.localRepoManager = assertNotNull("localRepoManager", localRepoManager);
		this.backend = backend;
	}

	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}

	public boolean isBackend() {
		return backend;
	}
}
