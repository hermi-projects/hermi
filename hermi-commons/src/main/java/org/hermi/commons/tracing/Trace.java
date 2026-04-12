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
public @interface Trace {
  /**
   * The business event name (e.g., "SaveUser"). If set, it will propagate to all nested traced
   * calls.
   */
  String event() default "";

  /** The default log level for this trace. Default is "INFO". */
  String severity() default "INFO";

  /** If true, the method arguments will not be logged. Default is false. */
  boolean excludeArgs() default false;

  /** If true, the method return value will not be logged. Default is false. */
  boolean excludeResult() default false;

  /**
   * The threshold in milliseconds for a "slow" execution. If exceeded, the log severity can be
   * upgraded. Default is -1 (disabled).
   */
  long slowThresholdMs() default -1;
}
