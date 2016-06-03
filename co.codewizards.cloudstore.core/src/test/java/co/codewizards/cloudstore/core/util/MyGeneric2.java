package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.lang.reflect.Type;

public abstract class MyGeneric2<A, B, C> {

	public final Type actualTypeArgumentA;
	public final Type actualTypeArgumentB;
	public final Type actualTypeArgumentC;

	public MyGeneric2() {
		final Type[] actualTypeArguments = resolveActualTypeArguments(MyGeneric2.class, this);
		actualTypeArgumentA = actualTypeArguments[0];
		actualTypeArgumentB = actualTypeArguments[1];
		actualTypeArgumentC = actualTypeArguments[2];
	}
}
