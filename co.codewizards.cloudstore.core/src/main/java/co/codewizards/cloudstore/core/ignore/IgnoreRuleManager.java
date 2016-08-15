package co.codewizards.cloudstore.core.ignore;

import co.codewizards.cloudstore.core.oio.File;

public interface IgnoreRuleManager {

	/**
	 * Determines, whether the given {@code file} is ignored.
	 * @param file a file . Must not be <code>null</code>.
	 * @return <code>true</code>, if the file is ignored, i.e. the non-qualified {@code fileName}
	 * matches an ignore-rule. <code>false</code> otherwise.
	 */
	boolean isIgnored(File file);

}
