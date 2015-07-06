package co.codewizards.cloudstore.rest.server.ldap;

import co.codewizards.cloudstore.core.config.Config;

/**
 * Helper class for tests that use LDAP DN templates.
 * <p>
 * It allows overriding system properties in @Before method and clearing them in @After method.
 * <p>
 * It's important to set numberOfTemplates high enough (e.g. 10), so that all local properties are overridden.
 * @see Config
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
class DnTemplatePropertyHelper {

	private static final String SYSTEM_PATTERN = Config.SYSTEM_PROPERTY_PREFIX + LdapClientProvider.LDAP_TEMPLATE_PATTERN;

	/**
	 * Indicates how many of DN templates will be overridden with system properties.
	 * First templates will be overridden with values provided with setPatterns() method,
	 * and next ones will be empty, but will still override local properties.
	 */
	private final int numberOfTemplates;

	public DnTemplatePropertyHelper(int numberOfTemplates){
		this.numberOfTemplates = numberOfTemplates;
	}

	public void setPatterns(String... patterns){
		for(int i=0; i<numberOfTemplates; i++){
			if(i < patterns.length){
				System.setProperty(String.format(SYSTEM_PATTERN, i), patterns[i]);
			} else{
				System.setProperty(String.format(SYSTEM_PATTERN, i), "");
			}
		}
	}

	public void removePatterns(){
		for(int i=0; i<numberOfTemplates; i++){
			System.clearProperty(String.format(SYSTEM_PATTERN, i));
		}
	}
}
