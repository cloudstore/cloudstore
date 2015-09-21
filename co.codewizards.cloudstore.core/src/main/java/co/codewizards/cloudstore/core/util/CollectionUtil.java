package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CollectionUtil {

	private CollectionUtil() {
	}

	/**
	 * Splits a given {@code Set} into multiple {@code Set}s, each with the maximum size specified.
	 * <p>
	 * For example, imagine a {@code Set} with 26 elements. Invoking this method with {@code maxSize == 10}
	 * will cause a resulting {@code List} having 3 {@code Set} elements. The first 2 of the {@code Set}
	 * elements each have 10 elements, while the 3rd one will have only 6 elements.
	 * <p>
	 * Please note that the order is maintained ({@link LinkedHashSet} is used internally for each
	 * segment-{@code Set}).
	 *
	 * @param inputSet the {@code Set} to be split. Must not be <code>null</code>. This {@code Set} is not
	 * modified in any way by this method.
	 * @param maxSize the maximum size of each resulting segment-{@code Set}. Must be greater than 0.
	 * @return a {@code List} containing all elements from the given {@code inputSet} in the same order,
	 * grouped in groups each not greater than {@code maxSize}. Never <code>null</code>.
	 * @see #splitList(List, int)
	 */
	public static <E> List<Set<E>> splitSet(final Set<E> inputSet, final int maxSize) {
		assertNotNull("inputSet", inputSet);
		if (maxSize < 1)
			throw new IllegalArgumentException("maxSize < 1");

		final List<Set<E>> result = new ArrayList<>(inputSet.size() / maxSize + 1);
		Set<E> current = null;
		for (final E element : inputSet) {
			if (current == null || current.size() >= maxSize) {
				current = new LinkedHashSet<E>(maxSize);
				result.add(current);
			}
			current.add(element);
		}
		return result;
	}

	/**
	 * Splits a given {@code List} into multiple {@code List}s, each with the maximum size specified.
	 * <p>
	 * For example, imagine a {@code List} with 26 elements. Invoking this method with {@code maxSize == 10}
	 * will cause a resulting {@code List} having 3 {@code List} elements. The first 2 of the {@code List}
	 * elements each have 10 elements, while the 3rd one will have only 6 elements.
	 * <p>
	 * Please note that the order is maintained.
	 *
	 * @param inputList the {@code List} to be split. Must not be <code>null</code>. This {@code List} is not
	 * modified in any way by this method.
	 * @param maxSize the maximum size of each resulting segment-{@code List}. Must be greater than 0.
	 * @return a {@code List} containing all elements from the given {@code inputList} in the same order,
	 * grouped in groups each not greater than {@code maxSize}. Never <code>null</code>.
	 * @see #splitSet(Set, int)
	 */
	public static <E> List<List<E>> splitList(final List<E> inputList, final int maxSize) {
		assertNotNull("inputList", inputList);
		if (maxSize < 1)
			throw new IllegalArgumentException("maxSize < 1");

		final List<List<E>> result = new ArrayList<>(inputList.size() / maxSize + 1);
		List<E> current = null;
		for (final E element : inputList) {
			if (current == null || current.size() >= maxSize) {
				current = new ArrayList<E>(maxSize);
				result.add(current);
			}
			current.add(element);
		}
		return result;
	}

	public static <E> List<E> nullToEmpty(final List<E> list) {
		return list == null ? Collections.<E> emptyList() : list;
	}

	public static <E> Set<E> nullToEmpty(final Set<E> set) {
		return set == null ? Collections.<E> emptySet() : set;
	}

	public static <E> Collection<E> nullToEmpty(final Collection<E> collection) {
		return collection == null ? Collections.<E> emptyList() : collection;
	}

	public static <E> Iterator<E> nullToEmpty(final Iterator<E> iterator) {
		return iterator == null ? Collections.<E> emptyList().iterator() : iterator;
	}
}
