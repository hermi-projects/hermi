package org.hermi.usecase.commons.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.usecase.commons.validation.constraintvalidators.SSNValidator;

/** Social Security Number validation constraint. */
@Documented
@Constraint(validatedBy = {SSNValidator.class})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SSN {

  /**
   * Error message.
   *
   * @return message
   */
  String message() default "Not valid SSN.";

  /**
   * Constraint groups.
   *
   * @return groups
   */
  Class<?>[] groups() default {};

  /**
   * Payload.
   *
   * @return payload
   */
  Class<? extends Payload>[] payload() default {};

  /**
   * Regex pattern.
   *
   * @return pattern
   */
  String pattern() default "^(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-" + "(?!0{4})\\d{4}$";
}
