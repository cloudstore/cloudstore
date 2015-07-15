package co.codewizards.cloudstore.rest.server.ldap;

import java.util.Hashtable;

import javax.naming.Context;

@SuppressWarnings({"rawtypes","unchecked"})
public class LdapConfig extends Hashtable{

	private static final long serialVersionUID = -368776156473298955L;
	private static final String CONTEXT_FACTORY_DEFAULT = "com.sun.jndi.ldap.LdapCtxFactory";
	private static final String AUTHENTICATION_DEFAULT = "simple";

	public LdapConfig(String url, String userName, char[] password){
		putConstants();
		put(Context.PROVIDER_URL, url);
		put(Context.SECURITY_PRINCIPAL, userName);
		put(Context.SECURITY_CREDENTIALS, password);
	}

	private void putConstants(){
		put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY_DEFAULT);
		put(Context.SECURITY_AUTHENTICATION, AUTHENTICATION_DEFAULT);
	}
}
