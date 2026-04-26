package org.hermi.shell;

/**
 * Interface for semantic translation between Core and Shell domains.
 *
 * <p><b>AI-Friendly Design (Naming Prophecy)</b>: Implementations should follow the naming pattern:
 * {Vendor}{Action}{Resource}Mapper. Separating mapping from IO logic solves AI context limit and
 * prevents over-editing.
 *
 * <p><b>Implementation Tip</b>: Use MapStruct for complex mappings to maintain declarative
 * integrity.
 *
 * @param <C> Core Context type
 * @param <R> Core Result type
 * @param <I> Infrastructure (Vendor) Input type
 * @param <O> Infrastructure (Vendor) Output type
 */
public interface Mapper<C, R, I, O> {

  /**
   * Translates the pure domain Context object into the Vendor-specific Input payload.
   *
   * @param context the domain context defined securely within the Use Case layer
   * @return the vendor-specific input payload ready for transmission
   */
  I convertContext(C context);

  /**
   * Translates the Vendor-specific Output payload back into the pure domain Result object.
   *
   * @param output the native response or event received from the external system
   * @return the domain result interpreted from the vendor payload
   */
  R convertResult(O output);
}
