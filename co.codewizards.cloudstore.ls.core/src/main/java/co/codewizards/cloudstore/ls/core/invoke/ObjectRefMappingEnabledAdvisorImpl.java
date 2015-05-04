//package co.codewizards.cloudstore.ls.core.invoke;
//
//import java.lang.reflect.Proxy;
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.UUID;
//
//import co.codewizards.cloudstore.core.dto.Uid;
//
//public class ObjectRefMappingEnabledAdvisorImpl implements ObjectRefMappingEnabledAdvisor {
//
//	private static final Set<Class<?>> mappingDisabledClasses = new HashSet<Class<?>>(
//			Arrays.asList(new Class<?>[] {
//					Byte.class,
//					Short.class,
//					Integer.class,
//					Long.class,
//					Float.class,
//					Double.class,
//					Character.class,
//					Boolean.class,
//					BigDecimal.class,
//					BigInteger.class,
//					UUID.class,
//					Uid.class,
//					String.class
//			}));
//
//	@Override
//	public int getPriority() {
//		return 0;
//	}
//
//	@Override
//	public Boolean isObjectRefMappingEnabled(final Object object) {
//		if (object == null)
//			return false;
//
//		if (object instanceof ObjectRef)
//			return false;
//
//		final Class<? extends Object> clazz = getClassOrArrayComponentType(object);
//
//		if (Proxy.isProxyClass(clazz))
//			return true;
//
//		if (ClassManager.getPrimitiveClasses().contains(clazz))
//			return false;
//
//		if (mappingDisabledClasses.contains(clazz))
//			return false;
//
//		for (final Class<?> mdc : mappingDisabledClasses) {
//			if (mdc.isAssignableFrom(clazz))
//				return false;
//		}
//
//		if (isDto(clazz))
//			return false;
//
//		return null;
//	}
//
//	private Class<?> getClassOrArrayComponentType(final Object object) {
//		final Class<? extends Object> clazz = object.getClass();
//		if (clazz.isArray())
//			return clazz.getComponentType();
//		else
//			return clazz;
//	}
//
//	private boolean isDto(Class<?> clazz) {
//		final String className = clazz.getName();
//		if (className.endsWith("Dto") || className.contains(".dto."))
//			return true;
//		else
//			return false;
//	}
//
//}
