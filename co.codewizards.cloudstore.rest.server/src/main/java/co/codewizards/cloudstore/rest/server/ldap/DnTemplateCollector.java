package co.codewizards.cloudstore.rest.server.ldap;

import java.util.ArrayList;
import java.util.List;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.util.StringUtil;

/**
 * Helper class for getting LDAP DN templates from cloudstore.properties config file
 *
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
class DnTemplateCollector{

	private int index;

	DnTemplateCollector() {
	}

	public List<String> collect(){
		index = 0;
		final List<String> templates = new ArrayList<String>();
		for(String nextTemplate = getNextTemplate(); !StringUtil.isEmpty(nextTemplate); index++, nextTemplate = getNextTemplate()){
			templates.add(nextTemplate);
		}
		return templates;
	}

	private String getNextTemplate(){
		return Config.getInstance().getProperty(getNextProperty(), null);
	}

	private String getNextProperty(){
		return String.format(LdapClientProvider.LDAP_TEMPLATE_PATTERN, index);
	}
}