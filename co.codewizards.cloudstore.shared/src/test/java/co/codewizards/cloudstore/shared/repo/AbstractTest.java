package co.codewizards.cloudstore.shared.repo;

import java.io.File;
import java.math.BigInteger;
import java.util.Random;

public abstract class AbstractTest {

	protected static final Random random = new Random();
	protected static RepositoryManagerRegistry repositoryManagerRegistry = RepositoryManagerRegistry.getInstance();

	protected File newTestRepositoryLocalRoot() {
		long timestamp = System.currentTimeMillis();
		int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36);
		return new File(getTestRepositoryBaseDir(), repoName);
	}

	protected File getTestRepositoryBaseDir() {
		File dir = new File(new File("target"), "repo");
		dir.mkdirs();
		return dir;
	}

}
