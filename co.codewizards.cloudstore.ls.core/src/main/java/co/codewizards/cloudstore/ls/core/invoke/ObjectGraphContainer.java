package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import co.codewizards.cloudstore.core.ls.NoObjectRef;

public class ObjectGraphContainer implements Serializable {
	private static final long serialVersionUID = 1L;

	private Object root;

	@NoObjectRef(inheritToObjectGraphChildren = false)
	private IdentityHashMap<Object, ForceNonTransientContainer> transientFieldOwnerObject2ForceNonTransientContainer = new IdentityHashMap<Object, ForceNonTransientContainer>();

	public ObjectGraphContainer(final Object root) {
		this.root = assertNotNull("root", root);
	}

	public Object getRoot() {
		return root;
	}

	public void putForceNonTransientContainer(final ForceNonTransientContainer forceNonTransientContainer) {
		assertNotNull("forceNonTransientContainer", forceNonTransientContainer);
		transientFieldOwnerObject2ForceNonTransientContainer.put(forceNonTransientContainer.getTransientFieldOwnerObject(), forceNonTransientContainer);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(root);
		out.writeObject(transientFieldOwnerObject2ForceNonTransientContainer);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		root = in.readObject();

		@SuppressWarnings("unchecked")
		final IdentityHashMap<Object, ForceNonTransientContainer> m = (IdentityHashMap<Object, ForceNonTransientContainer>) in.readObject();
		transientFieldOwnerObject2ForceNonTransientContainer = m;
	}

	@SuppressWarnings("unused") // seems, Eclipse does not (yet) know this (new?!) serialization method
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException("WTF?!");
	}

	public Map<Object, ForceNonTransientContainer> getTransientFieldOwnerObject2ForceNonTransientContainer() {
		return Collections.unmodifiableMap(transientFieldOwnerObject2ForceNonTransientContainer);
	}
}
