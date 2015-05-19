package co.codewizards.cloudstore.core.collection;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public abstract class LazyUnmodifiableList<E> implements List<E> {

	private List<E> elements;

	@Override
	public void forEach(Consumer<? super E> action) {
		getElements().forEach(action);
	}

	@Override
	public int size() {
		return getElements().size();
	}

	@Override
	public boolean isEmpty() {
		return getElements().isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return getElements().contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return getElements().iterator();
	}

	@Override
	public Object[] toArray() {
		return getElements().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return getElements().toArray(a);
	}

	@Override
	public boolean add(E e) {
		return getElements().add(e);
	}

	@Override
	public boolean remove(Object o) {
		return getElements().remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return getElements().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return getElements().addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return getElements().addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return getElements().removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return getElements().retainAll(c);
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		getElements().replaceAll(operator);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		return getElements().removeIf(filter);
	}

	@Override
	public void sort(Comparator<? super E> c) {
		getElements().sort(c);
	}

	@Override
	public void clear() {
		getElements().clear();
	}

	@Override
	public boolean equals(Object o) {
		return getElements().equals(o);
	}

	@Override
	public int hashCode() {
		return getElements().hashCode();
	}

	@Override
	public E get(int index) {
		return getElements().get(index);
	}

	@Override
	public E set(int index, E element) {
		return getElements().set(index, element);
	}

	@Override
	public void add(int index, E element) {
		getElements().add(index, element);
	}

	@Override
	public Stream<E> stream() {
		return getElements().stream();
	}

	@Override
	public E remove(int index) {
		return getElements().remove(index);
	}

	@Override
	public Stream<E> parallelStream() {
		return getElements().parallelStream();
	}

	@Override
	public int indexOf(Object o) {
		return getElements().indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return getElements().lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return getElements().listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return getElements().listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return getElements().subList(fromIndex, toIndex);
	}

	@Override
	public Spliterator<E> spliterator() {
		return getElements().spliterator();
	}

	protected List<E> getElements() {
		if (elements == null)
			elements = Collections.unmodifiableList(new ArrayList<E>(assertNotNull(getClass().getName() + ".loadElements()", loadElements())));

		return elements;
	}

	protected abstract Collection<E> loadElements();
}
