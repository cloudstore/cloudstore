package co.codewizards.cloudstore.core.collection;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper class merging a given source-{@code List} into a given destination-{@code List}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <E>
 * @param <K>
 */
public abstract class ListMerger<E, K> {

	private List<E> source;
	private List<E> dest;

	private Map<K, List<E>> sourceKey2elements;
	private Map<K, List<E>> destKey2elements;

	public void merge(final List<E> source, final List<E> dest) {
		this.source = assertNotNull("source", source);
		this.dest = assertNotNull("dest", dest);

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

//		populateSourceKey2element(); // no need to re-create, because it should be correctly updated, before
//		populateDestKey2element();

		int index = -1;
		for (final E sourceElement : source) {
			++index;
			final K sourceKey = getKey(sourceElement);
//			final List<E> sourceElements = nullToEmptyList(sourceKey2elements.get(sourceKey));
			final List<E> destElements = nullToEmptyList(destKey2elements.get(sourceKey));

//			if (sourceElements.size() == destElements.size()) {
//				mergeElementsWithSameKey(sourceKey, sourceElements, destElements);
//				sourceElements.clear();
//				destElements.clear();
//				continue;
//			}

			E destElement = dest.size() <= index ? null : dest.get(index);
			K destKey = destElement == null ? null : getKey(destElement);
			if (equal(sourceKey, destKey)) {
				merge(sourceElement, destElement);
				destElements.remove(destElement);
				continue;
			}
			destElement = null; destKey = null;

			if (! destElements.isEmpty()) {
				destElement = destElements.remove(0);
				dest.remove(destElement);
				dest.add(index, destElement);
				merge(sourceElement, destElement);
				continue;
			}

			add(dest, index, sourceElement);
		}
	}

	private <T> List<T> nullToEmptyList(final List<T> list) {
		return list == null ? Collections.<T>emptyList() : list;
	}

	protected void add(List<E> dest, int index, E element) {
		dest.add(index, element);
	}

	protected void mergeElementsWithSameKey(final K key, List<E> sourceElements, List<E> destElements) {
		final Iterator<E> s = sourceElements.iterator();
		final Iterator<E> d = destElements.iterator();

		while (s.hasNext()) {
			if (! d.hasNext())
				throw new IllegalStateException("destElements contains less elements than sourceElements!");

			final E sourceElement = s.next();
			final E destElement = d.next();
			merge(sourceElement, destElement);
		}

		if (d.hasNext())
			throw new IllegalStateException("destElements contains more elements than sourceElements!");
	}

	protected abstract K getKey(E element);
	protected abstract void merge(E sourceElement, E destElement);

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
