package co.codewizards.cloudstore.rest.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.server.jersey.CloudStoreJaxbContextResolver;
import co.codewizards.cloudstore.rest.server.jersey.CloudStoreRESTProvider;
import co.codewizards.cloudstore.rest.server.jersey.DAOProvider;
import co.codewizards.cloudstore.rest.server.jersey.DefaultExceptionMapper;
import co.codewizards.cloudstore.rest.server.service.BeginPutFileService;
import co.codewizards.cloudstore.rest.server.service.ChangeSetService;
import co.codewizards.cloudstore.rest.server.service.EndPutFileService;
import co.codewizards.cloudstore.rest.server.service.EndSyncFromRepositoryService;
import co.codewizards.cloudstore.rest.server.service.EndSyncToRepositoryService;
import co.codewizards.cloudstore.rest.server.service.FileChunkSetService;
import co.codewizards.cloudstore.rest.server.service.RepositoryDTOService;
import co.codewizards.cloudstore.rest.server.service.TestService;
import co.codewizards.cloudstore.rest.server.service.WebDavService;

import com.sun.jersey.api.core.DefaultResourceConfig;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@ApplicationPath("CloudStoreREST")
public class CloudStoreREST
//extends Application
extends DefaultResourceConfig
{
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreREST.class);

	public static final boolean TEST_MODE = Boolean.parseBoolean(System.getProperty("co.codewizards.cloudstore.webapp.CloudStoreREST.TEST_MODE"));

	static {
		logger.debug("<static_init>: Class loaded. TEST_MODE={}", TEST_MODE);
	}

	{
		logger.debug("<init>: Instance created.");
//		getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", JDOTransactionRequestFilter.class.getName());
//		getProperties().put("com.sun.jersey.spi.container.ContainerResponseFilters", JDOTransactionResponseFilter.class.getName());
//		getProperties().put("com.sun.jersey.spi.container.ResourceFilters", value)
	}

	private static final Set<Class<?>> classes = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(new Class<?>[] {
			// BEGIN services
			BeginPutFileService.class,
			ChangeSetService.class,
			EndPutFileService.class,
			EndSyncFromRepositoryService.class,
			EndSyncToRepositoryService.class,
			FileChunkSetService.class,
			RepositoryDTOService.class,
			TestService.class,
			WebDavService.class,
			// END services

			// BEGIN providers
			// providers are not services (they are infrastructure), but they are registered the same way.
			CloudStoreJaxbContextResolver.class,
			DefaultExceptionMapper.class
			// END providers
	})));

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

	private Set<Object> singletons;

	@Override
	public Set<Object> getSingletons()
	{
		if (singletons == null) {

			Set<Object> s = new HashSet<Object>();
			s.add(new CloudStoreRESTProvider(this));
			s.add(new DAOProvider(this));

			singletons = Collections.unmodifiableSet(s);
		}

		return singletons;
	}


}
