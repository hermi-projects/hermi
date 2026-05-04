package org.hermi.commons.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import org.hermi.annotations.logging.Constraint;
import org.hermi.annotations.logging.ConstraintMarker;

/**
 * Thread-safe reflection cache for field discovery and annotation-based field masking.
 *
 * <p>Field metadata and {@link ConstraintMarker} instances are cached in LRU maps (max 1024 entries
 * each) backed by {@link Collections#synchronizedMap} — safe for concurrent use across executor
 * threads.
 */
public class ReflectionService {
  private static final int MAX_CACHE_SIZE = 1024;
  private final Map<Class<?>, List<Field>> fieldCache = createLRUMap(MAX_CACHE_SIZE);
  private final Map<MarkerKey, ConstraintMarker<Annotation, Object>> markerCache =
      createLRUMap(MAX_CACHE_SIZE);

  private ReflectionService() {}

  public static ReflectionService getInstance() {
    return Holder.INSTANCE;
  }

  private static class Holder {
    private static final ReflectionService INSTANCE = new ReflectionService();
  }

  private record MarkerKey(
      Class<? extends ConstraintMarker<?, ?>> markerClass, Annotation annotation) {}

  public List<Field> getFields(Class<?> clazz) {
    Class<?> actualClass = getActualClass(clazz);
    synchronized (fieldCache) {
      List<Field> fields = fieldCache.get(actualClass);
      if (fields == null) {
        fields = loadFields(actualClass);
        fieldCache.put(actualClass, fields);
      }
      return fields;
    }
  }

  @SuppressWarnings("unchecked")
  public Object applyMarker(Field field, Object value) {
    if (value == null) return null;

    for (Annotation ann : field.getAnnotations()) {
      Constraint c = ann.annotationType().getAnnotation(Constraint.class);
      if (c == null) continue;

      MarkerKey key = new MarkerKey(c.markedBy(), ann);

      synchronized (markerCache) {
        ConstraintMarker<Annotation, Object> marker = markerCache.get(key);
        if (marker == null) {
          try {
            marker =
                (ConstraintMarker<Annotation, Object>)
                    key.markerClass().getDeclaredConstructor().newInstance();
            marker.initialize(key.annotation());
            markerCache.put(key, marker);
          } catch (Exception e) {
            return null;
          }
        }
        return marker.mark(value);
      }
    }
    return null;
  }

  private List<Field> loadFields(Class<?> clazz) {
    List<Field> allFields = new ArrayList<>();
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      try {
        for (Field f : current.getDeclaredFields()) {
          f.setAccessible(true);
          allFields.add(f);
        }
      } catch (Throwable ignored) {
      }
      current = current.getSuperclass();
    }
    return Collections.unmodifiableList(allFields);
  }

  private Class<?> getActualClass(Class<?> clazz) {
    if (clazz == null) return null;
    String name = clazz.getName();

    if (name.contains("$$")) {
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && superclass != Object.class) return superclass;
    }

    if (Proxy.isProxyClass(clazz)) {
      Class<?>[] interfaces = clazz.getInterfaces();
      for (Class<?> iface : interfaces) {
        if (!iface.getName().startsWith("java.") && !iface.getName().startsWith("javax.")) {
          return iface;
        }
      }
    }
    return clazz;
  }

  private <K, V> Map<K, V> createLRUMap(int size) {
    return Collections.synchronizedMap(
        new LinkedHashMap<K, V>(size, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > size;
          }
        });
  }
}
