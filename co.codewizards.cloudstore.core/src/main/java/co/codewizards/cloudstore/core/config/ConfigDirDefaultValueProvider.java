package co.codewizards.cloudstore.core.config;

public interface ConfigDirDefaultValueProvider {

	int getPriority();

	String getConfigDir();

}
