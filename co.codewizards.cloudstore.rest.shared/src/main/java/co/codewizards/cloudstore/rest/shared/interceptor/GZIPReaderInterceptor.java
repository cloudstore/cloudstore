package co.codewizards.cloudstore.rest.shared.interceptor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GZIPReaderInterceptor implements ReaderInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(GZIPReaderInterceptor.class);

	@Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
		InputStream originalInputStream = context.getInputStream();

        if (!originalInputStream.markSupported())
        	originalInputStream = new BufferedInputStream(originalInputStream);

        // Test, if it contains data. We only try to unzip, if it is not empty.
        originalInputStream.mark(5);
        int read = originalInputStream.read();
        originalInputStream.reset();

        if (read > -1)
        	context.setInputStream(new GZIPInputStream(originalInputStream));
        else {
        	context.setInputStream(originalInputStream); // We might have wrapped it with our BufferedInputStream!
        	logger.debug("aroundReadFrom: originalInputStream is empty! Skipping GZIP.");
        }
        return context.proceed();
	}

}
