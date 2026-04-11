package org.hermi.commons.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

public class InputValidationException extends ConstraintViolationException {
  public InputValidationException(
      String message, Set<? extends ConstraintViolation<?>> constraintViolations) {
    super(message, constraintViolations);
  }
}
