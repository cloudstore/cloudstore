package co.codewizards.cloudstore.rest.server.ldap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;

/**
 * Fail-safe initializer of choosen LdapClient implementation.
 * <p>
 * Since LDAP-based authentication is optional - this class won't throw any Exception
 * during initialization, until you call getClient().
 * <p>
 * Choice of proper LdapClient implementation is based on configuration in cloudstore.properties config file.
 *
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class LdapClientProvider {

	private static final Logger log = LoggerFactory.getLogger(LdapClientProvider.class);

	public static final String LDAP_TEMPLATE_PATTERN = "ldap.bindDnTemplate[%d]";
	public static final String LDAP_URL = "ldap.url";
	private static final String LDAP_URL_DEFAULT = "ldap://localhost:10389/";

	private LdapClient ldapClient;

	protected LdapClientProvider(){
		try{
			String url = Config.getInstance().getProperty(LDAP_URL, LDAP_URL_DEFAULT);
			List<String> templates = new DnTemplateCollector().collect();
			ldapClient = new SimpleLdapClient(templates, url);
		} catch(Exception e){
			log.warn("LDAP client initialization failed. If you don't use LDAP you can ignore this warning, otherwise you can increase logging to DEBUG in order to see what is the cause of this failure.");
			log.debug("LDAP client initialization failed", e);
		}
	}

	public LdapClient getClient(){
		if(ldapClient == null){
			throw new IllegalStateException("LDAP is not properly configured. Maybe you forgot to put LDAP properties inside cloudstore.properties?");
		}
		return ldapClient;
	}

	public static LdapClientProvider getInstance(){
		return Helper.INSTANCE;
	}

	private static class Helper{
		private static final LdapClientProvider INSTANCE = new LdapClientProvider();
	}
}
