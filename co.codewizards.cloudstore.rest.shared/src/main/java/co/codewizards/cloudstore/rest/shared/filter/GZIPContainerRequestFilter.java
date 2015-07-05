package co.codewizards.cloudstore.rest.shared.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import co.codewizards.cloudstore.rest.shared.GZIPUtil;

/**
 * Filter that sets property Cloudstore-Content-Encoding if request contains a header of the same name.
 * <p>
 * Property can be accessed by interceptors in request/response scope, which is needed in this case.
 * @see GZIPClientRequestFilter
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */

public class GZIPContainerRequestFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if(GZIPUtil.isRequestCompressedWithGzip(requestContext)){
			requestContext.setProperty(GZIPUtil.CLOUDSTORE_ENCODING_HEADER, GZIPUtil.CLOUDSTORE_ENCODING_HEADER_VALUE);
		}
	}
}
