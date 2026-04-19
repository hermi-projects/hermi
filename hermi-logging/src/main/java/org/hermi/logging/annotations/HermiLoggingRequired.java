package org.hermi.logging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the annotated field is required when a log event is started, succeeded, or results
 * in a warning.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface HermiLoggingRequired {}
