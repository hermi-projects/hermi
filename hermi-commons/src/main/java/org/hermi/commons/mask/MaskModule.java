package org.hermi.commons.mask;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.hermi.annotations.mask.Constraint;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

public class MaskModule extends SimpleModule {
  private final Map<Class<?>, ValueSerializer<?>> serializerCache = new ConcurrentHashMap<>();

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
            for (BeanPropertyWriter writer : beanProperties) {
              Constraint constraint = findMaskConstraint(writer);
              if (constraint != null) {
                writer.assignSerializer(createSerializer(constraint.maskedBy()));
              }
            }
            return beanProperties;
          }
        });
  }

  private static Constraint findMaskConstraint(BeanPropertyWriter writer) {
    return writer
        .getMember()
        .annotations()
        .map(ann -> ann.annotationType().getAnnotation(Constraint.class))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private ValueSerializer<Object> createSerializer(Class<?> clazz) {
    return (ValueSerializer<Object>)
        serializerCache.computeIfAbsent(
            clazz,
            k -> {
              try {
                var constructor = k.getDeclaredConstructor();
                if (!constructor.canAccess(null)) {
                  constructor.setAccessible(true);
                }
                return (ValueSerializer<?>) constructor.newInstance();
              } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                    "Masking serializer " + k.getName() + " must have a no-args constructor", e);
              } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to instantiate mask serializer: " + k.getName(), e);
              }
            });
  }
}
