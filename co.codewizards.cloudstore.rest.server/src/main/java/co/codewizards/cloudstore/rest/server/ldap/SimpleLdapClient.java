package co.codewizards.cloudstore.rest.server.ldap;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static java.util.Objects.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.server.auth.Auth;
import co.codewizards.cloudstore.rest.server.auth.NotAuthorizedException;

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

	private static final String TEMPLATE_VARIABLE = "login";

	private final List<String> templates;
	private final String url;

	public SimpleLdapClient(final List<String> templates, final String url){
		this.templates = assertNotEmpty(templates, "templates");
		validateTemplates(templates);
		this.url = requireNonNull(url, "url");
	}

	@Override
	public String authenticate(final Auth auth){
		for(String template : templates){
			String userNameTemplate = convertTemplate(template, auth.getUserName());
			LdapConfig config = new LdapConfig(url, userNameTemplate, auth.getPassword());
			if(tryAuthenticate(config)){
				return auth.getUserName();
			}
		}
		throw new NotAuthorizedException();
	}

	private boolean tryAuthenticate(LdapConfig env){
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

}
