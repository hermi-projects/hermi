package org.hermi.logging.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

class EnableHermiLoggingAnnotationTest {

  @EnableHermiLogging
  static class AnnotatedClass {}

  @EnableHermiLogging("com.example.app")
  static class ClassWithCustomPackage {}

  @Test
  void shouldHaveRuntimeRetention() {
    Retention retention = EnableHermiLogging.class.getAnnotation(Retention.class);
    assertThat(retention).isNotNull();
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
  }

  @Test
  void shouldTargetTypeOnly() {
    Target target = EnableHermiLogging.class.getAnnotation(Target.class);
    assertThat(target).isNotNull();
    assertThat(target.value()).containsExactly(ElementType.TYPE);
  }

  @Test
  void shouldDefaultValueToEmptyString() throws Exception {
    String defaultVal = EnableHermiLogging.class.getMethod("value").getDefaultValue().toString();
    assertThat(defaultVal).isEmpty();
  }

  @Test
  void shouldBePresentOnAnnotatedClass() {
    EnableHermiLogging annotation = AnnotatedClass.class.getAnnotation(EnableHermiLogging.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEmpty();
  }

  @Test
  void shouldSupportCustomPackageValue() {
    EnableHermiLogging annotation =
        ClassWithCustomPackage.class.getAnnotation(EnableHermiLogging.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEqualTo("com.example.app");
  }

  @Test
  void shouldNotBeInherited() {
    assertThat(EnableHermiLogging.class.isAnnotationPresent(java.lang.annotation.Inherited.class))
        .isFalse();
  }

  @Test
  void shouldBeMetaAnnotatedWithTargetAndRetention() {
    assertThat(EnableHermiLogging.class.isAnnotationPresent(Target.class)).isTrue();
    assertThat(EnableHermiLogging.class.isAnnotationPresent(Retention.class)).isTrue();
  }
}
