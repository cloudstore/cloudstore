package co.codewizards.cloudstore.local;

import javax.jdo.PersistenceManager;

public interface ContextWithPersistenceManager {

	PersistenceManager getPersistenceManager();

}
