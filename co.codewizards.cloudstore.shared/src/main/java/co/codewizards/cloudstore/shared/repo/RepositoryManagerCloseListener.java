package co.codewizards.cloudstore.shared.repo;

import java.util.EventListener;

public interface RepositoryManagerCloseListener extends EventListener {
	void preClose(RepositoryManagerCloseEvent event);
	void postClose(RepositoryManagerCloseEvent event);
}
