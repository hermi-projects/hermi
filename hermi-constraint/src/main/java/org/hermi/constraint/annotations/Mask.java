package org.hermi.constraint.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.constraint.mask.Constraint;
import org.hermi.constraint.mask.serializers.MaskSerializer;

/**
 * Marks a field or parameter for generic masking. The value is serialized with the first and last
 * character preserved and interior characters replaced with {@code *}. Delegates to {@link
 * MaskSerializer} via the {@link org.hermi.constraint.mask.Constraint} meta-annotation.
 */
@Constraint(maskedBy = MaskSerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {}
