package co.codewizards.cloudstore.ls.core.invoke.refjanitor;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public class ReferenceJanitorRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ReferenceJanitorRegistry.class);

	private final ObjectManager objectManager;
	private final List<ReferenceJanitor> referenceJanitors;

	public ReferenceJanitorRegistry(final ObjectManager objectManager) {
		this.objectManager = assertNotNull("objectManager", objectManager);
		referenceJanitors = loadReferenceJanitors();
	}

	private List<ReferenceJanitor> loadReferenceJanitors() {
		final ArrayList<ReferenceJanitor> result = new ArrayList<>();

		final Iterator<ReferenceJanitor> it = ServiceLoader.load(ReferenceJanitor.class).iterator();
		while (it.hasNext())
			result.add(it.next());

		Collections.sort(result, new Comparator<ReferenceJanitor>() {
			@Override
			public int compare(ReferenceJanitor o1, ReferenceJanitor o2) {
				int result = -1 * Integer.compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});

		result.trimToSize();
		return result;
	}

	public void cleanUp() {
		for (final ReferenceJanitor referenceJanitor : referenceJanitors) {
			try {
				referenceJanitor.cleanUp();
			} catch (Exception x) {
				logger.error("cleanUp: " + x, x);
			}
		}
	}

	public void preInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		for (final ReferenceJanitor referenceJanitor : referenceJanitors) {
			try {
				referenceJanitor.preInvoke(extMethodInvocationRequest);
			} catch (Exception x) {
				logger.error("preInvoke: " + x, x);
			}
		}
	}

	public void postInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest, final Object resultObject, final Throwable error) {
		for (final ReferenceJanitor referenceJanitor : referenceJanitors) {
			try {
				referenceJanitor.postInvoke(extMethodInvocationRequest, resultObject, error);
			} catch (Exception x) {
				logger.error("preInvoke: " + x, x);
			}
		}
	}
}
