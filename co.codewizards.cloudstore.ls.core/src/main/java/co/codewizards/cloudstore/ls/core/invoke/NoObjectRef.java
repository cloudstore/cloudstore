package co.codewizards.cloudstore.ls.core.invoke;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface NoObjectRef {

	boolean value() default true;

	boolean inheritToObjectGraphChildren() default true;

	public static NoObjectRef DEFAULT_IF_MISSING = new NoObjectRef() {
		@Override
		public Class<? extends Annotation> annotationType() {
			return NoObjectRef.class;
		}

		@Override
		public boolean value() {
			return false;
		}

		@Override
		public boolean inheritToObjectGraphChildren() {
			return true;
		}
	};
}
