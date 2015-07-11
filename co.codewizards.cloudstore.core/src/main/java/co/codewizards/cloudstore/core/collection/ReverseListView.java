package co.codewizards.cloudstore.core.collection;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * View reversing a given {@link List}.
 * <p>
 * In contrast to {@link Collections#reverse(List)}, wrapping a {@code ReverseListView} around another
 * {@link List} does not modify the wrapped {@link List}. It is fully backed by the original {@link List},
 * hence every write operation is written through and modifications to the underlying {@link List} are
 * immediately visible.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <E> the element type.
 */
public final class ReverseListView<E> implements List<E> {
	private final List<E> list;

	public ReverseListView(final List<E> list) {
		this.list = assertNotNull("list", list);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public final Iterator<E> iterator() {
		return listIterator(0);
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[size()]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length < list.size())
			a = (T[]) Array.newInstance(a.getClass(), list.size());

		int index = -1;
		for (final E e : this)
			a[++index] = (T) e;

		return a;
	}

	@Override
	public boolean add(E e) {
		list.add(0, e);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return false;

		for (E e : c)
			add(e);

		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		if (c.isEmpty())
			return false;

		for (E e : c)
			add(index++, e);

		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this)
			return true;

		if (! (o instanceof List))
			return false;

		final List<?> other = (List<?>) o;
		if (this.size() != other.size())
			return false;

		final Iterator<E> thisIterator = this.iterator();
		final Iterator<?> otherIterator = other.iterator();
		while (thisIterator.hasNext() && otherIterator.hasNext()) {
			final E thisElement = thisIterator.next();
			final Object otherElement = otherIterator.next();
			if (! equal(thisElement, otherElement))
				return false;
		}
		return thisIterator.hasNext() == otherIterator.hasNext();
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
	}

	@Override
	public E get(int index) {
		return list.get(translateIndex(index));
	}

	@Override
	public E set(int index, E element) {
		return list.set(translateIndex(index), element);
	}

	@Override
	public void add(int index, E element) {
		list.add(translateIndex(index), element);
	}

	private int translateIndex(final int index) {
		return list.size() - index - 1;
	}

	@Override
	public E remove(int index) {
		return list.remove(translateIndex(index));
	}

	@Override
	public int indexOf(Object o) {
		return translateIndex(list.lastIndexOf(o));
	}

	@Override
	public int lastIndexOf(Object o) {
		return translateIndex(list.indexOf(o));
	}

	@Override
	public final ListIterator<E> listIterator() {
		return listIterator(0);
	}

	@Override
	public final ListIterator<E> listIterator(final int index) {
		return new ListIterator<E>() {
            private final ListIterator<E> listIter = list.listIterator(translateIndex(index) + 1);

            @Override public boolean hasNext() { return listIter.hasPrevious(); }
            @Override public E next() { return listIter.previous(); }
            @Override public void remove() { listIter.remove(); }
			@Override public boolean hasPrevious() { return listIter.hasNext(); }
			@Override public E previous() { return listIter.next(); }
			@Override public int nextIndex() { return listIter.previousIndex(); }
			@Override public int previousIndex() { return listIter.nextIndex(); }
			@Override public void set(E e) { listIter.set(e); }

			@Override
			public void add(E e) {
				listIter.add(e);

				// According to javadoc, next() must be unaffected by add(...), while previous() must return the added element.
				// In order to comply with this contract, we must call this.next() (which is the delegate's previous()), now!
				if (e != next())
					throw new IllegalStateException("WTF?!");
			}
        };
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new ReverseListView<E>(list.subList(translateIndex(toIndex), translateIndex(fromIndex)));
	}

	@Override
	public String toString() {
		final Iterator<E> it = iterator();
		if (! it.hasNext())
			return "[]";

		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (;;) {
			final E e = it.next();
			sb.append(e == this || e == list ? "(this Collection)" : e);
			if (! it.hasNext())
				return sb.append(']').toString();
			sb.append(',').append(' ');
		}
	}
}
