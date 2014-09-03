package co.codewizards.cloudstore.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;

import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

public class RestRepoTransportFactory extends AbstractRepoTransportFactory {

	public static final String PROTOCOL_HTTPS = "https";
	public static final String PROTOCOL_HTTP = "http";

	private volatile Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass;

	public Class<? extends DynamicX509TrustManagerCallback> getDynamicX509TrustManagerCallbackClass() {
		return dynamicX509TrustManagerCallbackClass;
	}
	public void setDynamicX509TrustManagerCallbackClass(Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass) {
		this.dynamicX509TrustManagerCallbackClass = dynamicX509TrustManagerCallbackClass;
	}

	@Override
	public String getName() {
		return "REST";
	}

	@Override
	public String getDescription() {
		return "Repository on a remote server accessible via REST";
	}

	@Override
	public boolean isSupported(URL remoteRoot) {
		return PROTOCOL_HTTP.equals(AssertUtil.assertNotNull("remoteRoot", remoteRoot).getProtocol())
				|| PROTOCOL_HTTPS.equals(remoteRoot.getProtocol());
	}

	@Override
	protected RepoTransport _createRepoTransport() {
		return new RestRepoTransport();
	}
}
