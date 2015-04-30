package co.codewizards.cloudstore.ls.core.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import co.codewizards.cloudstore.core.io.NoCloseInputStream;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@Provider
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE)
public class JavaNativeMessageBodyReader
implements MessageBodyReader<Object>
{
	private static volatile ClassLoader classLoader;

	public static ClassLoader getClassLoader() {
		return classLoader;
	}
	public static void setClassLoader(ClassLoader classLoader) {
		JavaNativeMessageBodyReader.classLoader = classLoader;
	}

	private String getLogPrefix()
	{
		return "(" + Integer.toHexString(System.identityHashCode(this)) + ") "; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public JavaNativeMessageBodyReader() {
		System.out.println(JavaNativeMessageBodyReader.class.getName() + getLogPrefix() + ": instantiated."); //$NON-NLS-1$
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
		try (ObjectInputStream oin = new MyObjectInputStream(new NoCloseInputStream(entityStream));) {
			final Object entity = oin.readObject();
			return entity;
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

	private static final class MyObjectInputStream extends ObjectInputStream
	{
		private ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		private ClassLoader staticClassLoader = JavaNativeMessageBodyReader.getClassLoader();

		public MyObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
		{
			try {
				return super.resolveClass(desc);
			} catch (final ClassNotFoundException x) {
				doNothing(); // try again with ContextClassLoader
			} catch (final NoClassDefFoundError x) {
				doNothing(); // try again with ContextClassLoader
			}

			try {
				return Class.forName(desc.getName(), false, contextClassLoader);
			} catch (final ClassNotFoundException x) {
				if (staticClassLoader == null)
					throw x;
			} catch (final NoClassDefFoundError x) {
				if (staticClassLoader == null)
					throw x;
			}

			return Class.forName(desc.getName(), false, staticClassLoader);
		}
	}
	private static final void doNothing() { }
}
