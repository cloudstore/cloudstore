package co.codewizards.cloudstore.core.dto;

import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UidTest {

	private static final Logger logger = LoggerFactory.getLogger(UidTest.class);

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}

	@Test
	public void toAndFromBytes() {
		logger.debug("[{}]toAndFromBytes: entered.", Integer.toHexString(identityHashCode(this)));
		for (int i = 0; i < 1000; ++i) {
			final Uid uid1 = new Uid();
			final byte[] bytes1 = uid1.toBytes();
			final Uid uid2 = new Uid(bytes1);
			final byte[] bytes2 = uid2.toBytes();

			assertThat(bytes2).isEqualTo(bytes1);
			assertThat(uid2).isEqualTo(uid1);
		}
	}

	@Test
	public void toAndFromString() {
		logger.debug("[{}]toAndFromString: entered.", Integer.toHexString(identityHashCode(this)));
		for (int i = 0; i < 1000; ++i) {
			final Uid uid1 = new Uid();
			final String string1 = uid1.toString();
			final Uid uid2 = new Uid(string1);
			final String string2 = uid2.toString();

//			System.out.println("toAndFromString: " + string1);

			assertThat(string2).isEqualTo(string1);
			assertThat(uid2).isEqualTo(uid1);
		}
	}

	@Test
	public void toAndFromString_static() {
		logger.debug("[{}]toAndFromString_static: entered.", Integer.toHexString(identityHashCode(this)));
		final String a = "azAZaaaaaaaa0123456789";
		final Uid uid1 = new Uid(a);
		final String string1 = uid1.toString();
		final Uid uid2 = new Uid(string1);
		final String string2 = uid2.toString();

//		System.out.println("toAndFromString_static: uid1=" + uid1);
//		System.out.println("toAndFromString_static: string1=" + string1);
//		System.out.println("toAndFromString_static: uid2=" + uid2);
//		System.out.println("toAndFromString_static: string2=" + string2);

		assertThat(string2).isEqualTo(string1);
		assertThat(uid2).isEqualTo(uid1);

		/* Taken from console out. If that changes, there would be mismappings
		/* of stored Uids on version upgrades. */
		final String expected = "azAZaaaaaaaa012345678w";
		assertThat(string1).isEqualTo(expected);
	}

	@Test
	public void toAndFromStringSpecialValues() {
		logger.debug("[{}]toAndFromStringSpecialValues: entered.", Integer.toHexString(identityHashCode(this)));
		final Uid uid1 = new Uid(0, 0);
		System.out.println(uid1);

		final Uid uid2 = new Uid(Long.MAX_VALUE, Long.MAX_VALUE);
//		System.out.println(uid2);

		final Uid uid3 = new Uid(Long.MIN_VALUE, Long.MIN_VALUE);
//		System.out.println(uid3);

		final Uid uid4 = new Uid(0, 1);
//		System.out.println(uid4);

		final Uid uid5 = new Uid(1, 0);
//		System.out.println(uid5);

		final Uid uid6 = new Uid(10000000, 0);
//		System.out.println(uid6);
	}

	@Test
	public void toAndFromStringUUIDComparison() {
		logger.debug("[{}]toAndFromStringUUIDComparison: entered.", Integer.toHexString(identityHashCode(this)));
		for (int i = 0; i < 1000; ++i) {
			final UUID uid1 = UUID.randomUUID();
			final String string1 = uid1.toString();
			final UUID uid2 = UUID.fromString(string1);
			final String string2 = uid2.toString();

//			System.out.println(string1);

			assertThat(string2).isEqualTo(string1);
			assertThat(uid2).isEqualTo(uid1);
		}
	}
}
