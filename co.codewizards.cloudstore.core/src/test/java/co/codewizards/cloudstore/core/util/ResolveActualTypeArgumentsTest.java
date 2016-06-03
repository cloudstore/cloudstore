package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;

public class ResolveActualTypeArgumentsTest {

	@Test
	public void myGeneric1_1() {
		MyGeneric1_1 object = new MyGeneric1_1();
		assertThat(object.actualTypeArgument1).isEqualTo(String.class);
	}

	@Test
	public void myGeneric1_1_1() {
		MyGeneric1_1_1 object = new MyGeneric1_1_1();
		assertThat(object.actualTypeArgument1).isEqualTo(String.class);
	}

	@Test
	public void myGeneric2_1() {
		MyGeneric2_1 object = new MyGeneric2_1();
		assertThat(object.actualTypeArgumentA).isEqualTo(Long.class);
		assertThat(object.actualTypeArgumentB).isEqualTo(Boolean.class);
		assertThat(object.actualTypeArgumentC).isEqualTo(String.class);
	}

	@Test
	public void myGeneric2_2_1() {
		MyGeneric2_2_1 object = new MyGeneric2_2_1();
		assertThat(object.actualTypeArgumentA).isEqualTo(Date.class);
		assertThat(object.actualTypeArgumentB).isEqualTo(Boolean.class);
		assertThat(object.actualTypeArgumentC).isEqualTo(BigDecimal.class);
	}

	@Test
	public void myGeneric2_3() {
		MyGeneric2_3 object = new MyGeneric2_3();

		// All of them are unresolved (i.e. no class, but TypeVariable).
		assertThat(object.actualTypeArgumentA).isInstanceOf(TypeVariable.class);
		assertThat(object.actualTypeArgumentB).isInstanceOf(TypeVariable.class);
		assertThat(object.actualTypeArgumentC).isInstanceOf(TypeVariable.class);

		assertThat(((TypeVariable<?>) object.actualTypeArgumentA).getName()).isEqualTo("A");
		assertThat(((TypeVariable<?>) object.actualTypeArgumentB).getName()).isEqualTo("B");
		assertThat(((TypeVariable<?>) object.actualTypeArgumentC).getName()).isEqualTo("C");
	}

	@Test
	public void myGeneric3_1() {
		MyGeneric3_1 object = new MyGeneric3_1();
		Type[] typeArguments = resolveActualTypeArguments(MyGeneric3.class, object);

		assertThat(typeArguments[0]).isEqualTo(Integer.class);
		assertThat(typeArguments[1]).isEqualTo(String.class);
		assertThat(typeArguments[2]).isEqualTo(UUID.class);
	}

	@Test
	public void nonGenericBaseClass() {
		Long object = new Long(44454455566666L);
		Type[] typeArguments = resolveActualTypeArguments(Number.class, object);
		assertThat(typeArguments).isNotNull();
		assertThat(typeArguments.length).isEqualTo(0);
	}
}
