package co.codewizards.cloudstore.core.dto;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class RepoFileDtoTreeNode implements Iterable<RepoFileDtoTreeNode> {

	/**
	 * Create a single tree from the given {@code repoFileDtos}.
	 * <p>
	 * The given {@code repoFileDtos} must meet the following criteria:
	 * <ul>
	 * <li>It must not be <code>null</code>.
	 * <li>It may be empty.
	 * <li>If it is <i>not</i> empty, it may contain any number of elements, but:
	 * <ul>
	 * <li>It must contain exactly one root-node (with
	 * {@link RepoFileDto#getParentEntityID() RepoFileDto.parentEntityID} being <code>null</code>).
	 * <li>It must resolve completely, i.e. there must be a {@code RepoFileDto} for every
	 * referenced {@code parentEntityID}.
	 * </ul>
	 * </ul>
	 * @param repoFileDtos the Dtos to be organized in a tree structure. Must not be <code>null</code>. If
	 * empty, the method result will be <code>null</code>.
	 * @return the tree's root node. <code>null</code>, if {@code repoFileDtos} is empty.
	 * Never <code>null</code>, if {@code repoFileDtos} contains at least one element.
	 * @throws IllegalArgumentException if the given {@code repoFileDtos} does not meet the criteria stated above.
	 */
	public static RepoFileDtoTreeNode createTree(final Collection<RepoFileDto> repoFileDtos) throws IllegalArgumentException {
		requireNonNull(repoFileDtos, "repoFileDtos");
		if (repoFileDtos.isEmpty())
			return null;

		final Map<Long, RepoFileDtoTreeNode> id2RepoFileDtoTreeNode = new HashMap<Long, RepoFileDtoTreeNode>();
		for (final RepoFileDto repoFileDto : repoFileDtos) {
			id2RepoFileDtoTreeNode.put(repoFileDto.getId(), new RepoFileDtoTreeNode(repoFileDto));
		}

		RepoFileDtoTreeNode rootNode = null;
		for (final RepoFileDtoTreeNode node : id2RepoFileDtoTreeNode.values()) {
			final Long parentId = node.getRepoFileDto().getParentId();
			if (parentId == null) {
				if (rootNode != null)
					throw new IllegalArgumentException("Multiple root nodes!");

				rootNode = node;
			}
			else {
				final RepoFileDtoTreeNode parentNode = id2RepoFileDtoTreeNode.get(parentId);
				if (parentNode == null)
					throw new IllegalArgumentException("parentEntityID unknown: " + parentId);

				parentNode.addChild(node);
			}
		}

		if (rootNode == null)
			throw new IllegalArgumentException("There is no root node!");

		return rootNode;
	}

	private static final Comparator<RepoFileDtoTreeNode> nodeComparatorByNameOnly = new Comparator<RepoFileDtoTreeNode>() {
		@Override
		public int compare(RepoFileDtoTreeNode node0, RepoFileDtoTreeNode node1) {
			final String name0 = node0.getRepoFileDto().getName();
			final String name1 = node1.getRepoFileDto().getName();
			return name0.compareTo(name1);
		}
	};

	private RepoFileDtoTreeNode parent;
	private final RepoFileDto repoFileDto;
	private final SortedSet<RepoFileDtoTreeNode> children = new TreeSet<RepoFileDtoTreeNode>(nodeComparatorByNameOnly);
	private List<RepoFileDtoTreeNode> flattenedTreeList;

	protected RepoFileDtoTreeNode(final RepoFileDto repoFileDto) {
		this.repoFileDto = requireNonNull(repoFileDto, "repoFileDto");
	}

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}

	public RepoFileDtoTreeNode getParent() {
		return parent;
	}
	protected void setParent(final RepoFileDtoTreeNode parent) {
		this.parent = parent;
	}

	public Set<RepoFileDtoTreeNode> getChildren() {
		return Collections.unmodifiableSet(children);
	}

	protected void addChild(final RepoFileDtoTreeNode child) {
		child.setParent(this);
		children.add(child);
	}

	/**
	 * Gets the path from the root to the current node.
	 * <p>
	 * The path's elements are separated by a slash ("/").
	 * @return the path from the root to the current node. Never <code>null</code>.
	 */
	public String getPath() {
		final RepoFileDtoTreeNode parent = getParent();
		if (parent == null)
			return getRepoFileDto().getName();
		else
			return parent.getPath() + '/' + getRepoFileDto().getName();
	}

	public List<RepoFileDtoTreeNode> getLeafs() {
		final List<RepoFileDtoTreeNode> leafs = new ArrayList<RepoFileDtoTreeNode>();
		populateLeafs(this, leafs);
		return leafs;
	}

	private void populateLeafs(final RepoFileDtoTreeNode node, final List<RepoFileDtoTreeNode> leafs) {
		if (node.getChildren().isEmpty()) {
			leafs.add(node);
		}
		for (final RepoFileDtoTreeNode child : node.getChildren()) {
			populateLeafs(child, leafs);
		}
	}

	@Override
	public Iterator<RepoFileDtoTreeNode> iterator() {
		return new MyIterator(getFlattenedTreeList().iterator());
	}

	private class MyIterator implements Iterator<RepoFileDtoTreeNode> {
		private final Iterator<RepoFileDtoTreeNode> delegate;
		private RepoFileDtoTreeNode current;

		public MyIterator(final Iterator<RepoFileDtoTreeNode> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public RepoFileDtoTreeNode next() {
			return current = delegate.next();
		}

		@Override
		public void remove() {
			if (current != null) {
				RepoFileDtoTreeNode p = current.getParent();
				if (p != null && p.children != null) {
					p.children.remove(current);
				}
			}
			delegate.remove();
		}
	}

	public int size() {
		return getFlattenedTreeList().size();
	}

	private List<RepoFileDtoTreeNode> getFlattenedTreeList() {
		if (flattenedTreeList == null) {
			final List<RepoFileDtoTreeNode> list = new LinkedList<RepoFileDtoTreeNode>();
			flattenTree(list, this);
			flattenedTreeList = list;
		}
		return flattenedTreeList;
	}

	private void flattenTree(final List<RepoFileDtoTreeNode> result, final RepoFileDtoTreeNode node) {
		result.add(node);
		for (final RepoFileDtoTreeNode child : node.getChildren()) {
			flattenTree(result, child);
		}
	}

	public RepoFileDtoTreeNode getRoot() {
		final RepoFileDtoTreeNode parent = getParent();
		if (parent == null)
			return this;
		else
			return parent.getRoot();
	}
}
