package co.codewizards.cloudstore.shared.persistence;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import co.codewizards.cloudstore.shared.dto.EntityID;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class DAO<E extends Entity, D extends DAO<E, D>>
{
	private Class<E> entityClass;
	private Class<D> daoClass;

	public DAO() {
		ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		Type[] actualTypeArguments = superclass.getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length < 2)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");

		@SuppressWarnings("unchecked")
		Class<E> c = (Class<E>) actualTypeArguments[0];
		this.entityClass = c;
		if (this.entityClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");

		@SuppressWarnings("unchecked")
		Class<D> k = (Class<D>) actualTypeArguments[1];
		this.daoClass = k;
		if (this.daoClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");
	}

	private PersistenceManager pm;

	public PersistenceManager getPersistenceManager() {
		return pm;
	}
	public void setPersistenceManager(PersistenceManager persistenceManager) {
		this.pm = persistenceManager;
	}

	protected PersistenceManager pm() {
		if (pm == null) {
			throw new IllegalStateException("persistenceManager not assigned!");
		}
		return pm;
	}

	public D persistenceManager(PersistenceManager persistenceManager) {
		setPersistenceManager(persistenceManager);
		return thisDAO();
	}

	protected D thisDAO() {
		return daoClass.cast(this);
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
	public E getObjectByIdOrFail(EntityID entityID)
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
	public E getObjectByIdOrNull(EntityID entityID)
	{
		return getObjectById(entityID, false);
	}

	private Query queryGetObjectById;

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
	private E getObjectById(EntityID entityID, boolean throwExceptionIfNotFound)
	throws JDOObjectNotFoundException
	{
		if (entityID == null)
			throw new IllegalArgumentException("entityID == null");

//		try {
//			Object result = pm.getObjectById(entityClass, entityID);
//			return entityClass.cast(result);
//		} catch (JDOObjectNotFoundException x) {
//			if (throwExceptionIfNotFound)
//				throw x;
//			else
//				return null;
//		}

		// The above currently fails :-(
		// See: http://www.datanucleus.org/servlet/forum/viewthread_thread,7079
		// Thus using the workaround below instead. Marco :-)

		Query query = queryGetObjectById;
		if (query == null) {
			query = pm.newQuery(entityClass);
			query.setFilter("this.idHigh == :entityID_idHigh && this.idLow == :entityID_idLow");
			query.setUnique(true);
			queryGetObjectById = query;
		}
		Object result = query.execute(entityID.idHigh, entityID.idLow);
		if (result == null && throwExceptionIfNotFound)
			throw new JDOObjectNotFoundException("There is no entity of type " + entityClass.getName() + " with entityID=" + entityID + '!');

		// No idea, if this is still an issue - I copied the stuff from an older project and don't know which DN version...

		return entityClass.cast(result);
	}

	public E makePersistent(E entity)
	{
		return pm().makePersistent(entity);
	}

	public void deletePersistent(E entity)
	{
		pm().deletePersistent(entity);
	}
}