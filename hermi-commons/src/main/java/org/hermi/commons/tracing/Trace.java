package org.hermi.commons.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class or method for structured execution logging.
 *
 * <p>When applied, the execution flow (entry, exit, and errors) will be automatically logged with
 * structured metadata to assist in understanding and debugging the system.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Trace {}
