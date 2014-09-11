package co.codewizards.cloudstore.rest.server;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.server.service.BeginPutFileService;
import co.codewizards.cloudstore.rest.server.service.ChangeSetDtoService;
import co.codewizards.cloudstore.rest.server.service.CopyService;
import co.codewizards.cloudstore.rest.server.service.EncryptedSignedAuthTokenService;
import co.codewizards.cloudstore.rest.server.service.EndPutFileService;
import co.codewizards.cloudstore.rest.server.service.EndSyncFromRepositoryService;
import co.codewizards.cloudstore.rest.server.service.EndSyncToRepositoryService;
import co.codewizards.cloudstore.rest.server.service.MakeDirectoryService;
import co.codewizards.cloudstore.rest.server.service.MakeSymlinkService;
import co.codewizards.cloudstore.rest.server.service.MoveFileInProgressService;
import co.codewizards.cloudstore.rest.server.service.MoveService;
import co.codewizards.cloudstore.rest.server.service.RepoFileDtoService;
import co.codewizards.cloudstore.rest.server.service.RepositoryDtoService;
import co.codewizards.cloudstore.rest.server.service.RequestRepoConnectionService;
import co.codewizards.cloudstore.rest.server.service.TestService;
import co.codewizards.cloudstore.rest.server.service.WebDavService;
import co.codewizards.cloudstore.rest.shared.GZIPReaderInterceptor;
import co.codewizards.cloudstore.rest.shared.GZIPWriterInterceptor;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@ApplicationPath("CloudStoreRest")
public class CloudStoreRest extends ResourceConfig {
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreRest.class);

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
				ChangeSetDtoService.class,
				CopyService.class,
				EncryptedSignedAuthTokenService.class,
				EndPutFileService.class,
				EndSyncFromRepositoryService.class,
				EndSyncToRepositoryService.class,
				RepoFileDtoService.class,
				MakeDirectoryService.class,
				MakeSymlinkService.class,
				MoveService.class,
				MoveFileInProgressService.class,
				RepositoryDtoService.class,
				RequestRepoConnectionService.class,
				TestService.class,
				WebDavService.class,
				// END services

				// BEGIN providers
				// providers are not services (they are infrastructure), but they are registered the same way.
				GZIPReaderInterceptor.class,
				GZIPWriterInterceptor.class,
				CloudStoreJaxbContextResolver.class,
				DefaultExceptionMapper.class
				// END providers
				);

		register(new CloudStoreBinder());
	}
}
