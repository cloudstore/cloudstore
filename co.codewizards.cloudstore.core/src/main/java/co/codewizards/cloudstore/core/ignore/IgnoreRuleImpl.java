package co.codewizards.cloudstore.core.ignore;

import static java.util.Objects.*;

import java.util.regex.Pattern;

public class IgnoreRuleImpl implements IgnoreRule {
	private String ignoreRuleId;
	private String namePattern;
	private String nameRegex;
	private boolean enabled;
	private boolean caseSensitive;
	private Pattern nameRegexPattern;

	public IgnoreRuleImpl() {
	}

	@Override
	public String getIgnoreRuleId() {
		return ignoreRuleId;
	}
	@Override
	public void setIgnoreRuleId(String ignoreRuleId) {
		this.ignoreRuleId = ignoreRuleId;
	}

	@Override
	public String getNamePattern() {
		return namePattern;
	}

	@Override
	public void setNamePattern(String namePattern) {
		this.namePattern = namePattern;
		this.nameRegexPattern = null;
	}

	@Override
	public String getNameRegex() {
		return nameRegex;
	}

	@Override
	public void setNameRegex(String nameRegex) {
		this.nameRegex = nameRegex;
		this.nameRegexPattern = null;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		this.nameRegexPattern = null;
	}

	@Override
	public Pattern getNameRegexPattern() {
		if (nameRegexPattern == null) {
			String regex = getNameRegex();
			if (regex == null) {
				final String pattern = getNamePattern();
				if (pattern == null)
					return null;

				regex = convertPatternToRegex(pattern);
			}
			int flags = isCaseSensitive() ? 0 : ( Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE );
			nameRegexPattern = Pattern.compile(regex, flags);
		}
		return nameRegexPattern;
	}

	private static String convertPatternToRegex(final String pattern) {
		requireNonNull(pattern, "pattern");
//		return pattern.replaceAll("\\.", "\\\\.").replaceAll("\\?", ".").replaceAll("\\*", ".*");
		// We better iterate *once* than calling replaceAll - which needs to do some iteration - again and again.
		final StringBuilder res = new StringBuilder();
		for (int i = 0; i < pattern.length(); ++i) {
			final char c = pattern.charAt(i);
			switch (c) {
				case '.':
					res.append("\\.");
					break;
				case '+':
					res.append("\\+");
					break;
				case '?':
					res.append('.');
					break;
				case '*':
					res.append(".*");
					break;
				case '\\':
					res.append("\\\\");
					break;
				default:
					res.append(c);
			}
		}
		return res.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this))
				+ "[namePattern=" + namePattern
				+ ", nameRegex=" + nameRegex
				+ ", caseSensitive=" + caseSensitive
				+ ", enabled=" + enabled
				+ ']';
	}
}
