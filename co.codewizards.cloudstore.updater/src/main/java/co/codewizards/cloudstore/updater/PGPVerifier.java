package co.codewizards.cloudstore.updater;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;

public class PGPVerifier {
	private PGPPublicKeyRingCollection publicKeyRingWithTrustedKeys;
	private final BouncyCastleProvider provider = new BouncyCastleProvider();

	/**
	 * Verify the specified {@code file}.
	 * @param file the file to be verified. Must not be <code>null</code>. There must be a second file
	 * with the same name and the additional suffix ".sig" next to this file (in the same directory).
	 * This secondary file is a so-called detached signature.
	 * @throws PGPVerifyException if the given {@code file} could not be verified successfully. Either
	 * there is no detached-signature-file, or its signature is broken or its signature does not match
	 * any of the {@linkplain #getPublicKeyRingWithTrustedKeys() trusted keys}.
	 */
	public void verify(final File file, final File signatureFile) throws PGPVerifyException {
		assertNotNull("file", file);
		assertNotNull("signatureFile", signatureFile);

		final PGPSignatureList sl = readSignatureFile(signatureFile);
		final PGPPublicKeyRingCollection publicKeyRing = getPublicKeyRingWithTrustedKeys();

		for (int index = 0; index < sl.size(); ++index) {
			try {
				final PGPSignature signature = sl.get(index);
				signature.initVerify(publicKeyRing.getPublicKey(signature.getKeyID()), provider);

				final InputStream contentIn = new FileInputStream(file);
				try {
					final byte[] buf = new byte[16 * 1024];
					int len;
					while (0 <= (len = contentIn.read(buf))) {
						if (len > 0)
							signature.update(buf, 0, len);
					}
				} finally {
					contentIn.close();
				}

				if (signature.verify())
					return;

			} catch (Exception e) {
				throw new PGPVerifyException(file.getAbsolutePath() + ": " + e, e);
			}
		}
		throw new PGPVerifyException(file.getAbsolutePath());
	}

	private PGPPublicKeyRingCollection getPublicKeyRingWithTrustedKeys() {
		try {
			PGPPublicKeyRingCollection ring = publicKeyRingWithTrustedKeys;
			if (ring == null) {
				// Currently only one single trusted key ;-)
				final InputStream publicKeyIn = new BufferedInputStream(PGPVerifier.class.getResourceAsStream("/0x4AB0FBC1.asc"));
				try {
					ring = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(publicKeyIn));
				} finally {
					publicKeyIn.close();
				}
				publicKeyRingWithTrustedKeys = ring;
			}
			return ring;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	private PGPSignatureList readSignatureFile(final File signatureFile) throws PGPVerifyException {
		assertNotNull("signatureFile", signatureFile);
		if (!signatureFile.isFile() || !signatureFile.canRead())
			throw new PGPVerifyException("The signature-file does not exist or is not readable: " + signatureFile.getAbsolutePath());

		try {
			InputStream in = new BufferedInputStream(new FileInputStream(signatureFile));
			try {
				final PGPObjectFactory objectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(in));
				final PGPSignatureList sl = (PGPSignatureList) objectFactory.nextObject();
				return sl;
			} finally {
				in.close();
			}
		} catch (Exception e) {
			throw new PGPVerifyException(signatureFile.getAbsolutePath() + ": " + e, e);
		}
	}
}
