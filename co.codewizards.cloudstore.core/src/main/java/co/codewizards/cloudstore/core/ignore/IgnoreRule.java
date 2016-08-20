package co.codewizards.cloudstore.core.ignore;

import java.util.regex.Pattern;

/**
 * An {@code IgnoreRule} specifies when to ignore a file.
 * <p>
 * If a file matches an {@code IgnoreRule}, this file is not touched by CloudStore at all. It is neither
 * synchronised to a remote location nor is it deleted or otherwise treated.
 * <p>
 * There are 2 ways to declare an ignore-rule:
 * <p>
 * One is using a simple (shell-like) pattern on the name like
 * "*.bak", for example. There are only the wild-cards '*' and '?' supported having the usual meaning:
 * <ul>
 * <li>'*' matches 0 or more arbitrary characters.</li>
 * <li>'?' matches exactly one arbitrary character.</li>
 * </ul>
 * The simple pattern is specified as {@link #getNamePattern() namePattern}.
 * <p>
 * The other one is using a regular expression. If the property {@link #getNameRegex() nameRegex} is specified,
 * the {@code namePattern} is ignored.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface IgnoreRule {

	String getIgnoreRuleId();

	void setIgnoreRuleId(String ignoreRuleId);

	/**
	 * Gets a shell-style name-pattern or <code>null</code>.
	 * <p>
	 * An ignore-rule may be specified using shell-patterns like "*.jpg" or "*.b?k".
	 * They are implicitly converted to regular expressions.
	 * <p>
	 * This <b>name</b>-pattern is checked against the file's name without path as returned by
	 * {@link java.io.File#getName() File.getName()} (e.g. "image_938732.jpg").
	 * <p>
	 * If both, a {@linkplain #getNameRegex() regular expression} and a shell-pattern are
	 * specified, the regular expression is used and this pattern ignored.
	 * @return a shell-style name-pattern or <code>null</code>.
	 * @see #getNameRegex()
	 */
	String getNamePattern();

	void setNamePattern(String namePattern);

	/**
	 * Gets a regular expression or <code>null</code>.
	 * <p>
	 * This regular expression must match the entire file name (without path) - not only be contained in it.
	 * For example the regular expression "tree\.jpg" matches only the file "tree.jpg" and not the file
	 * "large_tree.jpg".
	 * <p>
	 * This <b>name</b>-regex is checked against the file's name without path as returned by
	 * {@link java.io.File#getName() File.getName()} (e.g. "image_938732.jpg").
	 * @return a regular expression or <code>null</code>.
	 * @see #getNamePattern()
	 */
	String getNameRegex();

	void setNameRegex(String nameRegex);

	boolean isEnabled();

	void setEnabled(boolean enabled);

	boolean isCaseSensitive();

	void setCaseSensitive(boolean caseSensitive);

	/**
	 * Gets a regular-expression-{@link Pattern} compiled from {@link #getNameRegex() nameRegex}
	 * or {@link #getNamePattern() namePattern}. If {@code nameRegex} is specified, it overrules
	 * {@code namePattern}, i.e. {@code namePattern} is used only, if {@code nameRegex == null}.
	 * @return a regular-expression-{@link Pattern} compiled from {@link #getNameRegex() nameRegex}
	 * or {@link #getNamePattern() namePattern}. <code>null</code>, if both {@code nameRegex}
	 * and {@code namePattern} are <code>null</code>.
	 */
	Pattern getNameRegexPattern();
}
