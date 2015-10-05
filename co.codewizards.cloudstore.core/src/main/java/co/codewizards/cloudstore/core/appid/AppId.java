package co.codewizards.cloudstore.core.appid;

public interface AppId {

	int getPriority();

	String getSimpleId();

	String getQualifiedId();

	String getWebSiteBaseUrl();

}
