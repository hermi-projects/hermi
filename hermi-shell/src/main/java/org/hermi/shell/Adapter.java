package org.hermi.shell;

/**
 * Defines a contract for an adapter that bridges internal Core contexts with external Shell
 * technologies.
 *
 * @param <C> the internal Core context type
 * @param <R> the internal Core result type
 * @param <I> the external Shell input type (e.g. ApiRequest, JpaEntity)
 * @param <O> the external Shell output type (e.g. ApiResponse, JpaEntity)
 */
public interface Adapter<C, R, I, O> {

  /**
   * Converts the internal Core context into the external Shell input format.
   *
   * @param context the internal Core context object
   * @return the converted external Shell input type {@code I}
   */
  I convertContext(C context);

  /**
   * Processes the external input via the underlying infrastructure (Shell) and produces an external
   * output.
   *
   * @param input the external Shell input
   * @return the external Shell output {@code O}
   */
  O process(I input);

  /**
   * Converts the external Shell output back into the internal Core result type.
   *
   * @param output the external Shell output
   * @return the converted internal Core result
   */
  R convertResult(O output);
}
