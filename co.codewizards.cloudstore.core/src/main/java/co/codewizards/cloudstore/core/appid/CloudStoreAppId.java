package co.codewizards.cloudstore.core.appid;

public class CloudStoreAppId implements AppId {

	@Override
	public int getPriority() {
		return -100;
	}

	@Override
	public String getSimpleId() {
		return "cloudstore"; //$NON-NLS-1$
	}

	@Override
	public String getQualifiedId() {
		return "co.codewizards.cloudstore"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return "CloudStore"; //$NON-NLS-1$
	}

	@Override
	public String getWebSiteBaseUrl() {
		return "http://cloudstore.codewizards.co/"; //$NON-NLS-1$
	}
}
