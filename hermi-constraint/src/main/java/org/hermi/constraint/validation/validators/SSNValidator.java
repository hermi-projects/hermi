package org.hermi.constraint.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.hermi.constraint.annotations.SSN;

/** Validator for {@link SSN} constraint. */
public final class SSNValidator implements ConstraintValidator<SSN, String> {

  /** Compiled regex pattern. */
  private Pattern pattern;

  /**
   * Initializes the validator.
   *
   * @param ssn annotation to use for configuration
   */
  @Override
  public void initialize(final SSN ssn) {
    this.pattern = Pattern.compile("^(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-" + "(?!0{4})\\d{4}$");
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    return this.pattern.matcher(value).matches();
  }
}
