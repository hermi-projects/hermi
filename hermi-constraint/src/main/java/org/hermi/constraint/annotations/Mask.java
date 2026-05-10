package org.hermi.constraint.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.constraint.mask.serializers.MaskSerializer;

@org.hermi.constraint.mask.Constraint(maskedBy = MaskSerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {}
