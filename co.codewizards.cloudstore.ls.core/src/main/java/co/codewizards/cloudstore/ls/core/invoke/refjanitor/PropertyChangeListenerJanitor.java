package co.codewizards.cloudstore.ls.core.invoke.refjanitor;

import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static java.util.Objects.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.collection.WeakIdentityHashMap;
import co.codewizards.cloudstore.core.ref.IdentityWeakReference;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public class PropertyChangeListenerJanitor extends AbstractReferenceJanitor {

	private static final Logger logger = LoggerFactory.getLogger(PropertyChangeListenerJanitor.class);

	private final WeakIdentityHashMap<Object, Map<Object, List<IdentityWeakReference<PropertyChangeListener>>>> bean2Property2ListenerRefs = new WeakIdentityHashMap<>();
	private final WeakIdentityHashMap<PropertyChangeListener, WeakReference<FaultTolerantPropertyChangeListener>> originalListener2FaultTolerantPropertyChangeListenerRef =
			new WeakIdentityHashMap<>();

//	// needed to pin the original listener into memory as long as the FaultTolerantPropertyChangeListener is still needed.
//	private final WeakIdentityHashMap<FaultTolerantPropertyChangeListener, PropertyChangeListener> faultTolerantPropertyChangeListener2OriginalListener =
//			new WeakIdentityHashMap<>();

	@Override
	public void preInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final MethodInvocationRequest methodInvocationRequest = extMethodInvocationRequest.getMethodInvocationRequest();

		final Object bean = methodInvocationRequest.getObject(); // we don't support registering a PropertyChangeListener statically - or should we?!
		if (bean == null)
			return;

		final String methodName = methodInvocationRequest.getMethodName();
		final Object[] arguments = methodInvocationRequest.getArguments();

		Object property = null;
		PropertyChangeListener listener = null;
		if (arguments.length == 1 && arguments[0] instanceof PropertyChangeListener) {
			listener = (PropertyChangeListener) arguments[0];
			arguments[0] = getFaultTolerantPropertyChangeListenerOrCreate(listener);
		}
		else if (arguments.length == 2 && arguments[1] instanceof PropertyChangeListener) {
			listener = (PropertyChangeListener) arguments[1];
			if (arguments[0] instanceof PropertyBase)
				property = arguments[0];
			else if (arguments[0] instanceof String)
				property = arguments[0];
			else
				return;

			arguments[1] = getFaultTolerantPropertyChangeListenerOrCreate(listener);
		}
		else
			return;

		requireNonNull(listener, "listener");

		if ("addPropertyChangeListener".equals(methodName))
			trackAddPropertyChangeListener(bean, property, listener);
		else if ("removePropertyChangeListener".equals(methodName))
			trackRemovePropertyChangeListener(bean, property, listener);
	}

	private synchronized FaultTolerantPropertyChangeListener getFaultTolerantPropertyChangeListenerOrCreate(final PropertyChangeListener listener) {
		requireNonNull(listener, "listener");

		final WeakReference<FaultTolerantPropertyChangeListener> ref = originalListener2FaultTolerantPropertyChangeListenerRef.get(listener);
		FaultTolerantPropertyChangeListener faultTolerantListener = ref == null ? null : ref.get();
		if (faultTolerantListener == null) {
			faultTolerantListener = new FaultTolerantPropertyChangeListener(listener);
			originalListener2FaultTolerantPropertyChangeListenerRef.put(listener, new WeakReference<>(faultTolerantListener));
		}
		return faultTolerantListener;
	}

	private synchronized FaultTolerantPropertyChangeListener getFaultTolerantPropertyChangeListener(final PropertyChangeListener listener) {
		requireNonNull(listener, "listener");

		final WeakReference<FaultTolerantPropertyChangeListener> ref = originalListener2FaultTolerantPropertyChangeListenerRef.get(listener);
		final FaultTolerantPropertyChangeListener faultTolerantListener = ref == null ? null : ref.get();
		return faultTolerantListener;
	}

	@Override
	public void cleanUp() {
		final Map<Object, Map<Object, List<IdentityWeakReference<PropertyChangeListener>>>> bean2Property2ListenerRefs;
		synchronized (this) {
			bean2Property2ListenerRefs = new HashMap<>(this.bean2Property2ListenerRefs);
			this.bean2Property2ListenerRefs.clear();
		}

		for (final Map.Entry<Object, Map<Object, List<IdentityWeakReference<PropertyChangeListener>>>> me1 : bean2Property2ListenerRefs.entrySet()) {
			final Object bean = me1.getKey();
			if (bean == null)
				throw new IllegalStateException("bean2Property2ListenerRefs.entrySet() contained null-key!");

			for (final Map.Entry<Object, List<IdentityWeakReference<PropertyChangeListener>>> me2 : me1.getValue().entrySet()) {
				final Object property = me2.getKey();

				for (final IdentityWeakReference<PropertyChangeListener> ref : me2.getValue()) {
					final PropertyChangeListener listener = ref.get();
					if (listener != null)
						tryRemovePropertyChangeListener(bean, property, listener);
				}
			}
		}
	}

	private void tryRemovePropertyChangeListener(final Object bean, final Object property, final PropertyChangeListener listener) {
		requireNonNull(bean, "bean");
		requireNonNull(listener, "listener");

		final FaultTolerantPropertyChangeListener faultTolerantPropertyChangeListener = getFaultTolerantPropertyChangeListener(listener);
		if (faultTolerantPropertyChangeListener == null)
			return;

		try {
			if (property != null)
				invoke(bean, "removePropertyChangeListener", property, faultTolerantPropertyChangeListener);
			else
				invoke(bean, "removePropertyChangeListener", faultTolerantPropertyChangeListener);
		} catch (final Exception x) {
			logger.error("tryRemovePropertyChangeListener: " + x, x);
		}
	}

	private synchronized void trackAddPropertyChangeListener(final Object bean, final Object property, final PropertyChangeListener listener) {
		requireNonNull(bean, "bean");
		requireNonNull(listener, "listener");

		Map<Object, List<IdentityWeakReference<PropertyChangeListener>>> property2ListenerRefs = bean2Property2ListenerRefs.get(bean);
		if (property2ListenerRefs == null) {
			property2ListenerRefs = new HashMap<>();
			bean2Property2ListenerRefs.put(bean, property2ListenerRefs);
		}

		List<IdentityWeakReference<PropertyChangeListener>> listenerRefs = property2ListenerRefs.get(property);
		if (listenerRefs == null) {
			listenerRefs = new LinkedList<>();
			property2ListenerRefs.put(property, listenerRefs);
		}
		else
			expunge(listenerRefs);

		// PropertyChangeSupport.addPropertyChangeListener(...) causes the same listener to be added multiple times.
		// Hence, we do the same here: Add it once for each invocation.
		final IdentityWeakReference<PropertyChangeListener> listenerRef = new IdentityWeakReference<PropertyChangeListener>(listener);
		listenerRefs.add(listenerRef);
	}

	private synchronized void trackRemovePropertyChangeListener(final Object bean, final Object property, final PropertyChangeListener listener) {
		requireNonNull(bean, "bean");
		requireNonNull(listener, "listener");

		final Map<Object, List<IdentityWeakReference<PropertyChangeListener>>> property2ListenerRefs = bean2Property2ListenerRefs.get(bean);
		if (property2ListenerRefs == null)
			return;

		final List<IdentityWeakReference<PropertyChangeListener>> listenerRefs = property2ListenerRefs.get(property);
		if (listenerRefs == null)
			return;

		final IdentityWeakReference<PropertyChangeListener> listenerRef = new IdentityWeakReference<PropertyChangeListener>(listener);
		listenerRefs.remove(listenerRef);

		expunge(listenerRefs);

		if (listenerRefs.isEmpty())
			property2ListenerRefs.remove(property);

		if (property2ListenerRefs.isEmpty())
			bean2Property2ListenerRefs.remove(bean);
	}

	private void expunge(final List<IdentityWeakReference<PropertyChangeListener>> listenerRefs) {
		requireNonNull(listenerRefs, "listenerRefs");
		for (final Iterator<IdentityWeakReference<PropertyChangeListener>> it = listenerRefs.iterator(); it.hasNext();) {
			final IdentityWeakReference<PropertyChangeListener> ref = it.next();
			if (ref.get() == null)
				it.remove();
		}
	}

	private static class FaultTolerantPropertyChangeListener implements PropertyChangeListener {
		private static final Logger logger = LoggerFactory.getLogger(PropertyChangeListenerJanitor.FaultTolerantPropertyChangeListener.class);

		private final PropertyChangeListener delegate;

		public FaultTolerantPropertyChangeListener(final PropertyChangeListener delegate) {
			this.delegate = requireNonNull(delegate, "delegate");
		}

		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			try {
				delegate.propertyChange(event);
			} catch (final Exception x) {
				logger.error("propertyChange: " + x, x);
			}
		}

		@Override
		protected void finalize() throws Throwable {
			logger.debug("finalize: entered.");
			super.finalize();
		}
	}
}
