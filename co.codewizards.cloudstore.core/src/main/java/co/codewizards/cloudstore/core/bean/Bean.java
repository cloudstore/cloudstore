package co.codewizards.cloudstore.core.bean;

import java.beans.PropertyChangeListener;

public interface Bean<P extends PropertyBase> extends Cloneable {

	void addPropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(P property, PropertyChangeListener listener);

	void removePropertyChangeListener(P property, PropertyChangeListener listener);

	Object clone();
}
