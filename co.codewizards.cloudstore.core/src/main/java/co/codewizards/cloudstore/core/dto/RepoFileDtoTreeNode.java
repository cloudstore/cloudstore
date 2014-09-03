package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import co.codewizards.cloudstore.core.util.AssertUtil;

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
		AssertUtil.assertNotNull("repoFileDtos", repoFileDtos);
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

	private RepoFileDtoTreeNode parent;
	private final RepoFileDto repoFileDto;
	private final List<RepoFileDtoTreeNode> children = new ArrayList<RepoFileDtoTreeNode>();
	private List<RepoFileDtoTreeNode> flattenedTreeList;

	protected RepoFileDtoTreeNode(final RepoFileDto repoFileDto) {
		this.repoFileDto = AssertUtil.assertNotNull("repoFileDto", repoFileDto);
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

	public List<RepoFileDtoTreeNode> getChildren() {
		return Collections.unmodifiableList(children);
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
		if (getParent() == null)
			return getRepoFileDto().getName();
		else
			return getParent().getPath() + '/' + getRepoFileDto().getName();
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
		return getFlattenedTreeList().iterator();
	}

	public int size() {
		return getFlattenedTreeList().size();
	}

	private List<RepoFileDtoTreeNode> getFlattenedTreeList() {
		if (flattenedTreeList == null) {
			final List<RepoFileDtoTreeNode> list = new ArrayList<RepoFileDtoTreeNode>();
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
}
