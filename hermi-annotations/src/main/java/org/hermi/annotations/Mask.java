package org.hermi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.annotations.serializers.MaskSerializer;

@org.hermi.annotations.mask.Constraint(maskedBy = MaskSerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {}
