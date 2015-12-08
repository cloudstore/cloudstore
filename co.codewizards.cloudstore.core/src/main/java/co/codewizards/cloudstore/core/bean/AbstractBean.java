package co.codewizards.cloudstore.core.bean;

import java.beans.PropertyChangeListener;

public abstract class AbstractBean<P extends PropertyBase> implements CloneableBean<P>, Cloneable {
	private BeanSupport<AbstractBean<P>, P> beanSupport = new BeanSupport<AbstractBean<P>, P>(this);

	protected void setPropertyValue(P property, Object value) {
		beanSupport.setPropertyValue(property, value);
	}

	protected <V> V getPropertyValue(P property) {
		return beanSupport.getPropertyValue(property);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		beanSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		beanSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(P property, PropertyChangeListener listener) {
		beanSupport.addPropertyChangeListener(property, listener);
	}

	@Override
	public void removePropertyChangeListener(P property, PropertyChangeListener listener) {
		beanSupport.removePropertyChangeListener(property, listener);
	}

	protected void firePropertyChange(final P property, Object oldValue, Object newValue) {
		beanSupport.firePropertyChange(property, oldValue, newValue);
	}

	@Override
	public Object clone() { // returning Object to allow sub-classes to declare returning an interface or whatever they want (and fits)
		final AbstractBean<P> clone;
		try {
			@SuppressWarnings("unchecked")
			final AbstractBean<P> c = (AbstractBean<P>) super.clone();
			clone = c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.beanSupport = new BeanSupport<AbstractBean<P>, P>(clone);
		return clone;
	}
}
