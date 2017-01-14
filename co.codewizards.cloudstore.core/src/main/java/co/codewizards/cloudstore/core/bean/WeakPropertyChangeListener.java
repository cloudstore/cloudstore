package co.codewizards.cloudstore.core.bean;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class WeakPropertyChangeListener implements PropertyChangeListener {
	private static ReferenceQueue<PropertyChangeListener> listenerRefQueue = new ReferenceQueue<PropertyChangeListener>();
	private static Map<Reference<PropertyChangeListener>, WeakPropertyChangeListener> listenerRef2WeakPropertyChangeListener =
			Collections.synchronizedMap(new IdentityHashMap<Reference<PropertyChangeListener>, WeakPropertyChangeListener>());

	private final Object bean;
	private final Object property;
	private final WeakReference<PropertyChangeListener> listenerRef;
	private boolean registered;

	public WeakPropertyChangeListener(final Object bean, final PropertyChangeListener listener) {
		this(bean, null, listener);
	}

	public WeakPropertyChangeListener(final Object bean, final Object property, final PropertyChangeListener listener) {
		expunge();

		this.bean = assertNotNull(bean, "bean");
		this.property = property;

		listenerRef = new WeakReference<PropertyChangeListener>(listener, listenerRefQueue);
		listenerRef2WeakPropertyChangeListener.put(listenerRef, this);
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		expunge();

		final PropertyChangeListener listener = listenerRef.get();
		if (listener != null)
			listener.propertyChange(event);
	}

	private static void expunge() {
		Reference<? extends PropertyChangeListener> ref;
		while ((ref = listenerRefQueue.poll()) != null) {
			final WeakPropertyChangeListener weakPropertyChangeListener = listenerRef2WeakPropertyChangeListener.remove(ref);

			if (weakPropertyChangeListener != null)
				weakPropertyChangeListener.removePropertyChangeListener();
		}
	}

	public synchronized void addPropertyChangeListener() {
		if (registered)
			return;

		if (property != null)
			invoke(bean, "addPropertyChangeListener", property, this);
		else
			invoke(bean, "addPropertyChangeListener", this);

		registered = true;
	}

	public synchronized void removePropertyChangeListener() {
		if (! registered)
			return;

		if (property != null)
			invoke(bean, "removePropertyChangeListener", property, this);
		else
			invoke(bean, "removePropertyChangeListener", this);

		registered = false;
	}
}
