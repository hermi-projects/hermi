package org.hermi.logging.commons;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import org.hermi.annotations.logging.Constraint;
import org.hermi.annotations.logging.ConstraintMarker;

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

  // --- 缓存键定义 ---
  private record MarkerKey(
      Class<? extends ConstraintMarker<?, ?>> markerClass, Annotation annotation) {}

  /** 加固后的获取字段方法 */
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

  /** 修正笔误后的 Marker 应用方法 */
  @SuppressWarnings("unchecked")
  public Object applyMarker(Field field, Object value) {
    if (value == null) return null;

    for (Annotation ann : field.getAnnotations()) {
      Constraint c = ann.annotationType().getAnnotation(Constraint.class);
      if (c == null) continue;

      // 1. 创建复合 Key
      MarkerKey key = new MarkerKey(c.markedBy(), ann);

      // 2. 从缓存中获取或创建实例
      synchronized (markerCache) {
        ConstraintMarker<Annotation, Object> marker = markerCache.get(key);
        if (marker == null) {
          try {
            // 使用 key.markerClass() 获取 Record 中的 Class 对象
            marker =
                (ConstraintMarker<Annotation, Object>)
                    key.markerClass().getDeclaredConstructor().newInstance();
            // 使用 key.annotation() 获取 Record 中的注解实例
            marker.initialize(key.annotation());
            markerCache.put(key, marker);
          } catch (Exception e) {
            return null;
          }
        }
        // 3. 执行无状态的标记动作
        return marker.mark(value);
      }
    }
    return null;
  }

  // --- 内部辅助方法 ---

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

    // 1. CGLIB 识别
    if (name.contains("$$")) {
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && superclass != Object.class) return superclass;
    }

    // 2. JDK 代理识别：尝试获取业务接口
    if (Proxy.isProxyClass(clazz)) {
      Class<?>[] interfaces = clazz.getInterfaces();
      for (Class<?> iface : interfaces) {
        // 过滤掉 java.io, java.lang 等系统接口，取第一个业务接口
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
