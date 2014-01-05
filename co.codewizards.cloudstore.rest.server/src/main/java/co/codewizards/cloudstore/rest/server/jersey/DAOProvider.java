package co.codewizards.cloudstore.rest.server.jersey;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;

import co.codewizards.cloudstore.core.persistence.DAO;
import co.codewizards.cloudstore.rest.server.CloudStoreREST;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 * @author sschefczyk
 */
public class DAOProvider implements InjectableProvider<Context, Type>
{
//	private PersistenceManagerProvider persistenceManagerProvider;

	public DAOProvider(final CloudStoreREST cloudStoreREST) {
//		this.persistenceManagerProvider = licenceREST.getPersistenceManagerProvider();
	}

	@Override
	public Injectable<DAO<?, ?>> getInjectable(final ComponentContext ic, final Context a, final Type c)
	{
		if (!(c instanceof Class)) {
			return null;
		}

		final Class<?> klass = (Class<?>) c;
		if (!DAO.class.isAssignableFrom(klass)) {
			return null;
		}

		return new Injectable<DAO<?, ?>>() {
			@Override
			public DAO<?, ?> getValue() {
				final DAO<?, ?> dao;
				try {
					dao = (DAO<?, ?>) klass.newInstance();
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}

//				dao.setPersistenceManager(persistenceManagerProvider.getPersistenceManager());
				return dao;
			}
		};
	}

	@Override
	public ComponentScope getScope() {
		return ComponentScope.PerRequest;
	}

}
