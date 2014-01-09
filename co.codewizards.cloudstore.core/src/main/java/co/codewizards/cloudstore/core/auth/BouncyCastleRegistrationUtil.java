package co.codewizards.cloudstore.core.auth;

import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class BouncyCastleRegistrationUtil {

	private BouncyCastleRegistrationUtil() { }

	public static synchronized void registerBouncyCastleIfNeeded() {
		Provider provider = Security.getProvider("BC");
		if (provider != null)
			return;

		Security.addProvider(new BouncyCastleProvider());

		provider = Security.getProvider("BC");
		if (provider == null)
			throw new IllegalStateException("Registration of BouncyCastleProvider failed!");
	}
}
