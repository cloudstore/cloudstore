package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.lang.reflect.Type;

public abstract class MyGeneric1<T> {

	public final Class<T> actualTypeArgument1;

	public MyGeneric1() {
		final Type[] actualTypeArguments = resolveActualTypeArguments(MyGeneric1.class, this);
		actualTypeArgument1 = (Class<T>) actualTypeArguments[0];
	}
}
