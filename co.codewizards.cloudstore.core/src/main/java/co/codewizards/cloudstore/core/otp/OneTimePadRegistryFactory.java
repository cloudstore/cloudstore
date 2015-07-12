package co.codewizards.cloudstore.core.otp;

public class OneTimePadRegistryFactory {
	private static final String LDAP_ADMIN_CREDENTIALS_FILE_NAME = "ldapAdminCredentials";

	public static OneTimePadRegistry forLdapAdminCredentials(){
		return new OneTimePadRegistry(LDAP_ADMIN_CREDENTIALS_FILE_NAME);
	}
}
