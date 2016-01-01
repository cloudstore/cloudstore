package co.codewizards.cloudstore.rest.server.ldap;

import static org.assertj.core.api.Assertions.*;
import net.jcip.annotations.NotThreadSafe;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.otp.OneTimePadRegistry;

@NotThreadSafe
public class LdapClientProviderTest {

	private LdapClientProvider provider;
	private DnTemplatePropertyHelper helper;

	@Before
	public void setUp(){
		helper = new DnTemplatePropertyHelper(10);
	}

	@After
	public void cleanUp(){
		helper.removePatterns();
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + LdapClientProvider.LDAP_QUERY);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + LdapClientProvider.LDAP_QUERY_DN);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + LdapClientProvider.LDAP_ADMIN_DN);
	}

	@Test(expected = IllegalStateException.class)
	public void when_query_is_empty_and_templates_list_is_empty_then_provider_can_be_created_but_throws_ISE_when_getClient_called(){
		setSystemProperty(LdapClientProvider.LDAP_QUERY, "");
		helper.setPatterns();
		provider = new LdapClientProvider();
		provider.getClient();
	}

	@Test
	public void when_templates_are_proper_and_query_is_empty_then_simple_client_is_created(){
		setSystemProperty(LdapClientProvider.LDAP_QUERY, "");
		String template = "cn=${login}+sn=secret, ou=users, dc=example, dc=com";
		helper.setPatterns(template);
		provider = new LdapClientProvider();
		assertThat(provider.getClient()).isNotNull().isInstanceOf(SimpleLdapClient.class);
	}

	@Test
	public void when_query_is_not_empty_and_other_required_properties_are_set_then_query_client_is_created(){
		setSystemProperty(LdapClientProvider.LDAP_QUERY, "(|(cn=${login})(&(email=${login})(objectClass=inetOrgPerson))(emailAlias=${login}))");
		setSystemProperty(LdapClientProvider.LDAP_QUERY_DN, "ou=users, dc=example, dc=com");
		setSystemProperty(LdapClientProvider.LDAP_ADMIN_DN, "cn=admin");

		OneTimePadRegistry registry = new OneTimePadRegistry("NOT_EXISTING_FILE_ONLY_FOR_TESTS");
		registry.encryptAndStorePassword("password".toCharArray());
		provider = new LdapClientProvider(registry);
		assertThat(provider.getClient()).isNotNull().isInstanceOf(QueryLdapClient.class);
	}

	private void setSystemProperty(String property, String value){
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + property, value);

	}
}
