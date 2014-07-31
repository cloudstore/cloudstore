package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
	public static RepoFileDtoTreeNode createTree(Collection<RepoFileDto> repoFileDtos) throws IllegalArgumentException {
		assertNotNull("repoFileDtos", repoFileDtos);
		if (repoFileDtos.isEmpty())
			return null;

		Map<Long, RepoFileDtoTreeNode> id2RepoFileDtoTreeNode = new HashMap<Long, RepoFileDtoTreeNode>();
		for (RepoFileDto repoFileDto : repoFileDtos) {
			id2RepoFileDtoTreeNode.put(repoFileDto.getId(), new RepoFileDtoTreeNode(repoFileDto));
		}

		RepoFileDtoTreeNode rootNode = null;
		for (RepoFileDtoTreeNode node : id2RepoFileDtoTreeNode.values()) {
			Long parentId = node.getRepoFileDto().getParentId();
			if (parentId == null) {
				if (rootNode != null)
					throw new IllegalArgumentException("Multiple root nodes!");

				rootNode = node;
			}
			else {
				RepoFileDtoTreeNode parentNode = id2RepoFileDtoTreeNode.get(parentId);
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

	protected RepoFileDtoTreeNode(RepoFileDto repoFileDto) {
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
	}

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}

	public RepoFileDtoTreeNode getParent() {
		return parent;
	}
	protected void setParent(RepoFileDtoTreeNode parent) {
		this.parent = parent;
	}

	public List<RepoFileDtoTreeNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

	protected void addChild(RepoFileDtoTreeNode child) {
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
		List<RepoFileDtoTreeNode> leafs = new ArrayList<RepoFileDtoTreeNode>();
		populateLeafs(this, leafs);
		return leafs;
	}

	private void populateLeafs(RepoFileDtoTreeNode node, List<RepoFileDtoTreeNode> leafs) {
		if (node.getChildren().isEmpty()) {
			leafs.add(node);
		}
		for (RepoFileDtoTreeNode child : node.getChildren()) {
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
			List<RepoFileDtoTreeNode> list = new ArrayList<RepoFileDtoTreeNode>();
			flattenTree(list, this);
			flattenedTreeList = list;
		}
		return flattenedTreeList;
	}

	private void flattenTree(List<RepoFileDtoTreeNode> result, RepoFileDtoTreeNode node) {
		result.add(node);
		for (RepoFileDtoTreeNode child : node.getChildren()) {
			flattenTree(result, child);
		}
	}
}
