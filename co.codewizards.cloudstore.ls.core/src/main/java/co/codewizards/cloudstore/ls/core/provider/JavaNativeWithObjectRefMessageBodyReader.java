package co.codewizards.cloudstore.ls.core.provider;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import co.codewizards.cloudstore.core.io.NoCloseInputStream;
import co.codewizards.cloudstore.ls.core.invoke.ForceNonTransientContainer;
import co.codewizards.cloudstore.ls.core.invoke.ObjectGraphContainer;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverter;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverterFactory;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@Provider
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
public class JavaNativeWithObjectRefMessageBodyReader
implements MessageBodyReader<Object>
{
	private final ObjectRefConverterFactory objectRefConverterFactory;

	@Context
	private SecurityContext securityContext;

	public JavaNativeWithObjectRefMessageBodyReader(final ObjectRefConverterFactory objectRefConverterFactory) {
		this.objectRefConverterFactory = assertNotNull(objectRefConverterFactory, "objectRefConverterFactory");
	}

	@Override
	public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
		// We return always true, because we declared our media-type already in the @Consumes above and thus don't need to check it here.
		// At least I hope we don't get consulted for media-types that were not declared in @Consumes.
		return true;
	}

	@Override
	public Object readFrom(
			final Class<Object> type, final Type genericType,
			final Annotation[] annotations, final MediaType mediaType,
			final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream
			)
					throws IOException, WebApplicationException
	{
		final ObjectRefConverter objectRefConverter = objectRefConverterFactory.createObjectRefConverter(securityContext);
		try (ObjectInputStream oin = new ResolvingObjectInputStream(new NoCloseInputStream(entityStream), objectRefConverter);) {
			final Object o = oin.readObject();
			final ObjectGraphContainer objectGraphContainer = (ObjectGraphContainer) o;

			for (ForceNonTransientContainer forceNonTransientContainer : objectGraphContainer.getTransientFieldOwnerObject2ForceNonTransientContainer().values())
				restoreTransientFields(forceNonTransientContainer);

			return objectGraphContainer.getRoot();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}

	}

	private void restoreTransientFields(final ForceNonTransientContainer container) {
		final Object ownerObject = container.getTransientFieldOwnerObject();

		for (final Map.Entry<String, Object> me : container.getTransientFieldName2Value().entrySet()) {
			final String qualifiedFieldName = me.getKey();
			final Object fieldValue = me.getValue();
			setFieldValue(ownerObject, qualifiedFieldName, fieldValue);
		}
	}

	private static class ResolvingObjectInputStream extends ExtObjectInputStream {
		private final ObjectRefConverter objectRefConverter;

		public ResolvingObjectInputStream(final InputStream in, final ObjectRefConverter objectRefConverter) throws IOException {
			super(in);
			this.objectRefConverter = assertNotNull(objectRefConverter, "objectRefConverter");
			enableResolveObject(true);
		}

		@Override
		protected Object resolveObject(Object object) throws IOException {
			final Object result = objectRefConverter.convertFromObjectRefIfNeeded(object);
			return result;
		}
	}
}
