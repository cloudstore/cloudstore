package co.codewizards.cloudstore.rest.server.ldap;

import javax.ws.rs.WebApplicationException;

import net.jcip.annotations.NotThreadSafe;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
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
@CreateDS(
	    name = "AddExampleCom",
	    partitions =
	        {
	            @CreatePartition(
	                name = "example",
	                suffix = "dc=example,dc=com",
	                contextEntry = @ContextEntry(
	                    entryLdif =
	                    "dn: dc=example,dc=com\n" +
	                        "dc: example\n" +
	                        "objectClass: top\n" +
	                        "objectClass: domain\n\n"))

	    },
	    enableChangeLog = true)
@NotThreadSafe
public class QueryLdapClientTest extends AbstractLdapTestUnit{

	private static final String ADMIN_DN = "uid=admin,ou=system";
	private static final String ADMIN_PASSWORD = "secret";

	private static final String USER_NAME = "testUser";
	private static final String USER_PASSWORD = "testUserPassword";

	private static final String QUERY = "(&(|(cn=${login})(mail=${login}))(objectClass=inetOrgPerson))";

	private LdapConnection connection;
	private QueryLdapClient client;

	@Before
    public void setup() throws Exception {
		LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setLdapPort( ldapServer.getPort() );
        config.setName(ADMIN_DN);
        config.setCredentials(ADMIN_PASSWORD);
        connection = new LdapNetworkConnection( config);
    }

    @After
    public void shutdown() throws Exception{
    	if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void when_query_returns_one_result_and_this_result_is_bounded_to_context_then_authenticate() throws LdapException{
    	addUser(USER_NAME, USER_PASSWORD, "");
    	client = client(QUERY, "dc=example,dc=com");
    	client.authenticate(new Auth(USER_NAME, USER_PASSWORD.toCharArray()));
    }

    @Test(expected = WebApplicationException.class)
    public void when_query_returns_result_but_password_is_wrong_then_throw_WAE() throws LdapException{
    	addUser(USER_NAME, USER_PASSWORD, "");
    	client = client(QUERY, "ou=system");
    	client.authenticate(new Auth(USER_NAME, "wrongPassword".toCharArray()));
    }

    @Test
    public void when_there_exist_multiple_results_for_query_and_password_is_correct_only_for_the_last_result_then_still_authenticate() throws LdapException{
    	String user2Password = "user2Password";
    	String email = "test@test.com";
    	// two users with the same email
    	addUser(USER_NAME, USER_PASSWORD, email);
    	addUser("testUser2", user2Password, email);

		client = client(QUERY, "dc=example,dc=com");
		client.authenticate(new Auth("test@test.com", user2Password.toCharArray()));

    }

    private void addUser(String cn, String password, String email) throws LdapException{
    	connection.bind(ADMIN_DN, ADMIN_PASSWORD);
		connection.add(
    	        new DefaultEntry(getService().getSchemaManager(),
    	        	"cn=" + cn + ",dc=example,dc=com",
    	            "ObjectClass: top",
    	            "ObjectClass: person",
    	            "ObjectClass: inetOrgPerson",
    	            "userPassword", password,
    	            "cn",cn,
    	            "sn",cn,
    	            "mail", email
    	            ) );
    }

    private QueryLdapClient client(String query, String queryDn) {
		return new QueryLdapClient(query, queryDn, "ldap://localhost:"+ ldapServer.getPort(), ADMIN_DN, ADMIN_PASSWORD.toCharArray());
	}
}
