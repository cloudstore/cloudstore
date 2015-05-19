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

public abstract class LazyUnmodifiableList<E> {

	private List<E> elements;

	public void forEach(Consumer<? super E> action) {
		getElements().forEach(action);
	}

	public int size() {
		return getElements().size();
	}

	public boolean isEmpty() {
		return getElements().isEmpty();
	}

	public boolean contains(Object o) {
		return getElements().contains(o);
	}

	public Iterator<E> iterator() {
		return getElements().iterator();
	}

	public Object[] toArray() {
		return getElements().toArray();
	}

	public <T> T[] toArray(T[] a) {
		return getElements().toArray(a);
	}

	public boolean add(E e) {
		return getElements().add(e);
	}

	public boolean remove(Object o) {
		return getElements().remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return getElements().containsAll(c);
	}

	public boolean addAll(Collection<? extends E> c) {
		return getElements().addAll(c);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		return getElements().addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return getElements().removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return getElements().retainAll(c);
	}

	public void replaceAll(UnaryOperator<E> operator) {
		getElements().replaceAll(operator);
	}

	public boolean removeIf(Predicate<? super E> filter) {
		return getElements().removeIf(filter);
	}

	public void sort(Comparator<? super E> c) {
		getElements().sort(c);
	}

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

	public E get(int index) {
		return getElements().get(index);
	}

	public E set(int index, E element) {
		return getElements().set(index, element);
	}

	public void add(int index, E element) {
		getElements().add(index, element);
	}

	public Stream<E> stream() {
		return getElements().stream();
	}

	public E remove(int index) {
		return getElements().remove(index);
	}

	public Stream<E> parallelStream() {
		return getElements().parallelStream();
	}

	public int indexOf(Object o) {
		return getElements().indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return getElements().lastIndexOf(o);
	}

	public ListIterator<E> listIterator() {
		return getElements().listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		return getElements().listIterator(index);
	}

	public List<E> subList(int fromIndex, int toIndex) {
		return getElements().subList(fromIndex, toIndex);
	}

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
