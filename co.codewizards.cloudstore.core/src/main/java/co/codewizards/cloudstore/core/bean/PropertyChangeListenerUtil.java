package co.codewizards.cloudstore.core.bean;

import java.beans.PropertyChangeListener;

public class PropertyChangeListenerUtil {

	private PropertyChangeListenerUtil() {
	}

	public static WeakPropertyChangeListener addWeakPropertyChangeListenerNonTypeSafe(Object bean, Object property, PropertyChangeListener listener) {
		final WeakPropertyChangeListener weakListener = new WeakPropertyChangeListener(bean, property, listener);
		weakListener.addPropertyChangeListener();
		return weakListener;
	}

	public static <P extends PropertyBase, B extends Bean<P>> WeakPropertyChangeListener addWeakPropertyChangeListener(B bean, P property, PropertyChangeListener listener) {
		final WeakPropertyChangeListener weakListener = new WeakPropertyChangeListener(bean, property, listener);
		weakListener.addPropertyChangeListener();
		return weakListener;
	}

	public static WeakPropertyChangeListener addWeakPropertyChangeListener(Object bean, PropertyChangeListener listener) {
		final WeakPropertyChangeListener weakListener = new WeakPropertyChangeListener(bean, null, listener);
		weakListener.addPropertyChangeListener();
		return weakListener;
	}
}
