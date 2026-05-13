package org.hermi.constraint.mask;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Base class for masking serializers. Extends Jackson's {@link StdSerializer} with a typed
 * constructor so subclasses only need to provide their serialization logic.
 *
 * @param <T> the type this serializer handles
 */
public abstract class ConstraintSerializer<T> extends StdSerializer<T> {

  protected ConstraintSerializer(Class<T> t) {
    super(t);
  }
}
