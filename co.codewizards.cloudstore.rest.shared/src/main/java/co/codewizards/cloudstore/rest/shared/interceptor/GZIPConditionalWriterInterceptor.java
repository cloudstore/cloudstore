package co.codewizards.cloudstore.rest.shared.interceptor;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptorContext;

import co.codewizards.cloudstore.rest.shared.GZIPUtil;

/**
 * Interceptor compressing response only if corresponding request was also compressed
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class GZIPConditionalWriterInterceptor extends GZIPWriterInterceptor{

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
		if(GZIPUtil.isRequestCompressedWithGzip(context)){
			super.aroundWriteTo(context);
		}
	}
}
