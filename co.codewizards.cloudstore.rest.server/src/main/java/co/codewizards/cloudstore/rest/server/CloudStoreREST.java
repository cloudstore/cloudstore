package co.codewizards.cloudstore.rest.server;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.server.jersey.CloudStoreBinder;
import co.codewizards.cloudstore.rest.server.jersey.CloudStoreJaxbContextResolver;
import co.codewizards.cloudstore.rest.server.jersey.DefaultExceptionMapper;
import co.codewizards.cloudstore.rest.server.service.BeginPutFileService;
import co.codewizards.cloudstore.rest.server.service.ChangeSetDTOService;
import co.codewizards.cloudstore.rest.server.service.EncryptedSignedAuthTokenService;
import co.codewizards.cloudstore.rest.server.service.EndPutFileService;
import co.codewizards.cloudstore.rest.server.service.EndSyncFromRepositoryService;
import co.codewizards.cloudstore.rest.server.service.EndSyncToRepositoryService;
import co.codewizards.cloudstore.rest.server.service.RepoFileDTOService;
import co.codewizards.cloudstore.rest.server.service.MakeDirectoryService;
import co.codewizards.cloudstore.rest.server.service.RepositoryDTOService;
import co.codewizards.cloudstore.rest.server.service.RequestRepoConnectionService;
import co.codewizards.cloudstore.rest.server.service.TestService;
import co.codewizards.cloudstore.rest.server.service.WebDavService;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@ApplicationPath("CloudStoreREST")
public class CloudStoreREST extends ResourceConfig {
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreREST.class);

	static {
		logger.debug("<static_init>: Class loaded.");
	}

    {
		logger.debug("<init>: Instance created.");
//		getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", JDOTransactionRequestFilter.class.getName());
//		getProperties().put("com.sun.jersey.spi.container.ContainerResponseFilters", JDOTransactionResponseFilter.class.getName());
//		getProperties().put("com.sun.jersey.spi.container.ResourceFilters", value)

		registerClasses(
				// BEGIN services
				BeginPutFileService.class,
				ChangeSetDTOService.class,
				EncryptedSignedAuthTokenService.class,
				EndPutFileService.class,
				EndSyncFromRepositoryService.class,
				EndSyncToRepositoryService.class,
				RepoFileDTOService.class,
//				LocalSyncService.class,
				MakeDirectoryService.class,
				RepositoryDTOService.class,
				RequestRepoConnectionService.class,
				TestService.class,
				WebDavService.class,
				// END services

				// BEGIN providers
				// providers are not services (they are infrastructure), but they are registered the same way.
				CloudStoreJaxbContextResolver.class,
				DefaultExceptionMapper.class
				// END providers
				);

		register(new CloudStoreBinder());
	}
}
