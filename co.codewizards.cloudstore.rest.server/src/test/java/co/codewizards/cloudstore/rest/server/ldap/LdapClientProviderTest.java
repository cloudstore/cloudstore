package co.codewizards.cloudstore.rest.server.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import net.jcip.annotations.NotThreadSafe;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	}

	@Test(expected = IllegalStateException.class)
	public void when_templates_list_is_empty_then_provider_can_be_created_but_throws_ISE_when_getClient_called(){
		helper.setPatterns();
		provider = new LdapClientProvider();
		provider.getClient();
	}

	@Test
	public void when_templates_are_proper_then_client_is_created(){
		String template = "cn=${login}+sn=secret, ou=users, dc=example, dc=com";
		helper.setPatterns(template);
		provider = new LdapClientProvider();
		assertThat(provider.getClient()).isNotNull();
	}
}
