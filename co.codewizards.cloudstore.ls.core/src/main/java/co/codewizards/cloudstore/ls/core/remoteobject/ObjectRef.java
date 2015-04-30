package co.codewizards.cloudstore.ls.core.remoteobject;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;

import co.codewizards.cloudstore.core.dto.Uid;

public class ObjectRef implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Uid clientId;
	private final long objectId;

	public ObjectRef(final Uid clientId, final long objectId) {
		this.clientId = assertNotNull("clientId", clientId);
		this.objectId = objectId;
	}

	public Uid getClientId() {
		return clientId;
	}
	public long getObjectId() {
		return objectId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + clientId.hashCode();
		result = prime * result + (int) (objectId ^ (objectId >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ObjectRef other = (ObjectRef) obj;
		return this.objectId == other.objectId && this.clientId.equals(other.clientId);
	}

	@Override
	public String toString() {
		return String.format("%s[%s, %s]", this.getClass().getSimpleName(), clientId, objectId);
	}
}
