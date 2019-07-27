package co.codewizards.cloudstore.rest.server.ldap;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.server.auth.Auth;
import co.codewizards.cloudstore.rest.server.auth.NotAuthorizedException;
/**
 * Authentication flow used by this client:
 * At first DirContext is created, based on provided url, adminDn and adminPassword.
 * Then on this DirContext instance search is executed for given LDAP query and queryDN.
 *
 * Search has SUBTREE_SCOPE.
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/naming/directory/SearchControls.html#SUBTREE_SCOPE">SUBTREE_SCOPE</a>
 *
 * Query should contain template variable ${login}(one or more) which is replaced with userName from provided Auth object
 *
 * If search returns any results then credentials of DirContext are replaced with result's DN (as PRINCIPAL)
 * and password from provided Auth (as CREDENTIALS), and lookup() is called.
 *
 * If lookup() doesn't throw AuthenticationException then authentication succeeded.
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class QueryLdapClient implements LdapClient{

	private static final String TEMPLATE_VARIABLE = "login";

	private final String query;
	private final String queryDn;
	private final String url;
	private final String adminDn;
	private final char[] adminPassword;

	public QueryLdapClient(String query, String queryDn, String url, String bindDn, char[] password) {
		this.query = requireNonNull(query, "query");
		this.queryDn = requireNonNull(queryDn, "queryDn");
		this.url = requireNonNull(url, "url");
		this.adminDn = requireNonNull(bindDn, "bindDn");
		this.adminPassword = requireNonNull(password, "password");
	}

	@Override
	public String authenticate(final Auth auth) {
		try{
			final LdapConfig config = new LdapConfig(url, adminDn, adminPassword);
			final DirContext context = new InitialDirContext(config);

			List<String> usersDns = findAllUsersThatMatchQuery(context, auth);

			for(String userDn : usersDns)
				if(tryAuthenticate(context, userDn, auth.getPassword()))
					return auth.getUserName();
		}catch(NamingException e){
			throw new RuntimeException(e);
		}
		throw new NotAuthorizedException();
	}

	private List<String> findAllUsersThatMatchQuery(final DirContext context, final Auth auth) throws NamingException{
		final NamingEnumeration<SearchResult> results = findUsersWithQuery(context, auth.getUserName());
		List<String> usersDns = new ArrayList<>();
		while(results.hasMore())
			usersDns.add(results.next().getNameInNamespace());
		return usersDns;
	}

	private NamingEnumeration<SearchResult> findUsersWithQuery(DirContext context, String userName) throws NamingException{
		final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String replacedQuery = convertTemplate(query, userName);
		return context.search(queryDn, replacedQuery, searchControls);
	}

	private boolean tryAuthenticate(final DirContext context, final String userName, final char[] password) throws NamingException{
		try{
			context.addToEnvironment(Context.SECURITY_PRINCIPAL, userName);
			context.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
			context.lookup(userName);
			return true;
		} catch(AuthenticationException e){
			return false;
		}
	}

	private String convertTemplate(final String template, final String username){
		final Map<String, String> map = new HashMap<String, String>(1);
		map.put(TEMPLATE_VARIABLE, username);
		return IOUtil.replaceTemplateVariables(template, map);
	}
}
