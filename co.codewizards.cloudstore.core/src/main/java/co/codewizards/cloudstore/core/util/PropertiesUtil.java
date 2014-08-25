package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

/**
 * {@link java.util.Properties} utilities.
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public final class PropertiesUtil
{
	private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

	private PropertiesUtil() { }

	/**
	 * Suffix appended to the real property-key to store the boolean flag whether
	 * the real property represents the <code>null</code> value.
	 * <p>
	 * It is not possible to store a <code>null</code> value in a {@link Properties} instance (and neither it is
	 * in a properties file). But sometimes it is necessary to explicitly formulate a <code>null</code> value,
	 * for example when overriding a property in a way as if it had not been specified in the overridden properties.
	 * <p>
	 * For example, let there be these properties declared in a persistence unit:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.TransactionType = JTA
	 * javax.persistence.jtaDataSource = jdbc/someDataSource
	 * javax.persistence.transactionType = JTA
	 * </pre>
	 * <p>
	 * If the transaction type is to be overridden by "RESOURCE_LOCAL", this is straight-forward:
	 * <p>
	 * <pre>
	 * javax.jdo.option.TransactionType = RESOURCE_LOCAL
	 * javax.persistence.transactionType = RESOURCE_LOCAL
	 * </pre>
	 * <p>
	 * But to override the datasource properties to be null, is not possible by simply writing
	 * "javax.jdo.option.ConnectionFactoryName = = " as this would
	 * be interpreted as empty string. Therefore it is possible to declare the <code>null</code> value using an additional
	 * key:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.ConnectionFactoryName.null = true
	 * javax.persistence.jtaDataSource.null = true
	 * </pre>
	 * It is not necessary to quote the referenced key as shown in the 2nd example ("javax.persistence.jtaDataSource" is
	 * not present). However, if it is present ("javax.jdo.option.ConnectionFactoryName" above), the
	 * null-indicating meta-property "javax.jdo.option.ConnectionFactoryName.null" overrides the value "jdbc/someDataSource".
	 *
	 * @see #getMetaPropertyKeyNullValue(String)
	 * @see #filterProperties(Map, Map)
	 */
	public static final String SUFFIX_NULL_VALUE = ".null";

	/**
	 * Get all matches where property keys match the given pattern.
	 * @param properties The properties to match
	 * @param pattern The pattern to match against
	 * @return The {@link Matcher}s that matched.
	 */
	public static Collection<Matcher> getPropertyKeyMatches(final java.util.Properties properties, final Pattern pattern)
	{
		final Collection<Matcher> matches = new ArrayList<Matcher>();
		for (final Object element : properties.keySet()) {
			final String key = (String) element;
			final Matcher m = pattern.matcher(key);
			if(m.matches())
				matches.add(m);
		}
		return matches;
	}

	/**
	 * Get all properties whose keys start with the given prefix.
	 * <p>
	 *  The returned property elements have the form <code>(key,value)</code> where <code>key</code> is
	 *  the part of the original key after the given <code>keyPrefix</code> and <code>value</code> is
	 *  the original value.
	 * </p>
	 * @param properties The properties to filter.
	 * @param keyPrefix The kex prefix to use
	 * @return the properties that start with the given prefix
	 */
	public static java.util.Properties getProperties(final java.util.Properties properties, final String keyPrefix)
	{
		final java.util.Properties newProperties = new java.util.Properties();
		final Collection<Matcher> matches = getPropertyKeyMatches(properties, Pattern.compile("^"+Pattern.quote(keyPrefix)+"(.*)$"));
		for (final Matcher m : matches)
			newProperties.put(m.group(1), properties.get(m.group(0)));
		return newProperties;
	}

	public static void putAll(final java.util.Properties source, final java.util.Properties target)
	{
		for (final Object element : source.keySet()) {
			final String key = (String) element;
			target.setProperty(key, source.getProperty(key));
		}
	}

	public static java.util.Properties load(final String filename) throws IOException
	{
		return load(filename != null ? createFile(filename) : null);
	}

	public static java.util.Properties load(final File file) throws IOException
	{
		final InputStream in = file.createFileInputStream();
		try {
			final java.util.Properties properties = new java.util.Properties();
			properties.load(in);
			return properties;
		} finally {
			in.close();
		}
	}

	public static void store(final String filename, final java.util.Properties properties, final String comment) throws IOException
	{
		store(filename != null ? createFile(filename) : null, properties, comment);
	}

	public static void store(final File file, final java.util.Properties properties, final String comment) throws IOException
	{
		final OutputStream out = file.createFileOutputStream();
		try {
			properties.store(out, comment);
		} finally {
			out.close();
		}
	}

	/**
	 * Filter the given raw properties.
	 * <p>
	 * This is a convenience method delegating to
	 * {@link #filterProperties(Map, Map)} with <code>variables == null</code>.
	 * @param rawProperties the properties to be filtered; must not be <code>null</code>.
	 * @return the filtered properties.
	 * @see #filterProperties(Map, Map)
	 */
	public static Map<String, String> filterProperties(final Map<?, ?> rawProperties)
	{
		return filterProperties(rawProperties, null);
	}

	/**
	 * Filter the given raw properties.
	 * <p>
	 * <u>Replace null-meta-data to <code>null</code> values:</u> Every property for which the
	 * method {@link #isNullValue(Map, String)} returns <code>true</code> is written with a <code>null</code>
	 * value into the result-map. Every property for which the method {@link #isMetaPropertyKeyNullValue(String)}
	 * returns <code>true</code>, is ignored (i.e. does not occur in the result-map).
	 * <p>
	 * It is not possible to store a <code>null</code> value in a {@link Properties} instance (and neither it is
	 * in a properties file). But sometimes it is necessary to explicitly formulate a <code>null</code> value,
	 * for example when overriding a property in a way as if it had not been specified in the overridden properties.
	 * <p>
	 * For example, let there be these properties declared in a persistence unit:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.TransactionType = JTA
	 * javax.persistence.jtaDataSource = jdbc/someDataSource
	 * javax.persistence.transactionType = JTA
	 * </pre>
	 * <p>
	 * If the transaction type is to be overridden by "RESOURCE_LOCAL", this is straight-forward:
	 * <p>
	 * <pre>
	 * javax.jdo.option.TransactionType = RESOURCE_LOCAL
	 * javax.persistence.transactionType = RESOURCE_LOCAL
	 * </pre>
	 * <p>
	 * But to override the datasource properties to be null, is not possible by simply writing
	 * "javax.jdo.option.ConnectionFactoryName = = " as this would
	 * be interpreted as empty string. Therefore it is possible to declare the <code>null</code> value using an additional
	 * key:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.ConnectionFactoryName.null = true
	 * javax.persistence.jtaDataSource.null = true
	 * </pre>
	 * It is not necessary to quote the referenced key as shown in the 2nd example ("javax.persistence.jtaDataSource" is
	 * not present). However, if it is present ("javax.jdo.option.ConnectionFactoryName" above), the
	 * null-indicating meta-property "javax.jdo.option.ConnectionFactoryName.null" overrides the value "jdbc/someDataSource".
	 * <p>
	 * <u>Replace template variables:</u> If the optional <code>variables</code> argument is present, every
	 * value is filtered using {@link IOUtil#replaceTemplateVariables(String, Map)}.
	 * <p>
	 * For example, let there be these properties:
	 * <pre>
	 * some.url1 = file:${java.io.tmpdir}/myTempDir
	 * some.url2 = http://host.domain.tld/${user.name}
	 * </pre>
	 * <p>
	 * If this method is called with {@link System#getProperties()} as <code>variables</code> and the user "marco" is currently
	 * working on a linux machine, the resulting <code>Map</code> will contain the following resolved properties:
	 * <pre>
	 * some.url1 = file:/tmp/myTempDir
	 * some.url2 = http://host.domain.tld/marco
	 * </pre>
	 * @param rawProperties the properties to be filtered; must not be <code>null</code>.
	 * @param variables optional template variables; if present, every value is filtered using
	 * {@link IOUtil#replaceTemplateVariables(String, Map)}.
	 * @return the filtered properties.
	 */
	public static Map<String, String> filterProperties(final Map<?, ?> rawProperties, final Map<?, ?> variables)
	{
		if (rawProperties == null)
			throw new IllegalArgumentException("rawProperties == null");

		final Map<String, String> filteredProperties = new HashMap<String, String>();
		for (final Map.Entry<?, ?> me : rawProperties.entrySet()) {
			final String key = me.getKey() == null ? null : me.getKey().toString();
			String value = me.getValue() == null ? null : me.getValue().toString();

			if (isMetaPropertyKey(key)) {
				if (isMetaPropertyKeyNullValue(key) && Boolean.parseBoolean(value)) {
					final String refKey = getReferencedPropertyKeyForMetaPropertyKey(key);
					filteredProperties.put(refKey, null);
				}
				continue;
			}

			if (value != null && isNullValue(rawProperties, key))
				value = null;

			if (value != null && variables != null)
				value = IOUtil.replaceTemplateVariables(value, variables);

			filteredProperties.put(key, value);
		}
		return filteredProperties;
	}

	/**
	 * Determine, if the given property-key is a <code>null</code>-indicating meta-property for another property.
	 * <p>
	 * It is not possible to store a <code>null</code> value in a {@link Properties} instance (and neither it is
	 * in a properties file). But sometimes it is necessary to explicitly formulate a <code>null</code> value,
	 * for example when overriding a property in a way as if it had not been specified in the overridden properties.
	 * <p>
	 * For example, let there be these properties declared in a persistence unit:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.TransactionType = JTA
	 * javax.persistence.jtaDataSource = jdbc/someDataSource
	 * javax.persistence.transactionType = JTA
	 * </pre>
	 * <p>
	 * If the transaction type is to be overridden by "RESOURCE_LOCAL", this is straight-forward:
	 * <p>
	 * <pre>
	 * javax.jdo.option.TransactionType = RESOURCE_LOCAL
	 * javax.persistence.transactionType = RESOURCE_LOCAL
	 * </pre>
	 * <p>
	 * But to override the datasource properties to be null, is not possible by simply writing
	 * "javax.jdo.option.ConnectionFactoryName = = " as this would
	 * be interpreted as empty string. Therefore it is possible to declare the <code>null</code> value using an additional
	 * key:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.ConnectionFactoryName.null = true
	 * javax.persistence.jtaDataSource.null = true
	 * </pre>
	 * It is not necessary to quote the referenced key as shown in the 2nd example ("javax.persistence.jtaDataSource" is
	 * not present). However, if it is present ("javax.jdo.option.ConnectionFactoryName" above), the
	 * null-indicating meta-property "javax.jdo.option.ConnectionFactoryName.null" overrides the value "jdbc/someDataSource".
	 *
	 * @param key the property-key to check.
	 * @return <code>true</code>, if the given key references a property that is a <code>null</code>-indicating
	 * meta-property for another property.
	 * @see #isMetaPropertyKey(String)
	 */
	public static boolean isMetaPropertyKeyNullValue(final String key)
	{
		if (key == null)
			return false;

		return key.endsWith(SUFFIX_NULL_VALUE);
	}

	/**
	 * Determine, if the given property-key is a meta-property for another property.
	 * <p>
	 * Currently, this is equivalent to {@link #isMetaPropertyKeyNullValue(String)}, but other
	 * meta-properties might be introduced later.
	 * @param key the property-key to check.
	 * @return <code>true</code>, if the given key references a property that is a meta-property
	 * for another property.
	 */
	public static boolean isMetaPropertyKey(final String key)
	{
		return isMetaPropertyKeyNullValue(key);
	}

	/**
	 * Get the referenced property-key for the given meta-property's key.
	 * @param key a meta-property's key - for example the <code>null</code>-indicating property-key
	 * "some.prop.null".
	 * @return the referenced property-key - for example "some.prop".
	 * @see #SUFFIX_NULL_VALUE
	 * @see #getMetaPropertyKeyNullValue(String)
	 */
	public static String getReferencedPropertyKeyForMetaPropertyKey(final String key)
	{
		if (!isMetaPropertyKeyNullValue(key))
			throw new IllegalArgumentException("key='" + key + "' is not a meta-property!");

		return key.substring(0, key.length() - SUFFIX_NULL_VALUE.length());
	}

	/**
	 * Get the <code>null</code>-indicating meta-property's key for the given real property's key.
	 * @param key a property-key - for example "some.prop".
	 * @return the <code>null</code>-indicating meta-property's key - for example "some.prop.null".
	 * @see #SUFFIX_NULL_VALUE
	 * @see #getReferencedPropertyKeyForMetaPropertyKey(String)
	 */
	public static String getMetaPropertyKeyNullValue(String key)
	{
		if (key == null)
			key = String.valueOf(key);

		if (isMetaPropertyKeyNullValue(key))
			throw new IllegalArgumentException("key='" + key + "' is already a meta-property indicating a null-value!");

		return key + SUFFIX_NULL_VALUE;
	}

	/**
	 * Determine, if the property identified by the given <code>key</code> has a <code>null</code>-value.
	 * <p>
	 * It is not possible to store a <code>null</code> value in a {@link Properties} instance (and neither it is
	 * in a properties file). But sometimes it is necessary to explicitly formulate a <code>null</code> value,
	 * for example when overriding a property in a way as if it had not been specified in the overridden properties.
	 * <p>
	 * For example, let there be these properties declared in a persistence unit:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.TransactionType = JTA
	 * javax.persistence.jtaDataSource = jdbc/someDataSource
	 * javax.persistence.transactionType = JTA
	 * </pre>
	 * <p>
	 * If the transaction type is to be overridden by "RESOURCE_LOCAL", this is straight-forward:
	 * <p>
	 * <pre>
	 * javax.jdo.option.TransactionType = RESOURCE_LOCAL
	 * javax.persistence.transactionType = RESOURCE_LOCAL
	 * </pre>
	 * <p>
	 * But to override the datasource properties to be null, is not possible by simply writing
	 * "javax.jdo.option.ConnectionFactoryName = = " as this would
	 * be interpreted as empty string. Therefore it is possible to declare the <code>null</code> value using an additional
	 * key:
	 * <pre>
	 * javax.jdo.option.ConnectionFactoryName = jdbc/someDataSource
	 * javax.jdo.option.ConnectionFactoryName.null = true
	 * javax.persistence.jtaDataSource.null = true
	 * </pre>
	 * It is not necessary to quote the referenced key as shown in the 2nd example ("javax.persistence.jtaDataSource" is
	 * not present). However, if it is present ("javax.jdo.option.ConnectionFactoryName" above), the
	 * null-indicating meta-property "javax.jdo.option.ConnectionFactoryName.null" overrides the value "jdbc/someDataSource".
	 *
	 * @param properties the properties. Must not be <code>null</code>.
	 * @param key the property-key for which to determine, whether its value is <code>null</code>.
	 * @return <code>true</code>, if the property referenced by the given <code>key</code> is <code>null</code>;
	 * <code>false</code> otherwise.
	 */
	public static boolean isNullValue(final Map<?, ?> properties, final String key)
	{
		if (properties == null)
			throw new IllegalArgumentException("properties == null");

		if (properties.get(key) == null)
			return true;

		if (isMetaPropertyKeyNullValue(key))
			return false;

		final String metaNullValue = String.valueOf(properties.get(getMetaPropertyKeyNullValue(key)));
		return Boolean.parseBoolean(metaNullValue);
	}

	public static int getSystemPropertyValueAsInt(final String key, final int defaultValue) {
		final long value = getSystemPropertyValueAsLong(key, defaultValue);
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
			logger.warn("System property '{}' is set to the value '{}' which is out of range for a 32-bit integer. Falling back to default value {}.",
					key, value, defaultValue);
			return defaultValue;
		}
		return (int) value;
	}

	public static long getSystemPropertyValueAsLong(final String key, final long defaultValue) {
		final String value = System.getProperty(key);
		if (value == null) {
			logger.debug("System property '{}' is not set. Falling back to default value {}.", key, defaultValue);
			return defaultValue;
		}
		try {
			return Integer.valueOf(value);
		} catch (final NumberFormatException x) {
			logger.warn("System property '{}' is set to the value '{}' which is not an integer (long). Falling back to default value {}.",
					key, value, defaultValue);
			return defaultValue;
		}
	}
}
