package co.codewizards.cloudstore.core.repo.local;

import static java.util.Objects.*;

import java.util.EventObject;

public class LocalRepoManagerCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final LocalRepoManager localRepoManager;
	private final boolean backend;

	public LocalRepoManagerCloseEvent(Object source, LocalRepoManager localRepoManager, boolean backend) {
		super(requireNonNull(source, "source"));
		this.localRepoManager = requireNonNull(localRepoManager, "localRepoManager");
		this.backend = backend;
	}

	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}

	public boolean isBackend() {
		return backend;
	}
}
