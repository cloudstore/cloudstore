package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.ObjectUtil.*;

import java.io.File;

import javax.jdo.PersistenceManagerFactory;


public class RepositoryManager {

	private File localRoot;
	private PersistenceManagerFactory persistenceManagerFactory;

	public RepositoryManager(File localRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
	}

	private void initPersistenceManagerFactory() {
//		persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory
	}


	public File getLocalRoot() {
		return localRoot;
	}

	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}
}
