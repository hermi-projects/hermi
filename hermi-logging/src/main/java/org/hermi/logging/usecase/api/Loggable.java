package org.hermi.logging.usecase.api;

/**
 * Interface for objects that provide a customized summary for tracing purposes.
 *
 * <p>When an object implementing this interface is passed as a method argument or returned as a
 * result, the {@code HermiLoggingAspect} will use the string returned by {@code toTraceMessage()}
 * instead of the default {@code toString()}.
 */
public interface Loggable {

  /**
   * Returns a concise, trace-friendly representation of the object.
   *
   * @return A string summary suitable for inclusion in structured logs.
   */
  String toHermiLoggingString();
}
