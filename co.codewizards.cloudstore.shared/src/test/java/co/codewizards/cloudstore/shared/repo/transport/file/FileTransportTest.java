package co.codewizards.cloudstore.shared.repo.transport.file;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.AbstractTest;
import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;
import co.codewizards.cloudstore.shared.dto.EntityID;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;
import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.shared.repo.local.RepositoryManager;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransport;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactoryRegistry;

public class FileTransportTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(FileTransportTest.class);

	private File remoteRoot;
	private ChangeSetResponse changeSetResponse1;

	@Test
	public void getChangeSetForEntireRepository() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot();
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(remoteRoot);
		assertThat(repositoryManager).isNotNull();

		File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");

		File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		repositoryManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest1 = new ChangeSetRequest();
		changeSetRequest1.setRevision(-1);

		changeSetResponse1 = repoTransport.getChangeSet(changeSetRequest1);
		assertThat(changeSetResponse1).isNotNull();
		assertThat(changeSetResponse1.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse1.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse1.getRepositoryDTO().getEntityID()).isNotNull();

		// changeSetResponse1 should contain the entire repository - including the root -, because really
		// every localRevision must be > -1.
		assertThat(changeSetResponse1.getRepoFileDTOs()).hasSize(14);

		Set<String> paths = getPaths(changeSetResponse1.getRepoFileDTOs());
		assertThat(paths).containsOnly("/1/a", "/1/b", "/1/c", "/2/a", "/2/1/a", "/3/a", "/3/b", "/3/c", "/3/d");

		repositoryManager.close();
	}

	@Test
	public void getChangeSetForAddedFile() throws Exception {
		getChangeSetForEntireRepository();

		RepositoryManager repositoryManager = repositoryManagerRegistry.getRepositoryManager(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		createFileWithRandomContent(child_2_1, "b");

		repositoryManager.localSync(new LoggerProgressMonitor(logger));

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest2 = new ChangeSetRequest();
		changeSetRequest2.setRevision(changeSetResponse1.getRepositoryDTO().getRevision());

		ChangeSetResponse changeSetResponse2 = repoTransport.getChangeSet(changeSetRequest2);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the added file and its direct parent, because they are both modified (new localRevision).
		// Additionally, we expect all parent-directories (recursively) until (including) the root, because they
		// are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDTOs()).hasSize(4);

		Set<String> paths = getPaths(changeSetResponse2.getRepoFileDTOs());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/b");

		repositoryManager.close();
	}

	private Set<String> getPaths(Collection<RepoFileDTO> repoFileDTOs) {
		RepoFileDTOTreeNode rootNode = buildTree(repoFileDTOs);
		assertThat(rootNode.current.getName()).isEqualTo("");
		List<RepoFileDTOTreeNode> leafs = new ArrayList<FileTransportTest.RepoFileDTOTreeNode>();
		populateLeafs(rootNode, leafs);
		Set<String> paths = new HashSet<String>(leafs.size());
		for (RepoFileDTOTreeNode leaf : leafs) {
			paths.add(leaf.getPath());
		}
		return paths;
	}

	private RepoFileDTOTreeNode buildTree(Collection<RepoFileDTO> repoFileDTOs) {
		Map<EntityID, RepoFileDTOTreeNode> entityID2RepoFileDTOTreeNode = new HashMap<EntityID, RepoFileDTOTreeNode>();
		for (RepoFileDTO repoFileDTO : repoFileDTOs) {
			entityID2RepoFileDTOTreeNode.put(repoFileDTO.getEntityID(), new RepoFileDTOTreeNode(repoFileDTO));
		}
		RepoFileDTOTreeNode rootNode = null;
		for (RepoFileDTOTreeNode node : entityID2RepoFileDTOTreeNode.values()) {
			EntityID parentEntityID = node.current.getParentEntityID();
			if (parentEntityID == null) {
				if (rootNode != null)
					Assert.fail("Multiple root nodes!");

				rootNode = node;
			}
			else {
				RepoFileDTOTreeNode parentNode = entityID2RepoFileDTOTreeNode.get(parentEntityID);
				assertThat(parentNode).isNotNull();
				node.parent = parentNode;
				parentNode.children.add(node);
			}
		}
		assertThat(rootNode).isNotNull();
		return rootNode;
	}

	private void populateLeafs(RepoFileDTOTreeNode node, List<RepoFileDTOTreeNode> leafs) {
		if (node.children.isEmpty()) {
			leafs.add(node);
		}
		for (RepoFileDTOTreeNode child : node.children) {
			populateLeafs(child, leafs);
		}
	}

	private static class RepoFileDTOTreeNode {
		public RepoFileDTOTreeNode parent;
		public final RepoFileDTO current;
		public List<RepoFileDTOTreeNode> children = new ArrayList<RepoFileDTOTreeNode>(0);
		public RepoFileDTOTreeNode(RepoFileDTO current) {
			this.current = current;
		}
		public String getPath() {
			if (parent == null)
				return current.getName();
			else
				return parent.getPath() + '/' + current.getName();
		}
	}


}
