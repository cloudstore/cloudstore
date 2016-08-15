package co.codewizards.cloudstore.core.ignore;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;

public class IgnoreRuleManagerImpl implements IgnoreRuleManager {
	private static final Logger logger = LoggerFactory.getLogger(IgnoreRuleManagerImpl.class);

	private final File directory;
	private Config config;
	private List<IgnoreRule> ignoreRules;
	private Long configVersion;

	protected IgnoreRuleManagerImpl(File directory) {
		this.directory = assertNotNull("directory", directory);
		config = ConfigImpl.getInstanceForDirectory(this.directory);
	}

	public static IgnoreRuleManager getInstanceForDirectory(File directory) {
		return createObject(IgnoreRuleManagerImpl.class, directory); // TODO we should add a cache! But one that's invalidated when the Config needs reloading.
	}

	public synchronized List<IgnoreRule> getIgnoreRules() {
		final long newConfigVersion = config.getVersion();
		if (configVersion == null || configVersion.equals(newConfigVersion))
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
}
