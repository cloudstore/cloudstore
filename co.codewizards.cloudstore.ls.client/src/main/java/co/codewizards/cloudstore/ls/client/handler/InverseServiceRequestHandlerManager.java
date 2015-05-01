package co.codewizards.cloudstore.ls.client.handler;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;

@SuppressWarnings("rawtypes")
public class InverseServiceRequestHandlerManager {

	private static final class HandlerClass implements Comparable<HandlerClass> {
		public final Class<? extends InverseServiceRequestHandler> handlerClass;
		public final int priority;

		public HandlerClass(final Class<? extends InverseServiceRequestHandler> handlerClass, final int priority) {
			this.handlerClass = assertNotNull("handlerClass", handlerClass);
			this.priority = priority;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>Important:</b> the implementation in {@code HandlerClass} sorts by priority first, then by name.
		 * The highest priority (greatest number) comes first!
		 */
		@Override
		public int compareTo(HandlerClass o) {
			int result = -1 * Integer.compare(this.priority, o.priority);
			if (result != 0)
				return result;

			return this.handlerClass.getName().compareTo(o.handlerClass.getName());
		}
	}

	private final Map<Class<?>, HandlerClass> requestType2HandlerClass = new HashMap<>();
	private final Map<Class<?>, Class<? extends InverseServiceRequestHandler>> resolvedRequestType2HandlerClassCache = new HashMap<>();

	private static final class Holder {
		public static final InverseServiceRequestHandlerManager instance = new InverseServiceRequestHandlerManager();
	}

	public static InverseServiceRequestHandlerManager getInstance() {
		return Holder.instance;
	}

	protected InverseServiceRequestHandlerManager() {
	}

	public InverseServiceRequestHandler getInverseServiceRequestHandler(Class<?> requestClass) {
		assertNotNull("requestClass", requestClass);

		final Class<? extends InverseServiceRequestHandler> handlerClass = getInverseServiceRequestHandlerClass(requestClass);
		if (handlerClass == null)
			return null;
		else
			return newInstance(handlerClass);
	}

	private synchronized Class<? extends InverseServiceRequestHandler> getInverseServiceRequestHandlerClass(Class<?> requestClass) {
		Class<? extends InverseServiceRequestHandler> result = resolvedRequestType2HandlerClassCache.get(requestClass);
		if (result == null) {
			final SortedSet<HandlerClass> handlerClasses = getInverseServiceRequestHandlerClasses(requestClass);
			if (handlerClasses.isEmpty())
				return null;

			result = handlerClasses.iterator().next().handlerClass;
		}
		return result;
	}

	private synchronized SortedSet<HandlerClass> getInverseServiceRequestHandlerClasses(final Class<?> requestClass) {
		assertNotNull("requestClass", requestClass);

		if (requestType2HandlerClass.isEmpty()) {
			final Iterator<InverseServiceRequestHandler> iterator = ServiceLoader.load(InverseServiceRequestHandler.class).iterator();
			while (iterator.hasNext()) {
				final InverseServiceRequestHandler handler = iterator.next();
				requestType2HandlerClass.put(handler.getInverseServiceRequestType(), new HandlerClass(handler.getClass(), handler.getPriority()));
			}
		}

		final SortedSet<HandlerClass> handlerClasses = new TreeSet<>();
		Class<?> c = requestClass;
		while (c != null && c != Object.class) {
			populateHandlerClasses(handlerClasses, c);
			c = c.getSuperclass();
		}
		return handlerClasses;
	}

	private void populateHandlerClasses(final Set<HandlerClass> handlerClasses, final Class<?> requestClass) {
		final HandlerClass handlerClass = requestType2HandlerClass.get(requestClass);
		if (handlerClass != null)
			handlerClasses.add(handlerClass);

		for (final Class<?> iface : requestClass.getInterfaces())
			populateHandlerClasses(handlerClasses, iface);
	}

	public InverseServiceRequestHandler getInverseServiceRequestHandlerOrFail(Class<?> requestClass) {
		final InverseServiceRequestHandler handler = getInverseServiceRequestHandler(requestClass);
		if (handler == null)
			throw new IllegalArgumentException("Could not find a handler for this requestClass: " + requestClass.getName());

		return handler;
	}

	public InverseServiceRequestHandler getInverseServiceRequestHandlerOrFail(final InverseServiceRequest request) {
		assertNotNull("request", request);
		return getInverseServiceRequestHandlerOrFail(request.getClass());
	}

	private InverseServiceRequestHandler newInstance(Class<? extends InverseServiceRequestHandler> handlerClass) {
		assertNotNull("handlerClass", handlerClass);
		try {
			return handlerClass.newInstance();
		} catch (final InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
