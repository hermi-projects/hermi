package org.hermi.logging.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class or method for structured execution tracing.
 *
 * <p>When applied, the execution flow (entry, exit, and errors) will be automatically logged with
 * structured metadata to assist in understanding and debugging the system.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface HermiLogging {
  /**
   * The business event name (e.g., "SaveUser"). If set, it will propagate to all nested traced
   * calls.
   */
  String event() default "";

  /** The default log level for this trace. Default is "INFO". */
  String severity() default "INFO";

  /** The maximum length of the result to log. Use < 0 to exclude the result. Default is 20. */
  int maxResultLength() default 20;

  /**
   * The threshold in milliseconds for a "slow" execution. If exceeded, the log severity can be
   * upgraded. Default is -1 (disabled).
   */
  long slowThresholdMs() default -1;

  /** The maximum length of arguments to log. Use < 0 to exclude arguments. Default is 20. */
  int maxArgLength() default 20;
}
