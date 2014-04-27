package co.codewizards.cloudstore.updater;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.junit.Test;

public class PGPTest {
	private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

	@Test
	public void verifyGoodSignature() throws Exception {
		final PGPPublicKeyRingCollection publicKeyRing = getPublicKeyRingWithTrustedKeys();

		final PGPSignatureList sl = readSignatureFile("/content1.sig");
		assertThat(sl.isEmpty()).isFalse();
		assertThat(sl.size()).isEqualTo(1);

		PGPSignature signature = sl.get(0);
		signature.initVerify(publicKeyRing.getPublicKey(signature.getKeyID()), PROVIDER);

		InputStream contentIn = PGPTest.class.getResourceAsStream("/content1");
		byte[] buf = new byte[4096];
		int len;
		while (0 <= (len = contentIn.read(buf))) {
			signature.update(buf, 0, len);
		}
		contentIn.close();
		assertThat(signature.verify()).isTrue();
	}

	@Test
	public void verifyBadSignature() throws Exception {
		final PGPPublicKeyRingCollection publicKeyRing = getPublicKeyRingWithTrustedKeys();

		final PGPSignatureList sl = readSignatureFile("/content1.sig");
		assertThat(sl.isEmpty()).isFalse();
		assertThat(sl.size()).isEqualTo(1);

		PGPSignature signature = sl.get(0);
		signature.initVerify(publicKeyRing.getPublicKey(signature.getKeyID()), PROVIDER);

		InputStream contentIn = PGPTest.class.getResourceAsStream("/content1");
		byte[] buf = new byte[4096];
		int len;
		while (0 <= (len = contentIn.read(buf))) {
			buf[0] = 0;
			signature.update(buf, 0, len);
		}
		contentIn.close();
		assertThat(signature.verify()).isFalse();
	}

	private PGPPublicKeyRingCollection getPublicKeyRingWithTrustedKeys() throws IOException, PGPException {
		// Currently only one single trusted key ;-)
		final InputStream publicKeyIn = PGPTest.class.getResourceAsStream("/0x4AB0FBC1.asc");
		final PGPPublicKeyRingCollection ring = new PGPPublicKeyRingCollection(
				PGPUtil.getDecoderStream(publicKeyIn));
		publicKeyIn.close();
		return ring;
	}

	private PGPSignatureList readSignatureFile(final String resourcePath) throws IOException {
		final InputStream signatureIn = PGPTest.class.getResourceAsStream(resourcePath);
		final PGPObjectFactory objectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(signatureIn));
		final PGPSignatureList sl = (PGPSignatureList) objectFactory.nextObject();
		signatureIn.close();
		return sl;
	}

}
