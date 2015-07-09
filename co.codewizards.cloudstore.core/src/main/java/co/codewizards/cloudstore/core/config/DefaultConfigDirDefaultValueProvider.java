package co.codewizards.cloudstore.core.config;

public class DefaultConfigDirDefaultValueProvider implements ConfigDirDefaultValueProvider {

	@Override
	public int getPriority() {
		return -100;
	}

	@Override
	public String getConfigDir() {
		return "${user.home}/.cloudstore";
	}
}
