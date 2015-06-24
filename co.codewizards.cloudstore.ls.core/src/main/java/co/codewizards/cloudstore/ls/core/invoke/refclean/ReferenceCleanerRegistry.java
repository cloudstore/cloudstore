package co.codewizards.cloudstore.ls.core.invoke.refclean;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public class ReferenceCleanerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ReferenceCleanerRegistry.class);

	private final ObjectManager objectManager;
	private final List<ReferenceCleaner> referenceCleaners;

	public ReferenceCleanerRegistry(final ObjectManager objectManager) {
		this.objectManager = assertNotNull("objectManager", objectManager);
		referenceCleaners = loadReferenceCleaners();
	}

	private List<ReferenceCleaner> loadReferenceCleaners() {
		final ArrayList<ReferenceCleaner> result = new ArrayList<>();

		final Iterator<ReferenceCleaner> it = ServiceLoader.load(ReferenceCleaner.class).iterator();
		while (it.hasNext())
			result.add(it.next());

		result.trimToSize();
		return result;
	}

	public void cleanUp() {
		for (final ReferenceCleaner referenceCleaner : referenceCleaners) {
			try {
				referenceCleaner.cleanUp();
			} catch (Exception x) {
				logger.error("cleanUp: " + x, x);
			}
		}
	}

	public void preInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		for (final ReferenceCleaner referenceCleaner : referenceCleaners) {
			try {
				referenceCleaner.preInvoke(extMethodInvocationRequest);
			} catch (Exception x) {
				logger.error("preInvoke: " + x, x);
			}
		}
	}

	public void postInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest, final Object resultObject, final Throwable error) {
		for (final ReferenceCleaner referenceCleaner : referenceCleaners) {
			try {
				referenceCleaner.postInvoke(extMethodInvocationRequest, resultObject, error);
			} catch (Exception x) {
				logger.error("preInvoke: " + x, x);
			}
		}
	}
}
