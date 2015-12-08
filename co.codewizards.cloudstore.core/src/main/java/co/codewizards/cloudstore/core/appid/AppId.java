package co.codewizards.cloudstore.core.appid;

/**
 * Service providing the identity of the currently running application.
 * <p>
 * CloudStore might be used as the base for another application, i.e. as a library.
 * In this case, the identity of this other application should use consistent file and
 * directory names, e.g. for its configuration. So for example, the configuration directory
 * "~/.cloudstore" should be "~/.myapp" instead.
 * <p>
 * Additionally, the auto-update must use a different web-site.
 * <p>
 * Therefore, the other application should implement an {@code AppId} with a higher
 * {@link #getPriority() priority} than the {@link CloudStoreAppId} (having the negative
 * priority -100).
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface AppId {

	/**
	 * Gets the priority of this {@code AppId} implementation. The {@link AppIdRegistry} chooses
	 * the {@code AppId} with the highest priority (the greatest number).
	 * @return the priority of this {@code AppId} implementation.
	 */
	int getPriority();

	/**
	 * Gets the simple (short) ID without any qualifier prefix.
	 * <p>
	 * Example: "cloudstore"
	 * @return the simple (short) ID without any qualifier prefix. Never <code>null</code>.
	 */
	String getSimpleId();

	/**
	 * Gets the qualified (long) ID.
	 * <p>
	 * Example: "co.codewizards.cloudstore"
	 * @return the qualified (long) ID. Never <code>null</code>.
	 */
	String getQualifiedId();

	/**
	 * Gets the name used by humans.
	 * <p>
	 * Example: "CloudStore"
	 * @return the name used by humans. Never <code>null</code>.
	 */
	String getName();

	/**
	 * Gets the base-URL. Certain sub-URLs are expected beneath it.
	 * <p>
	 * <b>Important:</b> This URL must end on '/'!
	 * <p>
	 * Example: "http://cloudstore.codewizards.co/" (one of the expected sub-URLs is "http://cloudstore.codewizards.co/update/"
	 * where meta-data about the current version is expected).
	 * @return the base-URL (ending on '/'). Never <code>null</code>.
	 */
	String getWebSiteBaseUrl();

}
