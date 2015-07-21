package co.codewizards.cloudstore.core.otp;

public class LdapPasswordOneTimePadRegistry extends OneTimePadRegistry{

	private static final String LDAP_ADMIN_CREDENTIALS_FILE_NAME = "ldapAdminCredentials";

	public LdapPasswordOneTimePadRegistry(){
		super(LDAP_ADMIN_CREDENTIALS_FILE_NAME);
	}
}
