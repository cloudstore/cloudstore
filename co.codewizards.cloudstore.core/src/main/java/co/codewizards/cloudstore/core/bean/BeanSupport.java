package co.codewizards.cloudstore.core.bean;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class BeanSupport<B, P extends PropertyBase> {
	private final B bean;
	private final PropertyChangeSupport propertyChangeSupport;

	public BeanSupport(final B bean) {
		this.bean = assertNotNull(bean, "bean");
		propertyChangeSupport = new PropertyChangeSupport(bean);
	}

	public B getBean() {
		return bean;
	}

	public void setPropertyValue(final P property, final Object value) {
		assertNotNull(property, "property");

		final Object old;
		synchronized (getMutex()) {
			old = getFieldValue(bean, property.name());
			if (isEqual(property, old, value))
				return;

			setFieldValue(bean, property.name(), value);
		}

		// We *must* fire the event *outside* of the *synchronized* block to make sure the listeners
		// do not run into a dead-lock!
		firePropertyChange(property, old, value);
	}

	public <V> V getPropertyValue(P property) {
		synchronized (getMutex()) {
			return getFieldValue(bean, property.name());
		}
	}

	protected Object getMutex() {
		return bean;
	}

	protected boolean isEqual(final P property, final Object oldValue, final Object newValue) {
		return equal(oldValue, newValue);
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		assertNotNull(listener, "listener");
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		assertNotNull(listener, "listener");
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(final P property, final PropertyChangeListener listener) {
		assertNotNull(property, "property");
		assertNotNull(listener, "listener");
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	public void removePropertyChangeListener(final P property, final PropertyChangeListener listener) {
		assertNotNull(property, "property");
		assertNotNull(listener, "listener");
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	public void firePropertyChange(final P property, Object oldValue, Object newValue) {
		assertNotNull(property, "property");
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}
