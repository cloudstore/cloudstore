package co.codewizards.cloudstore.ls.rest.server;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.ls.core.provider.JavaNativeMessageBodyReader;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeMessageBodyWriter;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeWithObjectRefMessageBodyReader;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeWithObjectRefMessageBodyWriter;
import co.codewizards.cloudstore.ls.rest.server.auth.AuthFilter;
import co.codewizards.cloudstore.ls.rest.server.service.ClassInfoService;
import co.codewizards.cloudstore.ls.rest.server.service.InverseServiceRequestService;
import co.codewizards.cloudstore.ls.rest.server.service.InverseServiceResponseService;
import co.codewizards.cloudstore.ls.rest.server.service.InvokeMethodService;
import co.codewizards.cloudstore.ls.rest.server.service.RepoInfoService;
import co.codewizards.cloudstore.ls.rest.server.service.TestService;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@ApplicationPath("LocalServerRest")
public class LocalServerRest extends ResourceConfig {
	private static final Logger logger = LoggerFactory.getLogger(LocalServerRest.class);

	static {
		logger.debug("<static_init>: Class loaded.");
	}

    {
		logger.debug("<init>: Instance created.");

		registerClasses(
				// BEGIN services
				ClassInfoService.class,
				InverseServiceRequestService.class,
				InverseServiceResponseService.class,
				InvokeMethodService.class,
				RepoInfoService.class,
				TestService.class,
				// END services

				// BEGIN providers
				// providers are not services (they are infrastructure), but they are registered the same way.
				AuthFilter.class,
				JavaNativeMessageBodyReader.class,
				JavaNativeMessageBodyWriter.class,
				CloudStoreJaxbContextResolver.class,
				DefaultExceptionMapper.class
				// END providers
				);

		register(new LocalServerRestBinder());

		final ObjectRefConverterFactoryImpl objectRefConverterFactory = new ObjectRefConverterFactoryImpl();
		register(new JavaNativeWithObjectRefMessageBodyReader(objectRefConverterFactory));
		register(new JavaNativeWithObjectRefMessageBodyWriter(objectRefConverterFactory));
	}
}
