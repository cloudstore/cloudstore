package co.codewizards.cloudstore.core.repo.local;

public interface DaoProvider {

	<D> D getDao(Class<D> daoClass);
}
