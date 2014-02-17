package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RepoFileDTOTreeNode implements Iterable<RepoFileDTOTreeNode> {

	/**
	 * Create a single tree from the given {@code repoFileDTOs}.
	 * <p>
	 * The given {@code repoFileDTOs} must meet the following criteria:
	 * <ul>
	 * <li>It must not be <code>null</code>.
	 * <li>It may be empty.
	 * <li>If it is <i>not</i> empty, it may contain any number of elements, but:
	 * <ul>
	 * <li>It must contain exactly one root-node (with
	 * {@link RepoFileDTO#getParentEntityID() RepoFileDTO.parentEntityID} being <code>null</code>).
	 * <li>It must resolve completely, i.e. there must be a {@code RepoFileDTO} for every
	 * referenced {@code parentEntityID}.
	 * </ul>
	 * </ul>
	 * @param repoFileDTOs the DTOs to be organized in a tree structure. Must not be <code>null</code>. If
	 * empty, the method result will be <code>null</code>.
	 * @return the tree's root node. <code>null</code>, if {@code repoFileDTOs} is empty.
	 * Never <code>null</code>, if {@code repoFileDTOs} contains at least one element.
	 * @throws IllegalArgumentException if the given {@code repoFileDTOs} does not meet the criteria stated above.
	 */
	public static RepoFileDTOTreeNode createTree(Collection<RepoFileDTO> repoFileDTOs) throws IllegalArgumentException {
		assertNotNull("repoFileDTOs", repoFileDTOs);
		if (repoFileDTOs.isEmpty())
			return null;

		Map<Long, RepoFileDTOTreeNode> id2RepoFileDTOTreeNode = new HashMap<Long, RepoFileDTOTreeNode>();
		for (RepoFileDTO repoFileDTO : repoFileDTOs) {
			id2RepoFileDTOTreeNode.put(repoFileDTO.getId(), new RepoFileDTOTreeNode(repoFileDTO));
		}

		RepoFileDTOTreeNode rootNode = null;
		for (RepoFileDTOTreeNode node : id2RepoFileDTOTreeNode.values()) {
			Long parentId = node.getRepoFileDTO().getParentId();
			if (parentId == null) {
				if (rootNode != null)
					throw new IllegalArgumentException("Multiple root nodes!");

				rootNode = node;
			}
			else {
				RepoFileDTOTreeNode parentNode = id2RepoFileDTOTreeNode.get(parentId);
				if (parentNode == null)
					throw new IllegalArgumentException("parentEntityID unknown: " + parentId);

				parentNode.addChild(node);
			}
		}

		if (rootNode == null)
			throw new IllegalArgumentException("There is no root node!");

		return rootNode;
	}

	private RepoFileDTOTreeNode parent;
	private final RepoFileDTO repoFileDTO;
	private final List<RepoFileDTOTreeNode> children = new ArrayList<RepoFileDTOTreeNode>();
	private List<RepoFileDTOTreeNode> flattenedTreeList;

	protected RepoFileDTOTreeNode(RepoFileDTO repoFileDTO) {
		this.repoFileDTO = assertNotNull("repoFileDTO", repoFileDTO);
	}

	public RepoFileDTO getRepoFileDTO() {
		return repoFileDTO;
	}

	public RepoFileDTOTreeNode getParent() {
		return parent;
	}
	protected void setParent(RepoFileDTOTreeNode parent) {
		this.parent = parent;
	}

	public List<RepoFileDTOTreeNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

	protected void addChild(RepoFileDTOTreeNode child) {
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
			return getRepoFileDTO().getName();
		else
			return getParent().getPath() + '/' + getRepoFileDTO().getName();
	}

	public List<RepoFileDTOTreeNode> getLeafs() {
		List<RepoFileDTOTreeNode> leafs = new ArrayList<RepoFileDTOTreeNode>();
		populateLeafs(this, leafs);
		return leafs;
	}

	private void populateLeafs(RepoFileDTOTreeNode node, List<RepoFileDTOTreeNode> leafs) {
		if (node.getChildren().isEmpty()) {
			leafs.add(node);
		}
		for (RepoFileDTOTreeNode child : node.getChildren()) {
			populateLeafs(child, leafs);
		}
	}

	@Override
	public Iterator<RepoFileDTOTreeNode> iterator() {
		return getFlattenedTreeList().iterator();
	}

	public int size() {
		return getFlattenedTreeList().size();
	}

	private List<RepoFileDTOTreeNode> getFlattenedTreeList() {
		if (flattenedTreeList == null) {
			List<RepoFileDTOTreeNode> list = new ArrayList<RepoFileDTOTreeNode>();
			flattenTree(list, this);
			flattenedTreeList = list;
		}
		return flattenedTreeList;
	}

	private void flattenTree(List<RepoFileDTOTreeNode> result, RepoFileDTOTreeNode node) {
		result.add(node);
		for (RepoFileDTOTreeNode child : node.getChildren()) {
			flattenTree(result, child);
		}
	}
}
