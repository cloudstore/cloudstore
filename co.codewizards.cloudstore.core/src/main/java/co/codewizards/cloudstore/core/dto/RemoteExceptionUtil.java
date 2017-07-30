package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class RemoteExceptionUtil {

	private RemoteExceptionUtil() { }

//	public static void throwOriginalExceptionIfPossible(final Error error) {
//		Class<?> clazz;
//		try {
//			clazz = Class.forName(error.getClassName());
//		} catch (final ClassNotFoundException e) {
//			return;
//		}
//		if (!Throwable.class.isAssignableFrom(clazz))
//			return;
//
//		@SuppressWarnings("unchecked")
//		final Class<? extends Throwable> clasz = (Class<? extends Throwable>) clazz;
//
//		final RemoteException cause = new RemoteException(error);
//
//		Throwable throwable = null;
//
//		// trying XyzException(String message, Throwable cause)
//		if (throwable == null)
//			throwable = getObjectOrNull(clasz, new Class<?>[] { String.class, Throwable.class }, error.getMessage(), cause);
//
//		// trying XyzException(String message)
//		if (throwable == null)
//			throwable = getObjectOrNull(clasz, new Class<?>[] { String.class }, error.getMessage());
//
//		// trying XyzException(Throwable cause)
//		if (throwable == null)
//			throwable = getObjectOrNull(clasz, new Class<?>[] { Throwable.class }, cause);
//
//		// trying XyzException()
//		if (throwable == null)
//			throwable = getObjectOrNull(clasz, null);
//
//		if (throwable != null) {
//			try {
//				throwable.initCause(cause);
//			} catch (final Exception x) {
//				// This happens, if either the cause was already set in an appropriate constructor (see above)
//				// or the concrete Throwable does not support it. If we were unable to set the cause we want,
//				// we better use a RemoteException and not the original one.
//				if (throwable.getCause() != cause)
//					return;
//			}
//			if (throwable instanceof RuntimeException)
//				throw (RuntimeException) throwable;
//
//			if (throwable instanceof java.lang.Error)
//				throw (java.lang.Error) throwable;
//
//			throw new RuntimeException(throwable);
//		}
//	}

	public static void throwOriginalExceptionIfPossible(final Error error) {
		Throwable throwable = toThrowable(error);

		if (throwable instanceof RuntimeException)
			throw (RuntimeException) throwable;

		if (throwable instanceof java.lang.Error)
			throw (java.lang.Error) throwable;

		throw new RuntimeException(throwable);
	}

	private static Throwable toThrowable(Error error) {
		return toThrowable(error, false);
	}

	private static Throwable toThrowable(Error error, boolean nested) {
		if (error == null)
			return null;

		Throwable cause = toThrowable(error.getCause(), true);

		Class<?> clazz;
		try {
			clazz = Class.forName(error.getClassName());
		} catch (final ClassNotFoundException e) {
			return new RemoteException(error, true).initCause(cause);
		}
		if (!Throwable.class.isAssignableFrom(clazz))
			return new RemoteException(error, true).initCause(cause);

		@SuppressWarnings("unchecked")
		final Class<? extends Throwable> clasz = (Class<? extends Throwable>) clazz;

		Throwable throwable = null;

		// trying XyzException(String message, Throwable cause)
		if (throwable == null)
			throwable = getObjectOrNull(clasz, new Class<?>[] { String.class, Throwable.class }, error.getMessage(), cause);

		// trying XyzException(String message)
		if (throwable == null)
			throwable = getObjectOrNull(clasz, new Class<?>[] { String.class }, error.getMessage());

		// trying XyzException(Throwable cause)
		if (throwable == null)
			throwable = getObjectOrNull(clasz, new Class<?>[] { Throwable.class }, cause);

		// trying XyzException()
		if (throwable == null)
			throwable = getObjectOrNull(clasz, null);

		// LAST: falling back to RemoteException
		if (throwable == null)
			throwable = new RemoteException(error, true);

		try {
			throwable.initCause(cause);
		} catch (final Exception x) {
			// This happens, if either the cause was already set in an appropriate constructor (see above)
			// or the concrete Throwable does not support it. If we were unable to set the cause we want,
			// we better use a RemoteException and not the original one.
			if (throwable.getCause() != cause)
				return new RemoteException(error, true).initCause(cause);
		}
		initStackTrace(throwable, error, nested);
		return throwable;
	}

	private static void initStackTrace(Throwable throwable, Error error, boolean replaceOriginalStackTrace) {
		assertNotNull(throwable, "throwable");
		assertNotNull(error, "error");

		int idx = -1;
		StackTraceElement[] origStackTrace = replaceOriginalStackTrace ? new StackTraceElement[0] : throwable.getStackTrace();
		StackTraceElement[] stackTrace = new StackTraceElement[origStackTrace.length + error.getStackTraceElements().size()];

		for (ErrorStackTraceElement errorStackTraceElement : error.getStackTraceElements()) {
			stackTrace[++idx] = new StackTraceElement(
					errorStackTraceElement.getClassName(),
					errorStackTraceElement.getMethodName(),
					errorStackTraceElement.getFileName(),
					errorStackTraceElement.getLineNumber()
					);
		}

		for (StackTraceElement stackTraceElement : origStackTrace) {
			stackTrace[++idx] = stackTraceElement;
		}

		throwable.setStackTrace(stackTrace);
	}

	private static <T> T getObjectOrNull(final Class<T> clazz, Class<?>[] argumentTypes, final Object ... arguments) {
		T result = null;
		if (argumentTypes == null)
			argumentTypes = new Class<?> [0];

		if (argumentTypes.length == 0) {
			try {
				result = clazz.newInstance();
			} catch (final InstantiationException e) {
				return null;
			} catch (final IllegalAccessException e) {
				return null;
			}
		}

		if (result == null) {
			Constructor<T> constructor;
			try {
				constructor = clazz.getConstructor(argumentTypes);
			} catch (final NoSuchMethodException e) {
				return null;
			} catch (final SecurityException e) {
				return null;
			}

			try {
				result = constructor.newInstance(arguments);
			} catch (final InstantiationException e) {
				return null;
			} catch (final IllegalAccessException e) {
				return null;
			} catch (final IllegalArgumentException e) {
				return null;
			} catch (final InvocationTargetException e) {
				return null;
			}
		}

		return result;
	}
}
