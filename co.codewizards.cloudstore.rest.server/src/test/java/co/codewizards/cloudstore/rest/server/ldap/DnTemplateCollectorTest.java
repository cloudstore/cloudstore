package co.codewizards.cloudstore.rest.server.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import net.jcip.annotations.NotThreadSafe;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@NotThreadSafe
public class DnTemplateCollectorTest {

	private DnTemplateCollector fetcher;
	private DnTemplatePropertyHelper helper;

	@Before
	public void setUp(){
		fetcher = new DnTemplateCollector();
		helper = new DnTemplatePropertyHelper(10);
	}

	@After
	public void cleanUp(){
		helper.removePatterns();
	}

	@Test
	public void when_templates_are_empty_then_return_empty_list(){
		helper.setPatterns();
		assertThat(fetcher.collect()).isEmpty();
	}

	@Test
	public void when_there_is_one_template_then_return_it(){
		String template = "cn=${login}+sn=secret, ou=users, dc=example, dc=com";
		helper.setPatterns(template);
		assertThat(fetcher.collect()).containsExactly(template);
	}

	@Test
	public void when_first_template_is_empty_but_second_is_not_then_still_return_empty_list(){
		String template = "cn=${login}+sn=secret, ou=users, dc=example, dc=com";
		helper.setPatterns("", template);
		assertThat(fetcher.collect()).isEmpty();
	}

	@Test
	public void when_the_are_multiple_templates_then_return_them_in_the_same_order(){
		String template0 = "cn=${login}+sn=secret, ou=users, dc=example, dc=com";
		String template1 = "cn=${login}+sn=secret, ou=customers, dc=example, dc=com";
		String template2 = "cn=${login}+sn=secret, ou=users, dc=test, dc=com";
		helper.setPatterns(template0, template1, template2);
		assertThat(fetcher.collect()).containsSequence(template0, template1, template2);
	}

}
