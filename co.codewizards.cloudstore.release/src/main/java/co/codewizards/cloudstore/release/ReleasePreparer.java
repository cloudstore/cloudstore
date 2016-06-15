package co.codewizards.cloudstore.release;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import co.codewizards.cloudstore.core.util.IOUtil;

public class ReleasePreparer {

	// Set 'newMavenVersion' to the new desired  version. Then run the main method. It will update
	// all files accordingly.
	//
	// IMPORTANT: In org.cumulus4j.store/pom.xml the repository for deployment needs to be manually
	// switched between release and snapshot versions!
	//
	// Don't forget:
	//   * to register the new version in "whats-new.apt" and "releases/index.md.vm",
	//   * to create a tag in SVN after check-in!
	//   * to update the symlinks on the web-server after the release was successful.

	protected String newMavenVersion = "0.9.7";
//	protected String newMavenVersion = "0.9.8-SNAPSHOT";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// DO NOT CHANGE ANYTHING BELOW THIS POINT, if you don't really want to improve this program.
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static final Logger logger = LoggerFactory.getLogger(ReleasePreparer.class);

	protected Pattern[] excludePathPatterns = {
		Pattern.compile(".*\\/target\\/.*")
	};

	protected String artifactIdPrefix = "co.codewizards.";

	protected String rootDir = "..";
	protected File rootDirFile;

	protected Properties properties;

	protected List<File> pomFiles;

	protected ReleasePreparer() { }

	public static void main(final String[] args) throws Exception {
		new ReleasePreparer().run();
	}

	public void run() throws Exception {
		logger.info("run: Entered.");
		rootDirFile = new File(this.rootDir);

		initProperties();

		logger.info("run: Collecting files.");
		collectFiles(rootDirFile);
		logger.info("run: Files collected.");

		logger.info("run: Updating files.");
		updatePomFiles();
		logger.info("run: Completed.");
	}

	protected void initProperties() {
		properties = new Properties();
		properties.setProperty("project.version", newMavenVersion);
	}

	protected void updatePomFiles() throws Exception {
		for (final File  f : pomFiles) {
			new PomUpdater(f).setArtifactIdPrefix(artifactIdPrefix).setNewMavenVersion(newMavenVersion).update();
		}
	}

	protected void collectFiles(final File dir) throws IOException {
		pomFiles = new ArrayList<File>();
		_collectFiles(dir.getCanonicalFile());
	}
	protected void _collectFiles(final File dirOrFile) {
		if (dirOrFile.getName().startsWith("."))
			return;

		for (final Pattern excludePathPattern : excludePathPatterns) {
			if (excludePathPattern.matcher(dirOrFile.getAbsolutePath()).matches()) {
				logger.debug("_collectFiles: excludePathPattern '{}' matches '{}'. Skipping.", excludePathPattern, dirOrFile);
				return;
			}
		}

		if ("pom.xml".equals(dirOrFile.getName())) {
			pomFiles.add(dirOrFile);
			return;
		}

		final File[] listFiles = dirOrFile.listFiles();
		if (listFiles != null) {
			for (final File child : listFiles)
				_collectFiles(child);
		}
	}

	protected static void setTagValue(final Document document, final Node node, final String value) {
		setTagValue(document, node, value, null);
	}

	protected static void setTagValue(final Document document, final Node node, final String value, final Map<?, ?> valueVariables) {
		while (node.getFirstChild() != null)
			node.removeChild(node.getFirstChild());

		final String v = (valueVariables == null || valueVariables.isEmpty()) ?
				value : IOUtil.replaceTemplateVariables(value, valueVariables);

		final Text textNode = document.createTextNode(v);
		node.appendChild(textNode);
	}

	protected static Map<String, String> resolveProperties(final Map<?, ?> properties, final int depth) {
		if (depth < 1)
			throw new IllegalArgumentException("depth < 1");

		Map<String, String> result = null;
		for (int i = 0; i < depth; ++i) {
			result = resolveProperties(result == null ? properties : result);
		}
		return result;
	}

	protected static Map<String, String> resolveProperties(final Map<?, ?> properties) {
		final Map<String, String> result = new HashMap<String, String>(properties.size());
		for (final Map.Entry<?, ?> me : properties.entrySet()) {
			final String key = me.getKey().toString();
			final String value = IOUtil.replaceTemplateVariables(me.getValue().toString(), properties);
			result.put(key, value);
		}
		return result;
	}
}
