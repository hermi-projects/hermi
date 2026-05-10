package org.hermi.constraint.mask;

import tools.jackson.databind.ser.std.StdSerializer;

public abstract class ConstraintSerializer<T> extends StdSerializer<T> {

  protected ConstraintSerializer(Class<T> t) {
    super(t);
  }
}
