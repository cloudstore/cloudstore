package co.codewizards.cloudstore.rest.server;

import static java.util.Objects.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.exception.ApplicationException;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @author Chairat Kongarayawetchakun - ckongarayawetchakun at nightlabs dot de
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable>
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionMapper.class);

	public DefaultExceptionMapper(@Context final CloudStoreRest cloudStoreRest) {
		logger.debug("<init>: Instance created. cloudStoreRest={}", cloudStoreRest);

		if (cloudStoreRest == null)
			throw new IllegalArgumentException("cloudStoreRest == null");

	}

	@Override
	public Response toResponse(final Throwable throwable)
	{
		// We need to log the exception here, because it otherwise doesn't occur in any log
		// in a vanilla tomcat 7.0.25. Marco :-)
		Throwable applicationException = getApplicationException(throwable);
		if (applicationException != null) { // normal part of protocol => only debug
			if (logger.isDebugEnabled())
				logger.debug(String.valueOf(throwable), throwable);
			else
				logger.info("toResponse: {} wrapped in {}. Enable debug-logging, if you need the stack-trace.", applicationException.getClass().getName(), throwable.getClass().getName());
		}
		else
			logger.error(String.valueOf(throwable),throwable);

		if (throwable instanceof WebApplicationException) {
			return ((WebApplicationException)throwable).getResponse();
		}

		final Error error = new Error(throwable);
		return Response
				.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_XML)
				.entity(error)
				.build();
	}

	private Throwable getApplicationException(final Throwable exception) {
		requireNonNull(exception, "exception");

		Throwable x = exception;
		while (x != null) {
			final ApplicationException appEx = x.getClass().getAnnotation(ApplicationException.class);
			if (appEx != null)
				return x;

			x = x.getCause();
		}
		return null;
	}
}
