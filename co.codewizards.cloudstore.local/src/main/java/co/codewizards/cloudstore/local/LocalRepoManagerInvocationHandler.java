package co.codewizards.cloudstore.local;

import static java.util.Objects.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerCloseEvent;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerCloseListener;

class LocalRepoManagerInvocationHandler implements InvocationHandler {
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoManagerInvocationHandler.class);

	final LocalRepoManagerImpl localRepoManagerImpl; // package-protected for our test
	private final AtomicBoolean open = new AtomicBoolean(true);
	private final List<LocalRepoManagerCloseListener> localRepoManagerCloseListeners = new CopyOnWriteArrayList<LocalRepoManagerCloseListener>();
	private volatile Throwable proxyCreatedStackTraceException = new Exception("proxyCreatedStackTraceException").fillInStackTrace();

	private static final Set<String> methodsAllowedOnClosedProxy = new HashSet<String>(Arrays.asList(
			"close",
			"finalize",
			"isOpen",
			"equals",
			"hashCode",
			"toString"));

	public LocalRepoManagerInvocationHandler(final LocalRepoManagerImpl localRepoManagerImpl) {
		this.localRepoManagerImpl = requireNonNull(localRepoManagerImpl, "localRepoManagerImpl");
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		final LocalRepoManager localRepoManagerProxy = (LocalRepoManager) proxy;
		boolean proxyClosedInThisInvocation = false;

		if (!methodsAllowedOnClosedProxy.contains(method.getName()))
			assertOpen();

		if ("close".equals(method.getName())) {
			proxyClosedInThisInvocation = close(localRepoManagerProxy, method, args);
			if (!proxyClosedInThisInvocation) // Multiple invocations of close() should have no effect.
				return null;
		}
		else if ("isOpen".equals(method.getName()))
			return isOpen(localRepoManagerProxy, method, args); // Do *not* delegate.
		else if ("addLocalRepoManagerCloseListener".equals(method.getName()))
			addLocalRepoManagerCloseListener(localRepoManagerProxy, method, args);
		else if ("removeLocalRepoManagerCloseListener".equals(method.getName()))
			removeLocalRepoManagerCloseListener(localRepoManagerProxy, method, args);
		else if ("finalize".equals(method.getName())) {
			finalize(localRepoManagerProxy, method, args);
			return null; // NEVER delegating finalize
		}

		final Object result = method.invoke(localRepoManagerImpl, args);

		if (proxyClosedInThisInvocation)
			firePostClose(localRepoManagerProxy);

		return result;
	}

	private void assertOpen() {
		if (!open.get()) {
			throw new IllegalStateException("This LocalRepoManager (proxy) is already closed!");
		}
	}

	private boolean close(final LocalRepoManager localRepoManagerProxy, final Method method, final Object[] args) {
		proxyCreatedStackTraceException = null;
		if (open.compareAndSet(true, false)) {
			firePreClose(localRepoManagerProxy);
			return true;
		}
		return false;
	}

	private void finalize(final LocalRepoManager localRepoManagerProxy, final Method method, final Object[] args) {
		if (proxyCreatedStackTraceException != null) {
			logger.warn("finalize: Detected forgotten close() invocation!", proxyCreatedStackTraceException);
		}
		close(localRepoManagerProxy, method, args);
	}

	private void firePreClose(final LocalRepoManager localRepoManagerProxy) {
		final LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(localRepoManagerProxy, localRepoManagerProxy, false);
		for (final LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.preClose(event);
		}
	}

	private void firePostClose(final LocalRepoManager localRepoManagerProxy) {
		final LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(localRepoManagerProxy, localRepoManagerProxy, false);
		for (final LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.postClose(event);
		}
	}

	private boolean isOpen(final LocalRepoManager localRepoManagerProxy, final Method method, final Object[] args) {
		return open.get();
	}

	private void addLocalRepoManagerCloseListener(final LocalRepoManager localRepoManagerProxy, final Method method, final Object[] args) {
		if (args == null || args.length != 1)
			throw new IllegalArgumentException("args == null || args.length != 1");

		if (!(args[0] instanceof LocalRepoManagerCloseListener))
			throw new IllegalArgumentException("args[0] is not an instance of LocalRepoManagerCloseListener");

		localRepoManagerCloseListeners.add((LocalRepoManagerCloseListener) args[0]);
	}

	private void removeLocalRepoManagerCloseListener(final LocalRepoManager localRepoManagerProxy, final Method method, final Object[] args) {
		if (args == null || args.length != 1)
			throw new IllegalArgumentException("args == null || args.length != 1");

		if (!(args[0] instanceof LocalRepoManagerCloseListener))
			throw new IllegalArgumentException("args[0] is not an instance of LocalRepoManagerCloseListener");

		localRepoManagerCloseListeners.remove(args[0]);
	}
}
