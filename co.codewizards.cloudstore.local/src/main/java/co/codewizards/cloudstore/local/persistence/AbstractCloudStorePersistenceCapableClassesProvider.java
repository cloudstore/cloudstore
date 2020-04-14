package co.codewizards.cloudstore.local.persistence;

public abstract class AbstractCloudStorePersistenceCapableClassesProvider implements CloudStorePersistenceCapableClassesProvider {

	@Override
	public int getOrderHint() {
		return 50;
	}

}
