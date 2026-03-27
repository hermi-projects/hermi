package org.hermi.usecase.commons.conversion;

/**
 * Interface for objects that can be converted to another type.
 *
 * @param <T> target type
 */
public interface Convertible<T> {

  /**
   * Converts to target type.
   *
   * @return target object
   */
  T convert();
}
