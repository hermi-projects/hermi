package org.hermi.test;
import org.hermi.test.boundaries.BooleanBoundary;
import org.hermi.test.boundaries.IntegerBoundary;
import org.hermi.test.boundaries.StringBoundary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoundaryRegistry {

  private static final Map<Class<?>, Boundary<?>> REGISTRY = new HashMap<>();

  static {
    register(String.class, String.class, new StringBoundary());

    register(Integer.class, int.class, new IntegerBoundary());

    register(Boolean.class, boolean.class, new BooleanBoundary());
  }

  private static <T> void register(Class<?> boxed, Class<?> primitive, Boundary<T> boundary) {
    REGISTRY.put(boxed, boundary);
    REGISTRY.put(primitive, boundary);
  }

  @SuppressWarnings("unchecked")
  public static <T> Boundary<T> boundaryFor(Class<T> type) {
    Boundary<?> boundary = REGISTRY.get(type);
    if (boundary == null) {
      return (Boundary<T>) new Boundary<Object>() {
        @Override
        public Object validValue() { return null; }
        @Override
        public List<Object> invalidValues() { return List.of(); }
      };
    }
    return (Boundary<T>) boundary;
  }
}
