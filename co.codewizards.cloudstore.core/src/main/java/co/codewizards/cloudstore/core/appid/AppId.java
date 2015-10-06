package co.codewizards.cloudstore.core.appid;

public interface AppId {

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
	 * Example: "http://cloudstore.codewizards.co/" (one of the expected sub-URLs is "http://cloudstore.codewizards.co/update/"
	 * where meta-data about the current version is expected).
	 * @return the base-URL. Never <code>null</code>.
	 */
	String getWebSiteBaseUrl();

}
