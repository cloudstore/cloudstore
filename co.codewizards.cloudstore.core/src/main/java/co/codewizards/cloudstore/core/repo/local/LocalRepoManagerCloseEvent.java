package co.codewizards.cloudstore.core.repo.local;

import java.util.EventObject;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class LocalRepoManagerCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final LocalRepoManager localRepoManager;
	private final boolean backend;

	public LocalRepoManagerCloseEvent(Object source, LocalRepoManager localRepoManager, boolean backend) {
		super(AssertUtil.assertNotNull("source", source));
		this.localRepoManager = AssertUtil.assertNotNull("localRepoManager", localRepoManager);
		this.backend = backend;
	}

	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}

	public boolean isBackend() {
		return backend;
	}
}
