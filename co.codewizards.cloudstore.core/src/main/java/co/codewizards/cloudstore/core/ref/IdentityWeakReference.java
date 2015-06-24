package co.codewizards.cloudstore.core.ref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class IdentityWeakReference<T> extends WeakReference<T> {
	private final int hashCode;

	public IdentityWeakReference(T o) {
		this(o, null);
	}

	public IdentityWeakReference(T o, ReferenceQueue<T> q) {
		super(o, q);
		this.hashCode = (o == null) ? 0 : System.identityHashCode(o);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;

		if (!(o instanceof IdentityWeakReference<?>))
			return false;

		final IdentityWeakReference<?> otherRef = (IdentityWeakReference<?>) o;
		final Object thisObject = get();
		return (thisObject != null && thisObject == otherRef.get());
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}