package co.codewizards.cloudstore.core.ignore;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class IgnoreRuleManagerImpl implements IgnoreRuleManager {
	private static final Logger logger = LoggerFactory.getLogger(IgnoreRuleManagerImpl.class);

	private final File directory;
	private Config config;
	private List<IgnoreRule> ignoreRules;
	private Long configVersion;

	private static final Object classMutex = IgnoreRuleManagerImpl.class;
	private final Object instanceMutex = this;

	private static final long fileRefsCleanPeriod = 60000L;
	private static long fileRefsCleanLastTimestamp;

	private static final LinkedHashSet<File> fileHardRefs = new LinkedHashSet<>();
	private static final int fileHardRefsMaxSize = 10;

	/**
	 * {@link SoftReference}s to the files used in {@link #file2IgnoreRuleManager}.
	 * <p>
	 * There is no {@code SoftHashMap}, hence we use a WeakHashMap combined with the {@code SoftReference}s here.
	 * @see #file2IgnoreRuleManager
	 */
	private static final LinkedList<SoftReference<File>> fileSoftRefs = new LinkedList<>();
	/**
	 * @see #fileSoftRefs
	 */
	private static final Map<File, IgnoreRuleManagerImpl> file2IgnoreRuleManager = new WeakHashMap<>();

	protected IgnoreRuleManagerImpl(File directory) {
		this.directory = assertNotNull("directory", directory);
		config = ConfigImpl.getInstanceForDirectory(this.directory);
	}

	private static void cleanFileRefs() {
		synchronized (classMutex) {
			if (System.currentTimeMillis() - fileRefsCleanLastTimestamp < fileRefsCleanPeriod)
				return;

			for (final Iterator<SoftReference<File>> it = fileSoftRefs.iterator(); it.hasNext(); ) {
				final SoftReference<File> fileRef = it.next();
				if (fileRef.get() == null)
					it.remove();
			}
			fileRefsCleanLastTimestamp = System.currentTimeMillis();
		}
	}

	public static IgnoreRuleManager getInstanceForDirectory(final File directory) {
		AssertUtil.assertNotNull("directory", directory);
		cleanFileRefs();

		File irm_dir = null;
		IgnoreRuleManagerImpl irm;
		synchronized (classMutex) {
			irm = file2IgnoreRuleManager.get(directory);
			if (irm != null) {
				irm_dir = irm.directory;
				if (irm_dir == null) // very unlikely, but it actually *can* happen.
					irm = null; // we try to make it extremely probable that the Config we return does have a valid file reference.
			}

			if (irm == null) {
				final File localRoot = LocalRepoHelper.getLocalRootContainingFile(directory);
				if (localRoot == null)
					throw new IllegalArgumentException("directory is not inside a repository: " + directory.getAbsolutePath());

				irm = new IgnoreRuleManagerImpl(directory);
				file2IgnoreRuleManager.put(directory, irm);
				fileSoftRefs.add(new SoftReference<File>(directory));
				irm_dir = irm.directory;
			}
			assertNotNull("irm_dir", irm_dir);
		}
		refreshFileHardRefAndCleanOldHardRefs(irm_dir);
		return irm;
	}


	public List<IgnoreRule> getIgnoreRules() {
		refreshFileHardRefAndCleanOldHardRefs();
		synchronized (instanceMutex) {
			final Long newConfigVersion = config.getVersion();
			if (! equal(configVersion, newConfigVersion))
				ignoreRules = null;

			if (ignoreRules == null) {
				final List<IgnoreRule> result = new ArrayList<>();
				int emptyCounter = 0;
				for (int index = 0; ; ++index) {
					final IgnoreRule ignoreRule = loadIgnoreRule(index);
					if (ignoreRule == null) {
						if (++emptyCounter > 3)
							break; // We're a bit tolerant and don't immediately break (but only after 3 empty indices).

						continue;
					}
					emptyCounter = 0;
					result.add(ignoreRule);
				}
				configVersion = newConfigVersion;
				ignoreRules = Collections.unmodifiableList(result);
			}
			return ignoreRules;
		}
	}

	@Override
	public boolean isIgnored(final File file) {
		final String fileName = assertNotNull("file", file).getName();

		if (! directory.equals(file.getParentFile()))
			throw new IllegalArgumentException(String.format("file '%s' is not located within parent-directory '%s'!",
					file.getAbsolutePath(), directory.getAbsolutePath()));

		for (final IgnoreRule ignoreRule : getIgnoreRules()) {
			if (! ignoreRule.isEnabled())
				continue;

			boolean matches = ignoreRule.getNameRegexPattern().matcher(fileName).matches();
			if (matches)
				return true;
		}
		return false;
	}

	private IgnoreRule loadIgnoreRule(int index) {
		String namePattern = config.getProperty(getConfigKeyNamePattern(index), null);
		final String nameRegex = config.getProperty(getConfigKeyNameRegex(index), null);

		if (namePattern == null && nameRegex == null)
			return null;

		if (namePattern != null && nameRegex != null) {
			logger.warn("loadIgnoreRule: index={}: namePattern='{}' and nameRegex='{}' are both specified! Ignoring namePattern!",
					index, namePattern, nameRegex);
			namePattern = null;
		}

		IgnoreRule ignoreRule = createObject(IgnoreRuleImpl.class);
		ignoreRule.setIndex(index);
		ignoreRule.setNamePattern(namePattern);
		ignoreRule.setNameRegex(nameRegex);
		ignoreRule.setEnabled(config.getPropertyAsBoolean(getConfigKeyEnabled(index), true));
		ignoreRule.setCaseSensitive(config.getPropertyAsBoolean(getConfigKeyCaseSensitive(index), false));
		return ignoreRule;
	}

	private String getConfigKeyNamePattern(int index) {
		return getConfigKeyIgnorePrefix(index) + "namePattern";
	}

	private String getConfigKeyNameRegex(int index) {
		return getConfigKeyIgnorePrefix(index) + "nameRegex";
	}

	private String getConfigKeyEnabled(int index) {
		return getConfigKeyIgnorePrefix(index) + "enabled";
	}

	private String getConfigKeyCaseSensitive(int index) {
		return getConfigKeyIgnorePrefix(index) + "caseSensitive";
	}

	private String getConfigKeyIgnorePrefix(int index) {
		return String.format("ignore[%d].", index);
	}

	private static final void refreshFileHardRefAndCleanOldHardRefs(final IgnoreRuleManagerImpl ignoreRuleManager) {
		final File dir = AssertUtil.assertNotNull("ignoreRuleManager", ignoreRuleManager).directory;
		if (dir != null)
			refreshFileHardRefAndCleanOldHardRefs(dir);
	}

	private final void refreshFileHardRefAndCleanOldHardRefs() {
		refreshFileHardRefAndCleanOldHardRefs(this);
	}

	private static final void refreshFileHardRefAndCleanOldHardRefs(final File dir) {
		AssertUtil.assertNotNull("dir", dir);
		synchronized (fileHardRefs) {
			// make sure the current dir is at the end of fileHardRefs
			fileHardRefs.remove(dir);
			fileHardRefs.add(dir);

			// remove the first entry until size does not exceed limit anymore.
			while (fileHardRefs.size() > fileHardRefsMaxSize)
				fileHardRefs.remove(fileHardRefs.iterator().next());
		}
	}
}
