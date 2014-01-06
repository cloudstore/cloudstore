package co.codewizards.cloudstore.client;

import java.net.URL;

public class CloudStoreClient implements Runnable {

	private URL remoteRoot;

	public CloudStoreClient() { }

	public static void main(String[] args) {
		new CloudStoreClient().run();
	}

	public URL getRemoteRoot() {
		return remoteRoot;
	}
	public void setRemoteRoot(URL remoteRoot) {
		this.remoteRoot = remoteRoot;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
