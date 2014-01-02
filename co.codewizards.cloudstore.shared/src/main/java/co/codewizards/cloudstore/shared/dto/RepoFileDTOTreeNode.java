package co.codewizards.cloudstore.shared.dto;

import static co.codewizards.cloudstore.shared.util.Util.*;

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
	 * The given {@code repoFileDTOs} must contain exactly one root-node (with
	 * {@link RepoFileDTO#getParentEntityID() RepoFileDTO.parentEntityID} being <code>null</code>) and must resolve
	 * completely, i.e. there must be a {@code RepoFileDTO} for every every referenced {@code parentEntityID}.
	 * @param repoFileDTOs the DTOs to be organized in a tree structure.
	 * @return the tree's root node. Never <code>null</code>.
	 */
	public static RepoFileDTOTreeNode createTree(Collection<RepoFileDTO> repoFileDTOs) {
		assertNotNull("repoFileDTOs", repoFileDTOs);

		Map<EntityID, RepoFileDTOTreeNode> entityID2RepoFileDTOTreeNode = new HashMap<EntityID, RepoFileDTOTreeNode>();
		for (RepoFileDTO repoFileDTO : repoFileDTOs) {
			entityID2RepoFileDTOTreeNode.put(repoFileDTO.getEntityID(), new RepoFileDTOTreeNode(repoFileDTO));
		}

		RepoFileDTOTreeNode rootNode = null;
		for (RepoFileDTOTreeNode node : entityID2RepoFileDTOTreeNode.values()) {
			EntityID parentEntityID = node.getRepoFileDTO().getParentEntityID();
			if (parentEntityID == null) {
				if (rootNode != null)
					throw new IllegalArgumentException("Multiple root nodes!");

				rootNode = node;
			}
			else {
				RepoFileDTOTreeNode parentNode = entityID2RepoFileDTOTreeNode.get(parentEntityID);
				if (parentNode == null)
					throw new IllegalStateException("parentEntityID unknown: " + parentEntityID);

				parentNode.addChild(node);
			}
		}

		if (rootNode == null)
			throw new IllegalStateException("There is no root node!");

		return rootNode;
	}

	private RepoFileDTOTreeNode parent;
	private final RepoFileDTO repoFileDTO;
	private final List<RepoFileDTOTreeNode> children = new ArrayList<RepoFileDTOTreeNode>();

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
		List<RepoFileDTOTreeNode> list = new ArrayList<RepoFileDTOTreeNode>();
		flattenTree(list, this);
		return list.iterator();
	}

	private void flattenTree(List<RepoFileDTOTreeNode> result, RepoFileDTOTreeNode node) {
		result.add(node);
		for (RepoFileDTOTreeNode child : node.getChildren()) {
			flattenTree(result, child);
		}
	}
}
