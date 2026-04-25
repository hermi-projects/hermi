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

  I convertContext(C context);

  R convertResult(O output);
}
