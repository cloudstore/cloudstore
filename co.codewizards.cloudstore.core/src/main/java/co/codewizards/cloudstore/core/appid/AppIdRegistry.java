package co.codewizards.cloudstore.core.appid;

import java.util.ServiceLoader;

public class AppIdRegistry {

	private static final class Holder {
		public static final AppIdRegistry instance = new AppIdRegistry();
	}

	public static AppIdRegistry getInstance() {
		return Holder.instance;
	}

	private volatile AppId appId;

	protected AppIdRegistry() {
	}

	public AppId getAppIdOrFail() {
		AppId appId = this.appId;
		if (appId == null) {
			for (final AppId ai : ServiceLoader.load(AppId.class)) {
				if (appId == null || appId.getPriority() < ai.getPriority())
					appId = ai;
			}

			if (appId == null)
				throw new IllegalStateException("No AppId implementation found!");

			this.appId = appId;
		}
		return appId;
	}
}
