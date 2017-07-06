package co.codewizards.cloudstore.core.util;

import java.util.Map;

public class Pair<K, V> implements Map.Entry<K, V> {
	
	private final K key;
	
	private final V value;
	
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException("Pair is read-only!");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		final Pair<?, ?> other = (Pair<?, ?>) obj;
		return equal(this.key, other.key) && equal(this.value, other.value);
	}

	private static final boolean equal(final Object one, final Object two) {
		return one == null ? two == null : one.equals(two);
	}

	@Override
	public String toString() {
		return String.format("%s[%s, %s]", getClass().getSimpleName(), key, value);
	}
}
