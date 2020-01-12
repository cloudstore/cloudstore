open module co.codewizards.cloudstore.ls.core {

	requires transitive java.ws.rs;

	requires transitive co.codewizards.cloudstore.core;

	requires transitive jersey.client;
	requires transitive jersey.common;

	uses co.codewizards.cloudstore.ls.core.invoke.ForceNonTransientAdvisor;
	uses co.codewizards.cloudstore.ls.core.invoke.filter.InvocationFilter;
	uses co.codewizards.cloudstore.ls.core.invoke.refjanitor.ReferenceJanitor;

	provides co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContextProvider
		with co.codewizards.cloudstore.ls.core.dto.jaxb.CloudStoreJaxbContextProviderImpl;

	provides co.codewizards.cloudstore.ls.core.invoke.filter.InvocationFilter
		with
			co.codewizards.cloudstore.ls.core.invoke.filter.AllowCloudStoreInvocationFilter,
			co.codewizards.cloudstore.ls.core.invoke.filter.AllowJavaInvocationFilter,
			co.codewizards.cloudstore.ls.core.invoke.filter.DenyReflectInvocationFilter;

	provides co.codewizards.cloudstore.ls.core.invoke.ForceNonTransientAdvisor
		with co.codewizards.cloudstore.ls.core.invoke.ForceNonTransientAdvisorImpl;

	provides co.codewizards.cloudstore.ls.core.invoke.refjanitor.ReferenceJanitor
		with co.codewizards.cloudstore.ls.core.invoke.refjanitor.PropertyChangeListenerJanitor;

	exports co.codewizards.cloudstore.ls.core;
	exports co.codewizards.cloudstore.ls.core.dto;
	exports co.codewizards.cloudstore.ls.core.dto.jaxb;
	exports co.codewizards.cloudstore.ls.core.invoke;
	exports co.codewizards.cloudstore.ls.core.invoke.filter;
	exports co.codewizards.cloudstore.ls.core.invoke.refjanitor;
	exports co.codewizards.cloudstore.ls.core.provider;
}