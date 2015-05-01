package co.codewizards.cloudstore.test.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ExampleServiceImpl implements ExampleService {

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private String stringValue;

	private long longValue;

	@Override
	public String getStringValue() {
		return stringValue;
	}
	@Override
	public void setStringValue(String stringValue) {
		String old = this.stringValue;
		this.stringValue = stringValue;
		firePropertyChange(PropertyEnum.stringValue, old, stringValue);
	}

	@Override
	public long getLongValue() {
		return longValue;
	}
	@Override
	public void setLongValue(long longValue) {
		long old = this.longValue;
		this.longValue = longValue;
		firePropertyChange(PropertyEnum.longValue, old, longValue);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}
	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}
	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}
