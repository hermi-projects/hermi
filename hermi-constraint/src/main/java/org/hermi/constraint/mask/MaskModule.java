package org.hermi.constraint.mask;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * Jackson module that replaces a field's serializer with a masking serializer when the field
 * carries an annotation meta-annotated with {@link Constraint}.
 *
 * <p>A new masking rule requires only two files: an annotation + a serializer. The module discovers
 * rules by scanning each property's annotations for the {@code @Constraint} meta-annotation,
 * recursively walking the annotation hierarchy to support composed annotations.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * JsonMapper mapper = JsonMapper.builder()
 *     .addModule(new MaskModule())
 *     .build();
 * }</pre>
 */
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
              Constraint constraint = findConstraint(writer);
              if (constraint != null) {
                writer.assignSerializer(createSerializer(constraint.maskedBy()));
              }
            }
            return beanProperties;
          }
        });
  }

  /**
   * Walks a property's annotations looking for a {@link Constraint} meta-annotation, recursively
   * searching the annotation hierarchy so that composed annotations (e.g. {@code @MySSN} annotated
   * with {@code @SSN}) are discovered.
   */
  private static Constraint findConstraint(BeanPropertyWriter writer) {
    return writer
        .getMember()
        .annotations()
        .map(MaskModule::resolveConstraint)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /**
   * Recursively searches {@code ann}'s annotation type and its meta-annotations for {@link
   * Constraint}, skipping standard {@code java.lang.annotation} types. Returns {@code null} when no
   * constraint is found or a cycle is detected.
   */
  private static Constraint resolveConstraint(Annotation ann) {
    return resolveConstraint(ann.annotationType(), new HashSet<>());
  }

  private static Constraint resolveConstraint(Class<?> annType, Set<Class<?>> visited) {
    if (!visited.add(annType)) {
      return null;
    }
    Constraint constraint = annType.getAnnotation(Constraint.class);
    if (constraint != null) {
      return constraint;
    }
    for (Annotation metaAnn : annType.getAnnotations()) {
      Class<? extends Annotation> metaType = metaAnn.annotationType();
      if (metaType.getName().startsWith("java.lang.annotation")) {
        continue;
      }
      constraint = resolveConstraint(metaType, visited);
      if (constraint != null) {
        return constraint;
      }
    }
    return null;
  }

  /**
   * Reflectively instantiates and caches a masking serializer via its no-arg constructor. Results
   * are memoized in {@code serializerCache} so each serializer class is created at most once.
   */
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
