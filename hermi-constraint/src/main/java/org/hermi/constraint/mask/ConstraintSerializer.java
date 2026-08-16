package org.hermi.constraint.mask;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.lang.annotation.Annotation;

/**
 * Base class for masking serializers. Extends Jackson's {@link StdSerializer} with a typed
 * constructor so subclasses only need to provide their serialization logic.
 *
 * @param <T> the type this serializer handles
 */
public abstract class ConstraintSerializer<A extends Annotation, T> extends StdSerializer<T> {

  protected ConstraintSerializer(Class<T> t) {
    super(t);
  }

  public void initialize(A constraintAnnotation) {}
}
