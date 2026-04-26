package org.hermi.shell;

import java.util.UUID;

/**
 * Generic interface for auditing external interactions. Separating auditing from protocol execution
 * follows the "Same Reason to Change" principle.
 *
 * @param <I> Input type (Request or Message)
 * @param <O> Output type (Response or Result)
 */
public interface Auditor<I, O> {

  /**
   * Saves the input before execution and returns a unique tracking identifier.
   *
   * @param input the input data
   * @return a unique ID for tracking the interaction
   */
  UUID save(I input);

  /**
   * Saves the output after execution.
   *
   * @param trackingId the unique ID returned by save
   * @param output the resulting output data
   */
  void save(UUID trackingId, O output);

  /**
   * Saves the exception after execution.
   *
   * @param trackingId the unique ID returned by save
   * @param e the exception
   */
  void save(UUID trackingId, Exception e);
}
