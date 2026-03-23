package org.hermi.commons.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

/** Utility for programmatic entity validation using Jakarta Validation. */
public abstract class Validator {

  /** Validator factory. */
  private static final ValidatorFactory VALIDATOR_FACTORY;

  /** Jakarta validator instance. */
  private static final jakarta.validation.Validator VALIDATOR;

  static {
    // Create factory once and keep it alive for usecase lifetime
    // Per Jakarta Validation spec, the factory should not be
    // closed immediately
    VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    VALIDATOR = VALIDATOR_FACTORY.getValidator();

    // Register shutdown hook to properly close factory on JVM shutdown
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (VALIDATOR_FACTORY != null) {
                    VALIDATOR_FACTORY.close();
                  }
                },
                "ValidatorFactory-Shutdown-Hook"));
  }

  /**
   * Validates the given data.
   *
   * @param <T> data type
   * @param data instance to validate
   */
  public static <T> void validate(final T data) {
    Set<ConstraintViolation<T>> result = VALIDATOR.validate(data);
    if (!result.isEmpty()) {
      throw new ConstraintViolationException(result);
    }
  }
}
