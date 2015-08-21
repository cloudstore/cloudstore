package co.codewizards.cloudstore.core.repo.local;

import java.util.UUID;

public class CreateRepositoryContext {

	public static final ThreadLocal<UUID> repositoryIdThreadLocal = new ThreadLocal<UUID>();

}
