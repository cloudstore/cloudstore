package co.codewizards.cloudstore.core.ls;

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
}
