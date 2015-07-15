package co.codewizards.cloudstore.client;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.otp.OneTimePadRegistry;
import co.codewizards.cloudstore.core.otp.OneTimePadRegistryFactory;
/**
 * {@link SubCommand} implementation for changing LDAP admin password.
 * @author wilk
 */
public class ChangeLdapPasswordSubCommand extends SubCommand{

	@Argument(metaVar="<password>", index = 0, required=true, usage="New LDAP admin password")
	private String password;

	@Override
	public String getSubCommandDescription() {
		return "Change LDAP admin password.";
	}

	@Override
	public void run() throws Exception {
		OneTimePadRegistry registry = OneTimePadRegistryFactory.forLdapAdminCredentials();
		registry.encryptAndStorePassword(password.toCharArray());
		System.out.println("LDAP admin password changed successfully.");
	}

}
