package org.hermi.commons.mask;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.hermi.annotations.mask.Constraint;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

public class MaskModule extends SimpleModule {
  private static final Map<Class<?>, Constraint> CONSTRAINT_CACHE = new ConcurrentHashMap<>();

  private final Map<Class<? extends ValueSerializer<?>>, ValueSerializer<?>> serializerCache =
      new ConcurrentHashMap<>();

  public MaskModule() {
    super("MaskModule", Version.unknownVersion());
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    context.addSerializerModifier(
        new ValueSerializerModifier() {
          @Override
          public List<BeanPropertyWriter> changeProperties(
              SerializationConfig config,
              BeanDescription.Supplier beanDesc,
              List<BeanPropertyWriter> beanProperties) {

            BeanDescription desc = beanDesc.get();

            for (BeanPropertyWriter writer : beanProperties) {
              Constraint constraint = findMaskConstraint(writer, desc);
              if (constraint != null) {
                writer.assignSerializer(createSerializer(constraint.maskedBy()));
              }
            }
            return beanProperties;
          }
        });
  }

  private static Constraint findMaskConstraint(BeanPropertyWriter writer, BeanDescription desc) {
    Constraint c = fieldConstraints(desc, writer).findFirst().orElse(null);
    if (c != null) {
      return c;
    }
    return writer
        .getMember()
        .annotations()
        .map(ann -> resolveConstraint(ann.annotationType()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Stream<Constraint> fieldConstraints(
      BeanDescription beanDesc, BeanPropertyWriter writer) {
    Field field = findField(beanDesc.getBeanClass(), writer.getName());
    if (field == null) {
      return Stream.empty();
    }
    return Stream.of(field.getAnnotations())
        .map(ann -> resolveConstraint(ann.annotationType()))
        .filter(Objects::nonNull);
  }

  private static Field findField(Class<?> clazz, String name) {
    for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
      try {
        return c.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        // continue to parent
      }
    }
    return null;
  }

  private static Constraint resolveConstraint(Class<?> annotationType) {
    return CONSTRAINT_CACHE.computeIfAbsent(annotationType, t -> t.getAnnotation(Constraint.class));
  }

  private ValueSerializer<Object> createSerializer(Class<? extends ValueSerializer<?>> clazz) {
    @SuppressWarnings("unchecked")
    ValueSerializer<Object> ser =
        (ValueSerializer<Object>)
            serializerCache.computeIfAbsent(
                clazz,
                k -> {
                  try {
                    return k.getDeclaredConstructor().newInstance();
                  } catch (Exception e) {
                    throw new RuntimeException(
                        "Failed to instantiate serializer: " + k.getName(), e);
                  }
                });
    return ser;
  }
}
