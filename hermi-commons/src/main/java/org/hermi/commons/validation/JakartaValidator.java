package org.hermi.commons.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Objects;
import java.util.Set;

public class JakartaValidator extends Validator {
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
        .addShutdownHook(new Thread(VALIDATOR_FACTORY::close, "ValidatorFactory-Shutdown-Hook"));
  }

  @Override
  public <T> void validate(T value) {
    Objects.requireNonNull(value);
    Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(
          String.format("%s is not valid", value.getClass().getSimpleName()), violations);
    }
  }
}
