package org.hermi.constraint.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.constraint.mask.serializers.SSNSerializer;
import org.hermi.constraint.validation.validators.SSNValidator;

@Documented
@jakarta.validation.Constraint(validatedBy = SSNValidator.class)
@org.hermi.constraint.mask.Constraint(maskedBy = SSNSerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SSN {}
