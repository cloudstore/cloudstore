package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.identity.LongIdentity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all data access objects (DAOs).
 * <p>
 * Usually an instance of a DAO is obtained using
 * {@link co.codewizards.cloudstore.local.LocalRepoTransactionImpl#getDAO(Class) LocalRepoTransaction.getDAO(...)}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class DAO<E extends Entity, D extends DAO<E, D>>
{
	private final Logger logger;
	private final Class<E> entityClass;
	private final Class<D> daoClass;

	/**
	 * Instantiate the DAO.
	 * <p>
	 * It is recommended <b>not</b> to invoke this constructor directly, but instead use
	 * {@link co.codewizards.cloudstore.local.LocalRepoTransactionImpl#getDAO(Class) LocalRepoTransaction.getDAO(...)},
	 * if a {@code LocalRepoTransaction} is available (which should be in most situations).
	 * <p>
	 * After constructing, you must {@linkplain #persistenceManager(PersistenceManager) assign a <code>PersistenceManager</code>},
	 * before you can use the DAO. This is already done when using the {@code LocalRepoTransaction}'s factory method.
	 */
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

	/**
	 * Gets the {@code PersistenceManager} assigned to this DAO.
	 * @return the {@code PersistenceManager} assigned to this DAO. May be <code>null</code>, if none
	 * was assigned, yet.
	 * @see #setPersistenceManager(PersistenceManager)
	 * @see #persistenceManager(PersistenceManager)
	 */
	public PersistenceManager getPersistenceManager() {
		return pm;
	}
	/**
	 * Assigns the given {@code PersistenceManager} to this DAO.
	 * <p>
	 * The DAO cannot be used, before a non-<code>null</code> value was set using this method.
	 * @param persistenceManager the {@code PersistenceManager} to be used by this DAO. May be <code>null</code>,
	 * but a non-<code>null</code> value must be set to make this DAO usable.
	 * @see #persistenceManager(PersistenceManager)
	 */
	public void setPersistenceManager(PersistenceManager persistenceManager) {
		if (this.pm != persistenceManager) {
			daoClass2DaoInstance.clear();
			this.pm = persistenceManager;
		}
	}

	protected PersistenceManager pm() {
		if (pm == null) {
			throw new IllegalStateException("persistenceManager not assigned!");
		}
		return pm;
	}

	/**
	 * Assigns the given {@code PersistenceManager} to this DAO and returns {@code this}.
	 * <p>
	 * This method delegates to {@link #setPersistenceManager(PersistenceManager)}.
	 * @param persistenceManager the {@code PersistenceManager} to be used by this DAO. May be <code>null</code>,
	 * but a non-<code>null</code> value must be set to make this DAO usable.
	 * @return {@code this} for a fluent API.
	 * @see #setPersistenceManager(PersistenceManager)
	 */
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
	 * @param id the identifier referencing the desired entity. Must not be <code>null</code>.
	 * @return the entity-instance referenced by the specified identifier. Never <code>null</code>.
	 * @throws JDOObjectNotFoundException if the entity referenced by the given identifier does not exist.
	 */
	public E getObjectByIdOrFail(long id)
	throws JDOObjectNotFoundException
	{
		return getObjectById(id, true);
	}

	/**
	 * Get the entity-instance referenced by the specified identifier.
	 *
	 * @param id the identifier referencing the desired entity. Must not be <code>null</code>.
	 * @return the entity-instance referenced by the specified identifier or <code>null</code>, if no
	 * such entity exists.
	 */
	public E getObjectByIdOrNull(long id)
	{
		return getObjectById(id, false);
	}

	/**
	 * Get the entity-instance referenced by the specified identifier.
	 *
	 * @param id the identifier referencing the desired entity. Must not be <code>null</code>.
	 * @param throwExceptionIfNotFound <code>true</code> to (re-)throw a {@link JDOObjectNotFoundException},
	 * if the referenced entity does not exist; <code>false</code> to return <code>null</code> instead.
	 * @return the entity-instance referenced by the specified identifier or <code>null</code>, if no
	 * such entity exists and <code>throwExceptionIfNotFound == false</code>.
	 * @throws JDOObjectNotFoundException if the entity referenced by the given identifier does not exist
	 * and <code>throwExceptionIfNotFound == true</code>.
	 */
	private E getObjectById(long id, boolean throwExceptionIfNotFound)
	throws JDOObjectNotFoundException
	{
		try {
			Object result = pm().getObjectById(new LongIdentity(entityClass, id));
			return entityClass.cast(result);
		} catch (JDOObjectNotFoundException x) {
			if (throwExceptionIfNotFound)
				throw x;
			else
				return null;
		}
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

	public <P extends E> P makePersistent(final P entity)
	{
		assertNotNull("entity", entity);
		try {
			final P result = pm().makePersistent(entity);
			logger.debug("makePersistent: entityID={}", JDOHelper.getObjectId(result));
			return result;
		} catch (final RuntimeException x) {
			logger.warn("makePersistent: FAILED for entityID={}: {}", JDOHelper.getObjectId(entity), x);
			throw x;
		}
	}

	public void deletePersistent(final E entity)
	{
		assertNotNull("entity", entity);
		logger.debug("deletePersistent: entityID={}", JDOHelper.getObjectId(entity));
		pm().deletePersistent(entity);
	}

	public void deletePersistentAll(final Collection<? extends E> entities)
	{
		assertNotNull("entities", entities);
		if (logger.isDebugEnabled()) {
			for (E entity : entities) {
				logger.debug("deletePersistentAll: entityID={}", JDOHelper.getObjectId(entity));
			}
		}
		pm().deletePersistentAll(entities);
	}

	protected Collection<E> load(final Collection<E> entities) {
		final Collection<E> result = new ArrayList<>();
		final Map<Class<? extends Entity>, Set<Long>> entityClass2EntityIDs = new HashMap<>();
		for (E entity : entities) {
			Set<Long> entityIDs = entityClass2EntityIDs.get(entity.getClass());
			if (entityIDs == null) {
				entityIDs = new HashSet<>();
				entityClass2EntityIDs.put(entity.getClass(), entityIDs);
			}
			entityIDs.add(entity.getId());
		}

		for (Map.Entry<Class<? extends Entity>, Set<Long>> me : entityClass2EntityIDs.entrySet()) {
			Class<? extends Entity> entityClass = me.getKey();
			Query query = pm().newQuery(pm().getExtent(entityClass, false));
			query.setFilter(":entityIDs.contains(this.id)");

			Set<Long> entityIDs = me.getValue();
			int idx = -1;
			Set<Long> entityIDSubSet = new HashSet<>(300);
			for (Long entityID : entityIDs) {
				++idx;
				entityIDSubSet.add(entityID);
				if (idx > 200) {
					idx = -1;
					populateLoadResult(result, query, entityIDSubSet);
				}
			}
			populateLoadResult(result, query, entityIDSubSet);
		}
		return result;
	}

	private void populateLoadResult(Collection<E> result, Query query, Set<Long> entityIDSubSet) {
		if (entityIDSubSet.isEmpty())
			return;

		@SuppressWarnings("unchecked")
		Collection<E> c = (Collection<E>) query.execute(entityIDSubSet);
		result.addAll(c);
		query.closeAll();
		entityIDSubSet.clear();
	}

	private final Map<Class<? extends DAO<?,?>>, DAO<?,?>> daoClass2DaoInstance = new HashMap<>(3);

	protected <T extends DAO<?, ?>> T getDAO(Class<T> daoClass) {
		T dao = daoClass.cast(daoClass2DaoInstance.get(assertNotNull("daoClass", daoClass)));
		if (dao == null) {
			try {
				dao = daoClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			dao.persistenceManager(pm);
			daoClass2DaoInstance.put(daoClass, dao);
		}
		return dao;
	}
}