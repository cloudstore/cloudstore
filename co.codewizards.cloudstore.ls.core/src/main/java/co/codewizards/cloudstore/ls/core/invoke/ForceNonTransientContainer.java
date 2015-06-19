package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import co.codewizards.cloudstore.core.ls.NoObjectRef;

public class ForceNonTransientContainer implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Object transientFieldOwnerObject;

	private final Map<String, Object> transientFieldName2Value;

	public ForceNonTransientContainer(final Object transientFieldOwnerObject, Map<String, Object> transientFieldName2Value) {
		this.transientFieldOwnerObject = assertNotNull("transientFieldOwnerObject", transientFieldOwnerObject);
		this.transientFieldName2Value = new NoObjectRefMap<String, Object>(assertNotNull("transientFieldName2Value", transientFieldName2Value));
	}

	public Object getTransientFieldOwnerObject() {
		return transientFieldOwnerObject;
	}

	public Map<String, Object> getTransientFieldName2Value() {
		return transientFieldName2Value;
	}

	@NoObjectRef(inheritToObjectGraphChildren = false)
	private static final class NoObjectRefMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;

		public NoObjectRefMap(Map<? extends K, ? extends V> m) {
			super(m);
		}
	}
}
