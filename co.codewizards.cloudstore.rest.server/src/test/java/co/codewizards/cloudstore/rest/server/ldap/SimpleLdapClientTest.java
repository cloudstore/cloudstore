package co.codewizards.cloudstore.rest.server.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.ws.rs.WebApplicationException;

import net.jcip.annotations.NotThreadSafe;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.codewizards.cloudstore.rest.server.auth.Auth;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
      {
        @CreateTransport(protocol = "LDAP")
      })
@NotThreadSafe
public class SimpleLdapClientTest extends AbstractLdapTestUnit{

	private static final String ADMIN_DN = "uid=admin,ou=system";
	private static final String ADMIN_PASSWORD = "secret";

	private static final String USER_NAME = "testUser";
	private static final String USER_DN = "cn=testUser,ou=system";
	private static final String USER_PASSWORD = "testUserPassword";

    private LdapConnection connection;
    private SimpleLdapClient client;

    @Before
    public void setup() throws Exception {
    	LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setLdapPort( ldapServer.getPort() );
        config.setName(ADMIN_DN);
        config.setCredentials(ADMIN_PASSWORD);
    	connection = new LdapNetworkConnection( config);
    	connection.bind(ADMIN_DN, ADMIN_PASSWORD);
		connection.add(
    	        new DefaultEntry(
    	        	USER_DN,
    	            "ObjectClass: top",
    	            "ObjectClass: person",
    	            "userPassword", USER_PASSWORD,
    	            "cn", "testUser",
    	            "sn", "test"
    	            ) );
		connection.unBind();
    }

    @After
    public void shutdown() throws Exception{
    	if(connection != null) {
    		connection.bind(ADMIN_DN, ADMIN_PASSWORD);
    		connection.delete(USER_DN);
    		connection.unBind();
            connection.close();
        }
    }

    @Test(expected = WebApplicationException.class)
	public void when_wrong_password_then_throw_401() throws LdapException{
		client = client("cn=${login},ou=system");
		client.authenticate(new Auth(USER_NAME, "wrong_password".toCharArray()));
	}

	@Test(expected = WebApplicationException.class)
	public void when_wrong_user_name_then_throw_401() throws LdapException{
		client = client("cn=${login},ou=system");
		client.authenticate(new Auth("wrong_user", USER_PASSWORD.toCharArray()));
	}

	@Test(expected = WebApplicationException.class)
	public void when_multiple_templates_and_none_is_valid_then_throw_401() throws LdapException{
		client = client("cn=${login},ou=notexisting",
						"cn=${login},ou=nothing, dc=example, dc=com");
		String username = client.authenticate(new Auth(USER_NAME, USER_PASSWORD.toCharArray()));
		assertThat(username).isEqualTo(USER_NAME);
	}

	@Test
	public void when_single_template_and_valid_credentials_then_authenticate_and_return_correct_user_name() throws LdapException{
		client = client("cn=${login},ou=system");
		String username = client.authenticate(new Auth(USER_NAME, USER_PASSWORD.toCharArray()));
		assertThat(username).isEqualTo(USER_NAME);
	}

	@Test
	public void when_multiple_templates_and_last_one_is_valid_then_authenticate() throws LdapException{
		client = client("cn=${login},ou=notexisting",
						"cn=${login},ou=nothing, dc=example, dc=com",
						"cn=${login},ou=system");
		String username = client.authenticate(new Auth(USER_NAME, USER_PASSWORD.toCharArray()));
		assertThat(username).isEqualTo(USER_NAME);
	}

	@Test
	public void when_multiple_templates_and_first_one_is_valid_then_authenticate() throws LdapException{
		client = client("cn=${login},ou=system",
						"cn=${login},ou=notexisting",
						"cn=${login},ou=nothing, dc=example, dc=com");
		String username = client.authenticate(new Auth(USER_NAME, USER_PASSWORD.toCharArray()));
		assertThat(username).isEqualTo(USER_NAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void when_url_is_null_then_throw_IllegalArgumentException(){
		new SimpleLdapClient(Arrays.asList("cn=${login},ou=system"), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void when_templates_are_null_then_throw_IllegalArgumentException(){
		new SimpleLdapClient(null, "ldap://localhost:"+ ldapServer.getPort());
	}

	@Test(expected = IllegalArgumentException.class)
	public void when_templates_dont_contain_template_variable_then_throw_IllegalArgumentException(){
		new SimpleLdapClient(Arrays.asList("cn=login,ou=system"), "ldap://localhost:"+ ldapServer.getPort());
	}

	private SimpleLdapClient client(String... templates) {
		return new SimpleLdapClient(Arrays.asList(templates), "ldap://localhost:"+ ldapServer.getPort());
	}
}
