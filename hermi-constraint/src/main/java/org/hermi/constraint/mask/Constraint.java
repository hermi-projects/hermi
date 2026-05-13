package org.hermi.constraint.mask;

import com.fasterxml.jackson.databind.JsonSerializer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that marks an annotation as a masking constraint. Annotations meta-annotated with
 * {@code @Constraint} are discovered by {@link MaskModule}, which replaces the annotated property's
 * serializer with the one returned by {@link #maskedBy()}.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Constraint {
  /**
   * The serializer class to use when masking values annotated with the owning annotation. Must have
   * a public no-arg constructor.
   */
  Class<? extends JsonSerializer<?>> maskedBy();
}
