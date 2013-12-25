package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.util.EventObject;

public class RepositoryManagerCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private RepositoryManager repositoryManager;

	public RepositoryManagerCloseEvent(Object source, RepositoryManager repositoryManager) {
		super(assertNotNull("source", source));
		this.repositoryManager = assertNotNull("repositoryManager", repositoryManager);
	}

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}
}
