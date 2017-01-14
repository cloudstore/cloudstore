package co.codewizards.cloudstore.core.collection;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import co.codewizards.cloudstore.core.ref.IdentityWeakReference;

public class WeakIdentityHashMap<K, V> implements Map<K, V>, Serializable {
	private static final long serialVersionUID = 1L;

	private final ReferenceQueue<K> keyRefQueue = new ReferenceQueue<K>();
	private final HashMap<Reference<K>, V> delegate;
	private transient Set<Map.Entry<K, V>> entrySet;

	public WeakIdentityHashMap() {
		delegate = new HashMap<>();
	}

	public WeakIdentityHashMap(int initialCapacity) {
		delegate = new HashMap<>(initialCapacity);
	}

	public WeakIdentityHashMap(final Map<? extends K, ? extends V> map) {
		this(assertNotNull(map, "map").size());
		putAll(map);
	}

	public WeakIdentityHashMap(int initialCapacity, float loadFactor) {
		delegate = new HashMap<>(initialCapacity, loadFactor);
	}

	@Override
	public V get(final Object key) {
		expunge();
		@SuppressWarnings("unchecked")
		final WeakReference<K> keyRef = createReference((K) key);
		return delegate.get(keyRef);
	}

	@Override
	public V put(final K key, final V value) {
		expunge();
		assertNotNull(key, "key");
		final WeakReference<K> keyRef = createReference(key, keyRefQueue);
		return delegate.put(keyRef, value);
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		expunge();
		assertNotNull(map, "map");
		for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
			final K key = entry.getKey();
			assertNotNull(key, "entry.key");
			final WeakReference<K> keyRef = createReference(key, keyRefQueue);
			delegate.put(keyRef, entry.getValue());
		}
	}

	@Override
	public V remove(final Object key) {
		expunge();
		@SuppressWarnings("unchecked")
		final WeakReference<K> keyref = createReference((K) key);
		return delegate.remove(keyref);
	}

	@Override
	public void clear() {
		expunge();
		delegate.clear();
	}

	@Override
	public int size() {
		expunge();
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		expunge();
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(final Object key) {
		expunge();
		@SuppressWarnings("unchecked")
		final WeakReference<K> keyRef = createReference((K) key);
		return delegate.containsKey(keyRef);
	}

	@Override
	public boolean containsValue(final Object value) {
		expunge();
		return delegate.containsValue(value);
	}

	@Override
	public Set<K> keySet() {
		expunge();
		throw new UnsupportedOperationException("NYI"); // TODO implement this! It should be backed! Read javadoc for the proper contract!
	}

	@Override
	public Collection<V> values() {
		expunge();
		return delegate.values();
	}

    @Override
	public Set<Map.Entry<K,V>> entrySet() {
    	expunge();
        Set<Map.Entry<K,V>> es = entrySet;
        if (es != null)
            return es;
        else
            return entrySet = new EntrySet();
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
		public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        @Override
		public boolean contains(Object o) {
            if (! (o instanceof Map.Entry))
                return false;

            @SuppressWarnings("unchecked")
			final Map.Entry<K, V> entry = (Map.Entry<K, V>)o;

            final K keyParam = entry.getKey();
            if (! WeakIdentityHashMap.this.containsKey(keyParam))
            	return false;

            final V value = WeakIdentityHashMap.this.get(keyParam);
            return equal(value, entry.getValue());
        }

        @Override
		public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;

            @SuppressWarnings("unchecked")
			final Map.Entry<K, V> entry = (Map.Entry<K, V>)o;

            final K keyParam = entry.getKey();

            if (! WeakIdentityHashMap.this.containsKey(keyParam))
            	return false;

			WeakIdentityHashMap.this.remove(keyParam);
			return true;
        }

        @Override
		public int size() {
            return WeakIdentityHashMap.this.size();
        }

        @Override
		public void clear() {
        	WeakIdentityHashMap.this.clear();
        }

        /*
         * Must revert from AbstractSet's impl to AbstractCollection's, as
         * the former contains an optimization that results in incorrect
         * behavior when c is a smaller "normal" (non-identity-based) Set.
         */
        @Override
		public boolean removeAll(Collection<?> c) {
            boolean modified = false;
            for (Iterator<Map.Entry<K,V>> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
		public Object[] toArray() {
            int size = size();
            Object[] result = new Object[size];
            Iterator<Map.Entry<K,V>> it = iterator();
            for (int i = 0; i < size; i++)
                result[i] = new AbstractMap.SimpleEntry<>(it.next());
            return result;
        }

        @Override
		@SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size) a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            Iterator<Map.Entry<K,V>> it = iterator();
            for (int i = 0; i < size; i++)
                a[i] = (T) new AbstractMap.SimpleEntry<>(it.next());
            if (a.length > size)
                a[size] = null;
            return a;
        }
    }

    private class EntryIterator implements Iterator<Map.Entry<K, V>> {
    	private final Iterator<Map.Entry<Reference<K>, V>> delegateIterator = delegate.entrySet().iterator();
    	private Map.Entry<K, V> nextEntry;

		@Override
		public boolean hasNext() {
			if (nextEntry != null)
				return true;

			nextEntry = pullNext();
			return nextEntry != null;
		}

		@Override
		public Map.Entry<K, V> next() throws NoSuchElementException {
			Map.Entry<K, V> result = nextEntry;
			nextEntry = null;

			if (result == null) {
				result = pullNext();

				if (result == null)
					throw new NoSuchElementException();
			}
			return result;
		}

		private Map.Entry<K, V> pullNext() {
			while (delegateIterator.hasNext()) {
				final Map.Entry<Reference<K>, V> delegateNext = delegateIterator.next();
				final K key = delegateNext.getKey().get();
				if (key != null) {
					final V value = delegateNext.getValue();
					return new AbstractMap.SimpleEntry<K, V>(key, value) {
						private static final long serialVersionUID = 1L;
						@Override
						public V setValue(V value) {
							return delegateNext.setValue(value);
						}
					};
				}
			}
			return null;
		}

		@Override
		public void remove() throws IllegalStateException {
			delegateIterator.remove();
		}
    }

	private void expunge() {
		Reference<? extends K> keyRef;
		while ((keyRef = keyRefQueue.poll()) != null)
			delegate.remove(keyRef);
	}

	private WeakReference<K> createReference(K referent) {
		return new IdentityWeakReference<K>(referent);
	}

	private WeakReference<K> createReference(K referent, ReferenceQueue<K> q) {
		return new IdentityWeakReference<K>(referent, q);
	}
}
