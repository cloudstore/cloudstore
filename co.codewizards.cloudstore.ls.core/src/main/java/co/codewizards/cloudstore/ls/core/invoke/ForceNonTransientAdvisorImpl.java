package co.codewizards.cloudstore.ls.core.invoke;

import java.util.EventObject;

public class ForceNonTransientAdvisorImpl implements ForceNonTransientAdvisor {

	@Override
	public Class<?>[] getForceNonTransientClasses() {
		return new Class<?>[] {
				EventObject.class // we need the source!
		};
	}

}
