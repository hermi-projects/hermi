package org.hermi.usecase.util.conversion;

/**
 * Simple converter interface.
 *
 * @param <S> source type
 * @param <T> target type
 */
public interface Converter<S, T> {

  /**
   * Converts S to T.
   *
   * @param source source
   * @return target
   */
  T convert(S source);
}
