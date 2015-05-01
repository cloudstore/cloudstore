package co.codewizards.cloudstore.test.model;

import java.beans.PropertyChangeListener;

import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface ExampleService {

	interface Property extends PropertyBase {
	}

	enum PropertyEnum implements Property {
		stringValue,
		longValue
	}

	String getStringValue();

	void setStringValue(String stringValue);

	long getLongValue();

	void setLongValue(long longValue);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}
