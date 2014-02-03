package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.EntityID;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class DAO<E extends Entity, D extends DAO<E, D>>
{
	private final Logger logger;
	private final Class<E> entityClass;
	private final Class<D> daoClass;

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

		logger = LoggerFactory.getLogger(String.format("%s<%s>", DAO.class.getName(), entityClass.getSimpleName()));
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
		assertNotNull("entityID", entityID);

//		try {
//			Object result = pm().getObjectById(entityClass, entityID);
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
			query = pm().newQuery(entityClass);
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

	public Collection<E> getObjects() {
		ArrayList<E> result = new ArrayList<E>();
		Iterator<E> iterator = pm().getExtent(entityClass).iterator();
		while (iterator.hasNext()) {
			result.add(iterator.next());
		}
		return result;
	}

	public long getObjectsCount() {
		Query query = pm().newQuery(entityClass);
		query.setResult("count(this)");
		Long result = (Long) query.execute();
		if (result == null)
			throw new IllegalStateException("Query for count(this) returned null!");

		return result;
	}

	public <P extends E> P makePersistent(P entity)
	{
		assertNotNull("entity", entity);
		logger.debug("makePersistent: entityID={} idHigh={} idLow={}", entity.getEntityID(), entity.getIdHigh(), entity.getIdLow());
		return pm().makePersistent(entity);
	}

	public void deletePersistent(E entity)
	{
		assertNotNull("entity", entity);
		logger.debug("deletePersistent: entityID={} idHigh={} idLow={}", entity.getEntityID(), entity.getIdHigh(), entity.getIdLow());
		pm().deletePersistent(entity);
	}

	public void deletePersistentAll(Collection<? extends E> entities)
	{
		assertNotNull("entities", entities);
		if (logger.isDebugEnabled()) {
			for (E entity : entities) {
				logger.debug("deletePersistentAll: entityID={} idHigh={} idLow={}", entity.getEntityID(), entity.getIdHigh(), entity.getIdLow());
			}
		}
		pm().deletePersistentAll(entities);
	}
}