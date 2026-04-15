package org.hermi.logging.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a parameter or field as sensitive.
 *
 * <p>When applied, the value will be masked in trace logs to prevent accidental exposure of
 * sensitive information.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HermiSensitive {}
