package co.codewizards.cloudstore.core.config;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.codewizards.cloudstore.core.appid.AppIdRegistry;

/**
 * Configuration of CloudStore supporting inheritance of settings.
 * <p>
 * Obtain an instance via one of the following methods:
 * <ul>
 * <li>{@link ConfigImpl#getInstance()}
 * <li>{@link ConfigImpl#getInstanceForDirectory(co.codewizards.cloudstore.core.oio.File)}
 * <li>{@link ConfigImpl#getInstanceForFile(co.codewizards.cloudstore.core.oio.File)}
 * </ul>
 * <p>
 * There is one {@code Config} instance available (lazily created, cached temporarily) for every
 * directory and every file in a repository. Each {@code Config} inherits the settings from the
 * parent-directory, if not explicitly overwritten.
 * <p>
 * The configuration is based on {@link Properties} files. Every property file is optional. If it
 * does not exist, all settings are inherited. If it does exist, only those properties contained in
 * the file are overriden. All properties not contained in the file are still inherited. Inheritance
 * is thus applicable on every individual property.
 * <p>
 * Modifications, deletions, creations of properties files are detected during runtime (pretty immediately).
 * Note, that this detection is based on the files' timestamps. Since most file systems have a granularity
 * of 1 second (some even 2) for the last-modified-timestamp, multiple modifications in the same second might
 * not be detected.
 * <p>
 * There is a global properties file in the user's home directory (or wherever {@link ConfigDir}
 * points to): <code>&#36;{user.home}/.cloudstore/cloudstore.properties</code>
 * <p>
 * Additionally, every directory can optionally contain the following files:
 * <ol>
 * <li><code>.cloudstore.properties</code>
 * <li><code>cloudstore.properties</code>
 * <li><code>.&#36;{anyFileName}.cloudstore.properties</code>
 * <li><code>&#36;{anyFileName}.cloudstore.properties</code>
 * </ol>
 * <p>
 * The files 1. and 2. are applicable to the entire directory and all sub-directories and files in it.
 * Usually, on GNU/Linux people will prefer 1., but when using Windows, files starting with a "." are
 * sometimes a bit hard to deal with. Therefore, we support both. The file 2. overrides the settings of file 1..
 * <p>
 * The files 3. and 4. are applicable only to the file <code>&#36;{anyFileName}</code>. Thus, if you want
 * to set special behaviour for the file <code>example.db</code> only, you can create the file
 * <code>.example.db.cloudstore.properties</code> in the same directory.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface Config {
	String APP_ID_SIMPLE_ID = AppIdRegistry.getInstance().getAppIdOrFail().getSimpleId();

	String PROPERTIES_FILE_NAME_FOR_DIRECTORY_LOCAL = '.' + APP_ID_SIMPLE_ID + ".local.properties";

	/**
	 * Prefix used for system properties overriding configuration entries.
	 * <p>
	 * Every property in the configuration (i.e. in its properties files) can be overridden
	 * by a corresponding system property. The system property must be prefixed.
	 * <p>
	 * For example, to override the configuration property with the key "deferrableExecutor.timeout",
	 * you can pass the system property "cloudstore.deferrableExecutor.timeout" to the JVM. If the
	 * system property exists, the configuration is not consulted, but the system property value is
	 * used as shortcut.
	 * <p>
	 * Additionally, it is possible to override configuration entries via OS environment variables.
	 * Since an env var's name must not contain a dot ("."), all dots are replaced by underscores ("_").
	 */
	String SYSTEM_PROPERTY_PREFIX = APP_ID_SIMPLE_ID + '.';

	/**
	 * Gets the property identified by the given key.
	 * <p>
	 * This method directly delegates to {@link Properties#getProperty(String, String)}.
	 * Thus, an empty String in the internal {@code Properties} is returned instead of the
	 * given {@code defaultValue}. The {@code defaultValue} is only returned, if neither
	 * the internal {@code Properties} of this {@code Config} nor any of its parents contains
	 * the entry.
	 * <p>
	 * <b>Important:</b> This is often not the desired behaviour. You might want to use
	 * {@link #getPropertyAsNonEmptyTrimmedString(String, String)} instead!
	 * <p>
	 * Every property can be overwritten by a system property prefixed with {@value #SYSTEM_PROPERTY_PREFIX}.
	 * If - for example - the key "updater.force" is to be read and a system property
	 * named "cloudstore.updater.force" is set, this system property is returned instead!
	 * @param key the key identifying the property. Must not be <code>null</code>.
	 * @param defaultValue the default value to fall back to, if neither this {@code Config}'s
	 * internal {@code Properties} nor any of its parents contains a matching entry.
	 * May be <code>null</code>.
	 * @return the property's value. Never <code>null</code> unless {@code defaultValue} is <code>null</code>.
	 * @see #getPropertyAsNonEmptyTrimmedString(String, String)
	 */
	String getProperty(final String key, final String defaultValue);

	/**
	 * Gets the property identified by the given key; <b>not</b> taking inheritance into account.
	 * <p>
	 * This method corresponds to {@link #getProperty(String, String)}, but it does not fall back
	 * to any inherited value.
	 * <p>
	 * <b>Important:</b> This method should never be used in order to control the behaviour of the
	 * application! It is intended for use of administrative tools / UIs which need to read/write
	 * directly.
	 *
	 * @param key the key identifying the property. Must not be <code>null</code>.
	 * @return the property's value. <code>null</code>, if the property is not set.
	 * @see #setDirectProperty(String, String)
	 */
	String getDirectProperty(final String key);

	/**
	 * Sets the property identified by the given key; <b>not</b> taking inheritance into account.
	 * @param key the key identifying the property. Must not be <code>null</code>.
	 * @param value the property's value. <code>null</code> removes the property from this concrete
	 * configuration instance.
	 * @see #getDirectProperty(String)
	 */
	void setDirectProperty(final String key, final String value);

	/**
	 * Gets the property identified by the given key; {@linkplain String#trim() trimmed}.
	 * <p>
	 * In contrast to {@link #getProperty(String, String)}, this method falls back to the given
	 * {@code defaultValue}, if the internal {@code Properties} contains an empty {@code String}
	 * (after trimming) as value for the given {@code key}.
	 * <p>
	 * It therefore means that a value set to an empty {@code String} in the properties file means
	 * to use the program's default instead. It is therefore consistent with
	 * {@link #getPropertyAsLong(String, long)} and all other {@code getPropertyAs...(...)}
	 * methods.
	 * <p>
	 * The same rules apply to the fall-back-strategy from system property to environment variable and
	 * finally config files.
	 * <p>
	 * Every property can be overwritten by a system property prefixed with {@value #SYSTEM_PROPERTY_PREFIX}.
	 * If - for example - the key "updater.force" is to be read and a system property
	 * named "cloudstore.updater.force" is set, this system property is returned instead!
	 * @param key the key identifying the property. Must not be <code>null</code>.
	 * @param defaultValue the default value to fall back to, if neither this {@code Config}'s
	 * internal {@code Properties} nor any of its parents contains a matching entry or
	 * if this entry's value is an empty {@code String}.
	 * May be <code>null</code>.
	 * @return the property's value. Never <code>null</code> unless {@code defaultValue} is <code>null</code>.
	 */
	String getPropertyAsNonEmptyTrimmedString(final String key, final String defaultValue);

	long getPropertyAsLong(final String key, final long defaultValue);

	long getPropertyAsPositiveOrZeroLong(final String key, final long defaultValue);

	int getPropertyAsInt(final String key, final int defaultValue);

	int getPropertyAsPositiveOrZeroInt(final String key, final int defaultValue);

	/**
	 * Gets the property identified by the given key.
	 * @param key the key identifying the property. Must not be <code>null</code>.
	 * @param defaultValue the default value to fall back to, if neither this {@code Config}'s
	 * internal {@code Properties} nor any of its parents contains a matching entry or
	 * if this entry's value does not match any possible enum value. Must not be <code>null</code>.
	 * If a <code>null</code> default value is required, use {@link #getPropertyAsEnum(String, Class, Enum)}
	 * instead!
	 * @return the property's value. Never <code>null</code>.
	 * @see #getPropertyAsEnum(String, Class, Enum)
	 * @see #getPropertyAsNonEmptyTrimmedString(String, String)
	 */
	<E extends Enum<E>> E getPropertyAsEnum(final String key, final E defaultValue);

	/**
	 * Gets the property identified by the given key.
	 * @param key the key identifying the property. Must not be <code>null</code>.
	 * @param enumClass the enum's type. Must not be <code>null</code>.
	 * @param defaultValue the default value to fall back to, if neither this {@code Config}'s
	 * internal {@code Properties} nor any of its parents contains a matching entry or
	 * if this entry's value does not match any possible enum value. May be <code>null</code>.
	 * @return the property's value. Never <code>null</code> unless {@code defaultValue} is <code>null</code>.
	 * @see #getPropertyAsEnum(String, Enum)
	 * @see #getPropertyAsNonEmptyTrimmedString(String, String)
	 */
	<E extends Enum<E>> E getPropertyAsEnum(final String key, final Class<E> enumClass, final E defaultValue);

	boolean getPropertyAsBoolean(final String key, final boolean defaultValue);

	/**
	 * Gets a version number that is guaranteed to be changed whenever the underlying files change.
	 * <p>
	 * It is <i>not</i> guaranteed to be incremented! Depending on the underlying change, a newer
	 * version number might be less than a previous version number! In most cases, however, the
	 * version number actually grows with each change. Code must not rely on this, but it is a
	 * helpful assumption for debugging.
	 * @return a version number that is guaranteed to be changed whenever the underlying files change.
	 */
	long getVersion();

	/**
	 * Gets all config-property-keys matching the given regular expression.
	 * <p>
	 * Just like {@link #getProperty(String, String)}, this method takes inheritance into account:
	 * It collects keys from the current {@code Config} instance and all its parents, recursively.
	 * <p>
	 * Note, that {@link Matcher#matches()} is used, i.e. the {@code regex} must match the entire
	 * config-key.
	 * <p>
	 * The given {@code regex} may contain capturing groups, whose results are returned in the {@code Map}'s
	 * values. Note, that the entire match (which is by convention the group 0) is ignored in the values,
	 * because it is the {@code Map}'s key, already. Hence, the first entry in the {@code Map}'s values
	 * (with index 0) corresponds to the first capturing group in the regular expression.
	 * <p>
	 * For example, let's say the regex is "ignore\[([^]]*)\]\.(.*)" and the following matching properties
	 * exist:
	 * <ul>
	 * <li>ignore[backup-file].namePattern=*.bak</li>
	 * <li>ignore[backup-file].enabled=false</li>
	 * <li>ignore[class-file].namePattern=*.class</li>
	 * </ul>
	 * <p>
	 * This leads to a resulting map with the following entries:
	 * <ul>
	 * <li>key: "ignore[backup-file].namePattern", value: "backup-file", "namePattern"</li>
	 * <li>key: "ignore[backup-file].enabled", value: "backup-file", "enabled"</li>
	 * <li>key: "ignore[class-file].namePattern", value: "class-file", "namePattern"</li>
	 * </ul>
	 * <p>
	 * @param regex the regular expression to look for. Must not be <code>null</code>.
	 * @return the keys found together with capturing groups. Never <code>null</code>.
	 */
	Map<String, List<String>> getKey2GroupsMatching(Pattern regex);
}
