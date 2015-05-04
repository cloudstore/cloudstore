package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.Map;

public class ForceNonTransientContainer implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Object objectWithTransientFields;
	private final Map<String, Object> transientFieldName2Value;

	public ForceNonTransientContainer(final Object objectWithTransientFields, Map<String, Object> transientFieldName2Value) {
		this.objectWithTransientFields = assertNotNull("objectWithTransientFields", objectWithTransientFields);
		this.transientFieldName2Value = assertNotNull("transientFieldName2Value", transientFieldName2Value);
	}

	public Object getObjectWithTransientFields() {
		return objectWithTransientFields;
	}

	public Map<String, Object> getTransientFieldName2Value() {
		return transientFieldName2Value;
	}
}
