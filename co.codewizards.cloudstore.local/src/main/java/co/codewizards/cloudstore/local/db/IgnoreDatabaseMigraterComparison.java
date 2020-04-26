package co.codewizards.cloudstore.local.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Getter-method annotated with this is ignored by
 * {@link DatabaseMigrater#comparePersistentObject(co.codewizards.cloudstore.local.persistence.Entity, co.codewizards.cloudstore.local.persistence.Entity)}.
 * @author mangu
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreDatabaseMigraterComparison {

}
