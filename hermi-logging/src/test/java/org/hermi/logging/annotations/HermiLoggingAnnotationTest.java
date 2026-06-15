package org.hermi.logging.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

class HermiLoggingAnnotationTest {

  @HermiLogging
  static class AnnotatedClass {
    @HermiLogging
    void annotatedMethod() {}
  }

  @HermiLogging(message = "custom message")
  static class ClassWithCustomMessage {}

  static class Subclass extends AnnotatedClass {}

  @HermiLogging
  interface AnnotatedInterface {}

  static class InterfaceImpl implements AnnotatedInterface {}

  @Test
  void shouldHaveRuntimeRetention() {
    Retention retention = HermiLogging.class.getAnnotation(Retention.class);
    assertThat(retention).isNotNull();
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
  }

  @Test
  void shouldTargetTypeAndMethod() {
    Target target = HermiLogging.class.getAnnotation(Target.class);
    assertThat(target).isNotNull();
    assertThat(target.value()).contains(ElementType.TYPE, ElementType.METHOD);
  }

  @Test
  void shouldBeInherited() {
    Inherited inherited = HermiLogging.class.getAnnotation(Inherited.class);
    assertThat(inherited).isNotNull();
  }

  @Test
  void shouldDefaultMessageToEmptyString() throws Exception {
    String defaultMsg = HermiLogging.class.getMethod("message").getDefaultValue().toString();
    assertThat(defaultMsg).isEmpty();
  }

  @Test
  void shouldBePresentOnAnnotatedClass() {
    HermiLogging annotation = AnnotatedClass.class.getAnnotation(HermiLogging.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.message()).isEmpty();
  }

  @Test
  void shouldBePresentOnAnnotatedMethod() throws Exception {
    HermiLogging annotation =
        AnnotatedClass.class.getDeclaredMethod("annotatedMethod").getAnnotation(HermiLogging.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.message()).isEmpty();
  }

  @Test
  void shouldSupportCustomMessage() {
    HermiLogging annotation = ClassWithCustomMessage.class.getAnnotation(HermiLogging.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.message()).isEqualTo("custom message");
  }

  @Test
  void shouldBeInheritedBySubclasses() {
    // @HermiLogging is marked @Inherited, so subclass should inherit it
    HermiLogging annotation = Subclass.class.getAnnotation(HermiLogging.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.message()).isEmpty();
  }

  @Test
  void shouldBePresentOnAnnotatedInterface() {
    HermiLogging annotation = AnnotatedInterface.class.getAnnotation(HermiLogging.class);
    assertThat(annotation).isNotNull();
  }

  @Test
  void shouldNotBeInheritedThroughInterfaceImplementation() {
    // @Inherited in Java only applies to superclass inheritance, not interfaces
    HermiLogging annotation = InterfaceImpl.class.getAnnotation(HermiLogging.class);
    assertThat(annotation).isNull();
  }

  @Test
  void shouldBeMetaAnnotatedWithTargetRetentionAndInherited() {
    // Verify all three meta-annotations are present
    assertThat(HermiLogging.class.isAnnotationPresent(Target.class)).isTrue();
    assertThat(HermiLogging.class.isAnnotationPresent(Retention.class)).isTrue();
    assertThat(HermiLogging.class.isAnnotationPresent(Inherited.class)).isTrue();
  }
}
