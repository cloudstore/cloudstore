package co.codewizards.cloudstore.ls.rest.server;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.dto.Error;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @author Chairat Kongarayawetchakun - ckongarayawetchakun at nightlabs dot de
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable>
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionMapper.class);

	/**
	 * The media-types supported for serialising an {@link Error}.
	 */
	private static final Set<MediaType> RESPONSE_MEDIA_TYPES = new LinkedHashSet<>(Arrays.asList(
			MediaType.APPLICATION_XML_TYPE,
			MediaType.APPLICATION_JSON_TYPE
	));

	@Context
	private HttpHeaders headers;

	public DefaultExceptionMapper(@Context final LocalServerRest localServerRest) {
		logger.debug("<init>: Instance created. localServerRest={}", localServerRest);

		if (localServerRest == null)
			throw new IllegalArgumentException("localServerRest == null");

	}

	@Override
	public Response toResponse(final Throwable throwable)
	{
		// We need to log the exception here, because it otherwise doesn't occur in any log
		// in a vanilla tomcat 7.0.25. Marco :-)
		if (throwable instanceof DeferredCompletionException) // normal part of protocol => only debug
			logger.debug(String.valueOf(throwable), throwable);
		else
			logger.error(String.valueOf(throwable),throwable);

		if (throwable instanceof WebApplicationException) {
			return ((WebApplicationException)throwable).getResponse();
		}

		final Error error = new Error(throwable);
		return Response
				.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(getResponseMediaType())
				.entity(error)
				.build();
	}

	private MediaType getResponseMediaType() {
		for (MediaType mediaType : headers.getAcceptableMediaTypes()) {
			if (RESPONSE_MEDIA_TYPES.contains(mediaType))
				return mediaType;
		}
		return MediaType.APPLICATION_XML_TYPE;
	}
}
