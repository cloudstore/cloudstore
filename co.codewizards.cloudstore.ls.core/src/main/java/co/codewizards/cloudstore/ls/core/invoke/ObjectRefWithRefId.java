package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import java.io.Serializable;

import co.codewizards.cloudstore.core.Uid;

public class ObjectRefWithRefId implements Serializable {
	private static final long serialVersionUID = 1L;

	public final Object object;
	public final Uid refId;

	public ObjectRefWithRefId(final Object object, final Uid refId) {
		this.object = requireNonNull(object, "object");
		this.refId = requireNonNull(refId, "refId");
	}
}