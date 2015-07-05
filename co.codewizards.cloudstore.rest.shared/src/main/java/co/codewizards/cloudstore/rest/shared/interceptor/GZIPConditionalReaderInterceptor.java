package co.codewizards.cloudstore.rest.shared.interceptor;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptorContext;

import co.codewizards.cloudstore.rest.shared.GZIPUtil;

/**
 * Interceptor decompressing request only if it was compressed
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class GZIPConditionalReaderInterceptor extends GZIPReaderInterceptor{

	@Override
	public Object aroundReadFrom(ReaderInterceptorContext context)
			throws IOException, WebApplicationException {
		if(GZIPUtil.isRequestCompressedWithGzip(context)){
			return super.aroundReadFrom(context);
		} else{
			return context.proceed();
		}
	}
}
