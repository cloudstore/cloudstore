package co.codewizards.cloudstore.rest.shared.filter;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import co.codewizards.cloudstore.rest.shared.GZIPUtil;

/**
 * Filter that adds to REST request a Cloudstore-Content-Encoding header with "gzip" value.
 * <p>
 * This header indicates that the request's body was compressed with GZIP and that response's body should be compressed with GZIP either.
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class GZIPClientRequestFilter implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		requestContext.getHeaders().add(GZIPUtil.CLOUDSTORE_ENCODING_HEADER, GZIPUtil.CLOUDSTORE_ENCODING_HEADER_VALUE);
	}
}
