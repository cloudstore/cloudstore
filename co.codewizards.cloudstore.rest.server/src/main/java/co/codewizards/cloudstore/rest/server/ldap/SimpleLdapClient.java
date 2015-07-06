package co.codewizards.cloudstore.rest.server.ldap;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotEmpty;
import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.server.auth.Auth;

/**
 * Simple implementation of LdapClient.
 * <p>
 * It is initialized with a list of DN templates, that are used to authenticate user.
 * Example DN template:
 * cn=${login}+sn=secret,ou=users,dc=example,dc=com
 * Where login is user's name provided by user.
 *
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class SimpleLdapClient implements LdapClient{

	private static final String CONTEXT_FACTORY_DEFAULT = "com.sun.jndi.ldap.LdapCtxFactory";
	private static final String AUTHENTICATION_DEFAULT = "simple";
	private static final String TEMPLATE_VARIABLE = "login";

	private final List<String> templates;
	private final String url;

	public SimpleLdapClient(final List<String> templates, final String url){
		this.templates = assertNotEmpty("templates", templates);
		validateTemplates(templates);
		this.url = assertNotNull("url", url);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String authenticate(final Auth auth){
		final Hashtable env = basicLdapConfig();

		env.put(Context.SECURITY_CREDENTIALS, auth.getPassword());
		for(String template : templates){
			env.put(Context.SECURITY_PRINCIPAL, convertTemplate(template, auth.getUserName()));
			if(tryAuthenticate(env)){
				return auth.getUserName();
			}
		}
		throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"CloudStoreServer\"").build());
	}

	@SuppressWarnings("rawtypes")
	private boolean tryAuthenticate(Hashtable env){
		try {
			new InitialDirContext(env);
			return true;
		} catch (AuthenticationException e) {
			return false;
		} catch(NamingException e){
			throw new RuntimeException(e);
		}
	}


	private String convertTemplate(final String template, final String username){
		final Map<String, String> map = new HashMap<String, String>(1);
		map.put(TEMPLATE_VARIABLE, username);
		return IOUtil.replaceTemplateVariables(template, map);
	}

	private void validateTemplates(List<String> templates){
		String variable = "${" + TEMPLATE_VARIABLE + "}";
		for(String template : templates){
			if(!template.contains(variable))
				throw new IllegalArgumentException("every template has to contain " + variable);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Hashtable basicLdapConfig(){
		final Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY_DEFAULT);
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, AUTHENTICATION_DEFAULT);
		return env;
	}

}
