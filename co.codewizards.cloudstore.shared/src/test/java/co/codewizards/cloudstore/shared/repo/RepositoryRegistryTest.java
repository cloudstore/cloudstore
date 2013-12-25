package co.codewizards.cloudstore.shared.repo;


import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryRegistryTest {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryRegistryTest.class);

	@Test
	public void createRepository() {
		RepositoryManagerRegistry repositoryManagerRegistry = RepositoryManagerRegistry.getInstance();

		File target = new File("target");
		logger.trace("LocalRoot: " + target.getAbsolutePath());
		repositoryManagerRegistry.getRepositoryManager(target);
	}
}
