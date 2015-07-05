package co.codewizards.cloudstore.rest.shared;

import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptorContext;

public class GZIPUtil {
	public static final String CLOUDSTORE_ENCODING_HEADER = "Cloudstore-Content-Encoding";
	public static final String CLOUDSTORE_ENCODING_HEADER_VALUE = "gzip";

	public static boolean isRequestCompressedWithGzip(ReaderInterceptorContext context){
		return CLOUDSTORE_ENCODING_HEADER_VALUE.equals(
				context.getProperty(CLOUDSTORE_ENCODING_HEADER));
	}

	public static boolean isRequestCompressedWithGzip(WriterInterceptorContext context){
		return CLOUDSTORE_ENCODING_HEADER_VALUE.equals(
				context.getProperty(CLOUDSTORE_ENCODING_HEADER));
	}

	public static boolean isRequestCompressedWithGzip(ContainerRequestContext requestContext){
		MultivaluedMap<String, String> headers = requestContext.getHeaders();
		List<String> encodingHeaderValues = headers.get(CLOUDSTORE_ENCODING_HEADER);
		if(encodingHeaderValues == null || encodingHeaderValues.size() != 1){
			return false;
		}
		return CLOUDSTORE_ENCODING_HEADER_VALUE.equals(encodingHeaderValues.get(0));
	}
}
