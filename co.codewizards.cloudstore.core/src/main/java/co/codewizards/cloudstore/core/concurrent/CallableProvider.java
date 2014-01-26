package co.codewizards.cloudstore.core.concurrent;

import java.util.concurrent.Callable;

public interface CallableProvider<V> {
	Callable<V> getCallable();
}
