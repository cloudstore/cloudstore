package co.codewizards.cloudstore.core.collection;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper {@linkplain #merge(List, List) merging} a given source-{@code List} into a given destination-{@code List}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <E> the element type.
 * @param <K> the key type (either the same as the element or a key contained in each list element).
 */
public abstract class ListMerger<E, K> {

	private List<E> source;
	private List<E> dest;

	private Map<K, List<E>> sourceKey2elements;
	private Map<K, List<E>> destKey2elements;

	/**
	 * Merge the given source into the given destination.
	 * <p>
	 * After this operation, both lists are semantically equal. This does not mean that their
	 * {@code equals(...)} method returns true, though! This is, because the lists are merged
	 * based on a key which might be wrapped by the elements. The elements are not required
	 * to correctly implement {@code equals(...)}.
	 *
	 * @param source the source from which to copy. Must not be <code>null</code>.
	 * @param dest the destination into which to write. Must not be <code>null</code>.
	 */
	public void merge(final List<E> source, final List<E> dest) {
		this.source = assertNotNull(source, "source");
		this.dest = assertNotNull(dest, "dest");

		populateSourceKey2element();
		populateDestKey2element();

		final List<E> destElementsToRemove = new LinkedList<>();
		for (E destElement : dest) {
			final K sourceKey = getKey(destElement);
			final List<E> sourceElements = nullToEmptyList(sourceKey2elements.get(sourceKey));
			final List<E> destElements = nullToEmptyList(destKey2elements.get(sourceKey));

			final int elementsToRemoveQty = destElements.size() - sourceElements.size();
			if (elementsToRemoveQty > 0) {
				for (int i = 0; i < elementsToRemoveQty; ++i) {
					final E removed = destElements.remove(0);
					destElementsToRemove.add(removed);
				}
			}
		}
//		dest.removeAll(destElementsToRemove);
		// removeAll(...) does *not* work, because it removes all occurrences that are equal to one element-to-be-removed!
		// Instead we want to remove exactly one element for each element to be removed! The following 2 lines do this:
		for (final E e : destElementsToRemove)
			dest.remove(e);

		int index = -1;
		for (final E sourceElement : source) {
			++index;
			final K sourceKey = getKey(sourceElement);
			final List<E> destElements = nullToEmptyList(destKey2elements.get(sourceKey));

			E destElement = dest.size() <= index ? null : dest.get(index);
			K destKey = destElement == null ? null : getKey(destElement);
			if (equal(sourceKey, destKey)) {
				update(dest, index, sourceElement, destElement);
				destElements.remove(destElement);
				continue;
			}
			destElement = null; destKey = null;

			if (! destElements.isEmpty()) {
				destElement = destElements.remove(0);
				final int lastIndexOf = dest.lastIndexOf(destElement);
				dest.remove(lastIndexOf);
				dest.add(index, destElement);
				update(dest, index, sourceElement, destElement);
				continue;
			}

			add(dest, index, sourceElement);
		}
	}

	private <T> List<T> nullToEmptyList(final List<T> list) {
		return list == null ? Collections.<T>emptyList() : list;
	}

	/**
	 * Add the given {@code element} to the given destination-{@code List} {@code dest} at the specified {@code index}.
	 * <p>
	 * The default implementation simply calls: {@code dest.add(index, element);}
	 *
	 * @param dest the destination. Never <code>null</code>.
	 * @param index the index at which the new element should be added.
	 * @param element the element to be added.
	 */
	protected void add(final List<E> dest, final int index, final E element) {
		dest.add(index, element);
	}

	/**
	 * Get the key from the given {@code element}.
	 * <p>
	 * If the element is the same as the key, this method should return the given {@code element}.
	 * @param element the element from which to extract the key. May be <code>null</code>, if the
	 * source or the destination {@code List} contains <code>null</code> elements.
	 * @return the key. May be <code>null</code>.
	 */
	protected abstract K getKey(E element);

	/**
	 * Update the the given {@code destElement} with the data from {@code sourceElement}; or replace it altogether.
	 * <p>
	 * Depending on whether the elements wrap the actual information in a mutable way, or whether they
	 * are immutable, this method may either copy the data from the {@code sourceElement} into the {@code destElement}
	 * or instead invoke {@link List#set(int, Object) dest.set(index, sourceElement)}.
	 * <p>
	 * <b>Important:</b> This method is only invoked, if the {@link #getKey(Object) key} of both
	 * {@code sourceElement} and {@code destElement} is the same!
	 *
	 * @param dest the destination {@code List}. Never <code>null</code>.
	 * @param index the index in {@code dest} addressing the element to be replaced.
	 * @param sourceElement the source from which to copy. May be <code>null</code>, if the source contains
	 * <code>null</code> elements.
	 * @param destElement the destination into which to write. May be <code>null</code>, if the destination contains
	 * <code>null</code> elements.
	 */
	protected abstract void update(List<E> dest, int index, E sourceElement, E destElement);

	protected void populateSourceKey2element() {
		sourceKey2elements = new HashMap<>();
		for (final E element : source) {
			final K key = getKey(element);
			List<E> elements = sourceKey2elements.get(key);
			if (elements == null) {
				elements = new LinkedList<>();
				sourceKey2elements.put(key, elements);
			}
			elements.add(element);
		}
	}

	protected void populateDestKey2element() {
		destKey2elements = new HashMap<>();
		for (E element : dest) {
			final K key = getKey(element);
			List<E> elements = destKey2elements.get(key);
			if (elements == null) {
				elements = new LinkedList<>();
				destKey2elements.put(key, elements);
			}
			elements.add(element);
		}
	}
}
