package co.codewizards.cloudstore.core.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.Test;

public class UidTest {

	@Test
	public void toAndFromBytes() {
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
		for (int i = 0; i < 1000; ++i) {
			final Uid uid1 = new Uid();
			final String string1 = uid1.toString();
			final Uid uid2 = new Uid(string1);
			final String string2 = uid2.toString();

//			System.out.println(string1);

			assertThat(string2).isEqualTo(string1);
			assertThat(uid2).isEqualTo(uid1);
		}
	}

	@Test
	public void toAndFromStringSpecialValues() {
		final Uid uid1 = new Uid(0, 0);
		System.out.println(uid1);

		final Uid uid2 = new Uid(Long.MAX_VALUE, Long.MAX_VALUE);
		System.out.println(uid2);

		final Uid uid3 = new Uid(Long.MIN_VALUE, Long.MIN_VALUE);
		System.out.println(uid3);

		final Uid uid4 = new Uid(0, 1);
		System.out.println(uid4);

		final Uid uid5 = new Uid(1, 0);
		System.out.println(uid5);

		final Uid uid6 = new Uid(10000000, 0);
		System.out.println(uid6);
	}

	@Test
	public void toAndFromStringUUIDComparison() {
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
