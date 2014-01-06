package co.codewizards.cloudstore.rest.client.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * {@link HostnameVerifier} implementation allowing all host names.
 * <p>
 * We use a separate trust-store for every destination host + port. Additionally, we use self-signed certificates
 * having the same common name for all servers. The user must verify himself and then explicitely accept every
 * server certificate. Thus we do not to verify any host name and use this class allowing them all without any check.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class HostnameVerifierAllowingAll implements HostnameVerifier {
	@Override
	public boolean verify(String name, SSLSession session) {
		return true;
	}
}
