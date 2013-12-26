package co.codewizards.cloudstore.shared.persistence;

import java.lang.reflect.ParameterizedType;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class DAO<E extends Entity>
{
	private Class<E> entityClass;

	public DAO() {
		ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		@SuppressWarnings("unchecked")
		Class<E> c = (Class<E>) superclass.getActualTypeArguments()[0];
		this.entityClass = c;
		if (this.entityClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");
	}

	protected PersistenceManager pm;

	public PersistenceManager getPersistenceManager() {
		return pm;
	}
	public void setPersistenceManager(PersistenceManager persistenceManager) {
		this.pm = persistenceManager;
	}

	/**
	 * Get the type of the entity.
	 * @return the type of the entity; never <code>null</code>.
	 */
	public Class<E> getEntityClass() {
		return entityClass;
	}

	/**
	 * Get the entity-instance referenced by the specified identifier.
	 *
	 * @param entityID the identifier referencing the desired entity. Must not be <code>null</code>.
	 * @return the entity-instance referenced by the specified identifier. Never <code>null</code>.
	 * @throws JDOObjectNotFoundException if the entity referenced by the given identifier does not exist.
	 */
	public E getObjectByIdOrFail(Object entityID)
	throws JDOObjectNotFoundException
	{
		return getObjectById(entityID, true);
	}

	/**
	 * Get the entity-instance referenced by the specified identifier.
	 *
	 * @param entityID the identifier referencing the desired entity. Must not be <code>null</code>.
	 * @return the entity-instance referenced by the specified identifier or <code>null</code>, if no
	 * such entity exists.
	 */
	public E getObjectByIdOrNull(Object entityID)
	{
		return getObjectById(entityID, false);
	}

	/**
	 * Get the entity-instance referenced by the specified identifier.
	 *
	 * @param entityID the identifier referencing the desired entity. Must not be <code>null</code>.
	 * @param throwExceptionIfNotFound <code>true</code> to (re-)throw a {@link JDOObjectNotFoundException},
	 * if the referenced entity does not exist; <code>false</code> to return <code>null</code> instead.
	 * @return the entity-instance referenced by the specified identifier or <code>null</code>, if no
	 * such entity exists and <code>throwExceptionIfNotFound == false</code>.
	 * @throws JDOObjectNotFoundException if the entity referenced by the given identifier does not exist
	 * and <code>throwExceptionIfNotFound == true</code>.
	 */
	private E getObjectById(Object entityID, boolean throwExceptionIfNotFound)
	throws JDOObjectNotFoundException
	{
		if (entityID == null)
			throw new IllegalArgumentException("entityID == null");

		try {
			Object result = pm.getObjectById(entityClass, entityID);
			return entityClass.cast(result);
		} catch (JDOObjectNotFoundException x) {
			if (throwExceptionIfNotFound)
				throw x;
			else
				return null;
		}
	}

	public E makePersistent(E entity)
	{
		return pm.makePersistent(entity);
	}

	public void deletePersistent(E entity)
	{
		pm.deletePersistent(entity);
	}
}