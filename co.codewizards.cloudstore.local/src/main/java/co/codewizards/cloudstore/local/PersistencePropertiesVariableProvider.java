package co.codewizards.cloudstore.local;

import java.util.Map;

public interface PersistencePropertiesVariableProvider {

	/**
	 * The higher the priority (= the greater this value), the later this instance is
	 * invoked (and can thus overwrite older values in the {@code variableMap}).
	 * 
	 * @return the priority of this provider.
	 */
	int getPriority();

	void populatePersistencePropertiesVariableMap(Map<String, Object> variableMap);

}
