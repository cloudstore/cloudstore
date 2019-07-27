package co.codewizards.cloudstore.local;

import static java.util.Objects.*;

public enum PersistencePropertiesEnum {
	CONNECTION_DRIVER_NAME("javax.jdo.option.ConnectionDriverName"),

	CONNECTION_URL("javax.jdo.option.ConnectionURL"),

//	/**
//	 * The connection-URL is modified, if the DB needs to be created. This property keeps
//	 * the original connection-URL. However, this URL, too, is resolved (i.e. variables
//	 * are replaced by values).
//	 */
//	CONNECTION_URL_ORIGINAL("_ORIGINAL_javax.jdo.option.ConnectionURL"),

	CONNECTION_USER_NAME("javax.jdo.option.ConnectionUserName"),

	CONNECTION_PASSWORD("javax.jdo.option.ConnectionPassword")
	;

	public final String key;

	private PersistencePropertiesEnum(String key) {
		this.key = requireNonNull(key, "key");
	}

	@Override
	public String toString() {
		return key;
	}

	public static PersistencePropertiesEnum fromKey(String key) {
		for (PersistencePropertiesEnum e : values()) {
			if (e.key.equals(key))
				return e;
		}
		throw new IllegalArgumentException("There is no PersistencePropertiesEnum value for this key: " + key);
	}
}