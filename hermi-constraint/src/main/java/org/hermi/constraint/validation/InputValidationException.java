package org.hermi.constraint.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * Exception thrown when input validation fails. Wraps Jakarta {@link ConstraintViolationException}
 * with a message and the set of constraint violations.
 */
public class InputValidationException extends ConstraintViolationException {
  public InputValidationException(
      String message, Set<? extends ConstraintViolation<?>> constraintViolations) {
    super(message, constraintViolations);
  }
}
