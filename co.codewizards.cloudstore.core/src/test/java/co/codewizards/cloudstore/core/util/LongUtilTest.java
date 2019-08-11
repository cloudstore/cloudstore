package co.codewizards.cloudstore.core.util;

import static org.assertj.core.api.Assertions.*;

import java.security.SecureRandom;
import java.util.Random;

import org.junit.Test;

public class LongUtilTest {

	private final Random random = new SecureRandom();

	@Test
	public void multipleRandomValuesToBytesAndBack() {
		for (int i = 0; i < 10000; ++i) {
			long value = random.nextLong();
			byte[] bytes = LongUtil.toBytes(value);
			long backValue = LongUtil.fromBytes(bytes);
			assertThat(backValue).isEqualTo(value);
		}
	}

	@Test
	public void multipleRandomValuesToBytesHexAndBack() {
		for (int i = 0; i < 10000; ++i) {
			long value = random.nextLong();
			String[] bytesHex = LongUtil.toBytesHex(value, (i % 2) == 0);
			long backValue = LongUtil.fromBytesHex(bytesHex);
			assertThat(backValue).isEqualTo(value);
		}
	}
}
