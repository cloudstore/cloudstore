package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.chronos.ChronosUtil.*;
import static co.codewizards.cloudstore.core.util.DebugUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.HashUtil;

public class RepoFileDtoTreeNodeIteratorRemoveMemoryReleaseTest {

	private static final Logger logger = LoggerFactory.getLogger(RepoFileDtoTreeNodeIteratorRemoveMemoryReleaseTest.class);
	private int nextRepoFileId = 0;
	private final int maxLevel = 5;
	private final Random random = new Random();

	@Test
	public void iteratorRemoveMemoryReleaseTest() {
		System.gc();
		logger.info("Memory before createLargeTree:");
		logMemoryStats(logger);

		RepoFileDtoTreeNode tree = createLargeTree();

		System.gc();
		logger.info("Memory after createLargeTree with {} nodes:", tree.size());
		logMemoryStats(logger);

		final long usedMemoryAfterCreateLargeTree = getUsedMemory();
		int halfSize = tree.size() / 2;

		int i = 0;
		for (Iterator<RepoFileDtoTreeNode> it = tree.iterator(); it.hasNext();) {
			RepoFileDtoTreeNode node = it.next();
			it.remove();

			if (++i % 1000 == 0) {
				System.gc();
				logger.info("Memory after removing {} nodes:", i);
				logMemoryStats(logger);
			}

			if (i >= halfSize) {
				halfSize = Integer.MAX_VALUE; // prevent from showing this again.
				long usedMemoryAfterIteratingHalf = getUsedMemory();
				assertThat(usedMemoryAfterIteratingHalf).isLessThan(usedMemoryAfterCreateLargeTree * 70 / 100);
				logger.info("*** SUCCESS *** Memory consumption is down by more than 30%.");
			}
		}

	}

	protected RepoFileDtoTreeNode createLargeTree() {
		List<RepoFileDto> repoFileDtos = new LinkedList<>();
		DirectoryDto root = new DirectoryDto();
		root.setId(nextRepoFileId());
		root.setName(""); // root is level 0
		root.setLastModified(nowAsDate());
		repoFileDtos.add(root);
		createDummyRepoFileDtos(repoFileDtos, root, 1); // children are level 1
		return RepoFileDtoTreeNode.createTree(repoFileDtos);
	}

	private long nextRepoFileId() {
		return nextRepoFileId++;
	}

	protected void createDummyRepoFileDtos(List<RepoFileDto> repoFileDtos, DirectoryDto parent, int level) {
		byte[] randomBytes = new byte[16];

		// create files
		for (int i = 0; i < 95; ++i) {
			random.nextBytes(randomBytes);
			String s = HashUtil.encodeHexStr(randomBytes);

			NormalFileDto child = new NormalFileDto();
			child.setId(nextRepoFileId());
			child.setParentId(parent.getId());
			child.setName(s + s + s + s);
			child.setLastModified(nowAsDate());

			child.setSha1(HashUtil.sha1(randomBytes));
			repoFileDtos.add(child);
		}

		// create sub-directories
		for (int i = 0; i < 5; ++i) {
			random.nextBytes(randomBytes);
			String s = HashUtil.encodeHexStr(randomBytes);

			DirectoryDto child = new DirectoryDto();
			child.setId(nextRepoFileId());
			child.setParentId(parent.getId());
			child.setName(s + s);
			child.setLastModified(nowAsDate());

			if (level < maxLevel) {
				createDummyRepoFileDtos(repoFileDtos, child, level + 1);
			}
			repoFileDtos.add(child);
		}

		if (level == maxLevel) {
			System.gc();
			logger.info("Memory after creating {} RepoFileDtos:", repoFileDtos.size());
			logMemoryStats(logger);
		}
	}

	protected static long getUsedMemory() {
		final Runtime runtime = Runtime.getRuntime();

		runtime.gc();

		// allocated: memory currently allocated by the VM (requested from and granted by the OS). Might be less than 'max'.
		final long allocated = runtime.totalMemory();

		// used: memory in use by Java objects (hence we invoke gc() above, otherwise this doesn't say anything useful).
		final long used = allocated - runtime.freeMemory();

		return used;
	}
}
