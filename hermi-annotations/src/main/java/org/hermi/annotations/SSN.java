package org.hermi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.annotations.markers.SSNMarker;
import org.hermi.annotations.validators.SSNValidator;

@Documented
@jakarta.validation.Constraint(validatedBy = SSNValidator.class)
@org.hermi.annotations.logging.Constraint(markedBy = SSNMarker.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SSN {}
