package co.codewizards.cloudstore.test;


import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.repo.RepositoryManagerRegistry;

public class TestRepositoryRegistry {
	private static final Logger logger = LoggerFactory.getLogger(TestRepositoryRegistry.class);
	@Test
	public void createRepository() {
		RepositoryManagerRegistry repositoryManagerRegistry = RepositoryManagerRegistry.getInstance();

		File target = new File("target");
		logger.trace("LocalRoot: " + target.getAbsolutePath());
		repositoryManagerRegistry.getRepositoryManager(target);
	}
}
