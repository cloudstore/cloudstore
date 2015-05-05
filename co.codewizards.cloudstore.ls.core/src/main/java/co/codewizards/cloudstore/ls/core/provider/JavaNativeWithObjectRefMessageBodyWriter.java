package co.codewizards.cloudstore.ls.core.provider;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.NoCloseOutputStream;
import co.codewizards.cloudstore.ls.core.invoke.ForceNonTransientClassSet;
import co.codewizards.cloudstore.ls.core.invoke.ForceNonTransientContainer;
import co.codewizards.cloudstore.ls.core.invoke.NoObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectGraphContainer;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverter;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverterFactory;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@Provider
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
public class JavaNativeWithObjectRefMessageBodyWriter
implements MessageBodyWriter<Object>
{
	private final ObjectRefConverterFactory objectRefConverterFactory;

	@Context
	private SecurityContext securityContext;

	public JavaNativeWithObjectRefMessageBodyWriter(final ObjectRefConverterFactory objectRefConverterFactory) {
		this.objectRefConverterFactory = assertNotNull("objectRefConverterFactory", objectRefConverterFactory);
	}

	@Override
	public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return -1;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		// We return always true, because we declared our media-type already in the @Produces above and thus don't need to check it here.
		// At least I hope we don't get consulted for media-types that were not declared in @Produces.
		return true;
	}

	@Override
	public void writeTo(
			Object t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream
			) throws IOException, WebApplicationException
	{
		final ObjectRefConverter objectRefConverter = objectRefConverterFactory.createObjectRefConverter(securityContext);
		final ObjectGraphContainer objectGraphContainer = new ObjectGraphContainer(t);
		final NoObjectRefAnalyser noObjectRefAnalyser = new NoObjectRefAnalyser(objectGraphContainer);
		try (ObjectOutputStream oout = new ReplacingObjectOutputStream(new NoCloseOutputStream(entityStream), objectRefConverter, noObjectRefAnalyser, objectGraphContainer);) {
			oout.writeObject(objectGraphContainer);
			oout.flush();
		}
	}

	private static class ReplacingObjectOutputStream extends ObjectOutputStream {
		private final NoObjectRefAnalyser noObjectRefAnalyser;
		private final ObjectRefConverter objectRefConverter;
		private final ObjectGraphContainer objectGraphContainer;

		public ReplacingObjectOutputStream(OutputStream out, final ObjectRefConverter objectRefConverter, final NoObjectRefAnalyser noObjectRefAnalyser, final ObjectGraphContainer objectGraphContainer) throws IOException {
			super(out);
			this.objectRefConverter = assertNotNull("objectRefConverter", objectRefConverter);
			this.noObjectRefAnalyser = assertNotNull("noObjectRefAnalyser", noObjectRefAnalyser);
			this.objectGraphContainer = assertNotNull("objectGraphContainer", objectGraphContainer);
			enableReplaceObject(true);
		}

		@Override
		protected Object replaceObject(final Object object) throws IOException {
			if (object == null || object instanceof ObjectRef || object instanceof Uid) // we replace the objects by ObjectRefs, hence we must ignore the replaced objects here - they are unknown to our analyser.
				return object;

			if (noObjectRefAnalyser.isNoObjectRef(object))
				return object;

			final Object result = objectRefConverter.convertToObjectRefIfNeeded(object);

			if (ForceNonTransientClassSet.getInstance().isForceNonTransientClass(result.getClass())) {
				final List<Field> transientFields = getTransientFields(result.getClass());
				if (!transientFields.isEmpty()) {
					final ForceNonTransientContainer forceNonTransientContainer = createForceNonTransientContainer(result, transientFields);
					objectGraphContainer.putForceNonTransientContainer(forceNonTransientContainer);
				}
			}
			return result;
		}

		private ForceNonTransientContainer createForceNonTransientContainer(final Object object, final List<Field> transientFields) {
			final Map<String, Object> transientFieldName2Value = new HashMap<>();
			for (final Field field : transientFields) {
				field.setAccessible(true);

				final Object fieldValue;
				try {
					fieldValue = field.get(object);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}

				final String fieldName = field.getDeclaringClass().getName() + '.' + field.getName();
				transientFieldName2Value.put(fieldName, fieldValue);
			}
			return new ForceNonTransientContainer(object, transientFieldName2Value);
		}

		private List<Field> getTransientFields(Class<?> clazz) {
			final List<Field> allDeclaredFields = getAllDeclaredFields(clazz);
			final List<Field> transientFields = new ArrayList<>();
			for (final Field field : allDeclaredFields) {
				if ((Modifier.TRANSIENT & field.getModifiers()) != 0)
					transientFields.add(field);
			}
			return transientFields;
		}
	}

	private static class NoObjectRefAnalyser {
		private static final Logger logger = LoggerFactory.getLogger(JavaNativeWithObjectRefMessageBodyWriter.NoObjectRefAnalyser.class);

		private final IdentityHashMap<Object, NoObjectRef> object2NoObjectRef = new IdentityHashMap<>();

		public NoObjectRefAnalyser(final Object root) {
			analyse(null, root, new IdentityHashMap<Object, Void>());
		}

		private void analyse(final Object parent, final Object object, final IdentityHashMap<Object, Void> processedObjects) {
			if (object == null)
				return;

			if (processedObjects.containsKey(object))
				return;

			logger.debug("analyse: object={}", object);

			processedObjects.put(object, null);

			final NoObjectRef noObjectRefVal = object2NoObjectRef.get(object);
			if (noObjectRefVal == null) {
				final NoObjectRef noObjectRef = object.getClass().getAnnotation(NoObjectRef.class);
				if (noObjectRef == null) {
					final NoObjectRef parentNoObjectRef = parent == null ? NoObjectRef.DEFAULT_IF_MISSING : object2NoObjectRef.get(parent);
					object2NoObjectRef.put(object, parentNoObjectRef);
				}
				else
					object2NoObjectRef.put(object, noObjectRef);
			}

			if (isTypeConsideredLeaf(object))
				return;

			if (object.getClass().isArray()) {
				// We do not treat children of a simple array, because they are no objects and ObjectOutputStream.replaceObject(...) should thus *not* be called for them.
				if (Object.class.isAssignableFrom(object.getClass().getComponentType())) {
					final int length = Array.getLength(object);
					for (int i = 0; i < length; ++i) {
						final Object child = Array.get(object, i);
						analyse(object, child, processedObjects);
					}
				}
			}
			else if (object instanceof Collection<?>) {
				final Collection<?> c = (Collection<?>) object;
				for (final Object child : c)
					analyse(object, child, processedObjects);
			}
			else if (object instanceof Map<?, ?>) {
				final Map<?, ?> m = (Map<?, ?>) object;
				for (Map.Entry<?, ?> me : m.entrySet()) {
					analyse(object, me.getKey(), processedObjects);
					analyse(object, me.getValue(), processedObjects);
				}
			} else {
				final List<Field> allDeclaredFields = getAllDeclaredFields(object.getClass());
				final List<Object> children = new ArrayList<Object>(allDeclaredFields.size());
				for (final Field field : allDeclaredFields) {
					if ((Modifier.STATIC & field.getModifiers()) != 0)
						continue;

					if ((Modifier.TRANSIENT & field.getModifiers()) != 0)
						continue;

					final Object child = getFieldValue(object, field);
					children.add(child);

					logger.debug("analyse: field={} child=", field, child);

					NoObjectRef noObjectRef = field.getAnnotation(NoObjectRef.class);
					if (noObjectRef == null)
						noObjectRef = child == null ? null : child.getClass().getAnnotation(NoObjectRef.class);

					if (noObjectRef == null) {
						final NoObjectRef parentNoObjectRef = object2NoObjectRef.get(object);
						if (parentNoObjectRef.inheritToObjectGraphChildren())
							object2NoObjectRef.put(child, parentNoObjectRef);
					}
					else
						object2NoObjectRef.put(child, noObjectRef);
				}

				for (final Object child : children)
					analyse(object, child, processedObjects);
			}
		}

		private Object getFieldValue(final Object object, final Field field) {
			field.setAccessible(true);
			try {
				return field.get(object);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean isNoObjectRef(Object object) {
			final NoObjectRef result = object2NoObjectRef.get(object);
			if (result == null) {
				final NoObjectRef noObjectRef = object.getClass().getAnnotation(NoObjectRef.class);
				if (noObjectRef != null)
					return noObjectRef.value();

				return false;
			}

			return result.value();
		}

		private boolean isTypeConsideredLeaf(final Object object) {
			final Class<?> clazz = object.getClass();

			// TODO do not hard-code the following, but use an advisor-service!
			return !clazz.getName().startsWith("co.codewizards.") && !clazz.getName().startsWith("org.");

//			return object instanceof BigDecimal
//					|| object instanceof BigInteger
//					|| object instanceof String
//					// BEGIN wrappers of simple types
//					|| object instanceof Boolean
//					|| object instanceof Byte
//					|| object instanceof Character
//					|| object instanceof Double
//					|| object instanceof Float
//					|| object instanceof Integer
//					|| object instanceof Long
//					|| object instanceof Short
//					// END wrappers of simple types
//					|| object instanceof Uid
//					|| object instanceof UUID
//					|| object instanceof PropertyChangeSupport
//					|| isProxy(object)
//					|| object instanceof Logger
//					|| object instanceof InvocationHandler
//					|| object instanceof Thread;
		}

//		private boolean isProxy(final Object object) {
//			final Class<?> clazz = object.getClass();
//
//			if (clazz.getSuperclass() == null) // without this, Proxy.isProxyClass(...) throws an exception - sometimes :-(
//				return false;
//
//			return Proxy.isProxyClass(clazz);
//		}
	}
}
