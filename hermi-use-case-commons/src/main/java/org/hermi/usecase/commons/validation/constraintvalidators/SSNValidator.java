package org.hermi.usecase.commons.validation.constraintvalidators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.hermi.usecase.commons.validation.constraints.SSN;

/** Validator for {@link SSN} constraint. */
public final class SSNValidator implements ConstraintValidator<SSN, CharSequence> {

  /** Compiled regex pattern. */
  private Pattern pattern;

  /**
   * Initializes the validator.
   *
   * @param ssn annotation to use for configuration
   */
  @Override
  public void initialize(final SSN ssn) {
    this.pattern = Pattern.compile(ssn.pattern());
  }

  @Override
  public boolean isValid(final CharSequence value, final ConstraintValidatorContext context) {
    if (value == null || value.isEmpty() || value.toString().isBlank()) {
      return true;
    }
    return this.pattern.matcher(value).matches();
  }
}
