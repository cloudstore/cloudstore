package co.codewizards.cloudstore.rest.server.ldap;

import co.codewizards.cloudstore.rest.server.auth.Auth;

public interface LdapClient {
	String authenticate(final Auth auth);
}
