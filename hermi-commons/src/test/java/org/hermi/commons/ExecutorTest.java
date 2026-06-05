package org.hermi.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hermi.commons.conversion.Converter;
import org.hermi.commons.conversion.Convertible;
import org.hermi.constraint.validation.InputValidationException;
import org.hermi.constraint.validation.Validatable;
import org.junit.jupiter.api.Test;

class ExecutorTest {

  // Test Fixtures
  static class ValidContext implements Validatable {
    @NotBlank String value;

    ValidContext(String value) {
      this.value = value;
    }
  }

  static class InvalidContext implements Validatable {
    @NotBlank String value;

    InvalidContext(String value) {
      this.value = value;
    }
  }

  static class ValidResult implements Validatable {
    @NotNull String output;

    ValidResult(String output) {
      this.output = output;
    }
  }

  static class NonValidatableContext {
    String value;

    NonValidatableContext(String value) {
      this.value = value;
    }
  }

  static class NonValidatableResult {
    String output;

    NonValidatableResult(String output) {
      this.output = output;
    }
  }

  // Executors
  static class NormalExecutor extends Executor<ValidContext, ValidResult> {
    @Override
    protected ValidResult doExecute(ValidContext context) {
      if ("error".equals(context.value)) throw new RuntimeException("Business error");
      if ("null-result".equals(context.value)) return null;
      if ("invalid-result".equals(context.value)) return new ValidResult(null);
      return new ValidResult("Processed " + context.value);
    }
  }

  static class UnconstrainedExecutor extends Executor<NonValidatableContext, NonValidatableResult> {
    @Override
    protected NonValidatableResult doExecute(NonValidatableContext context) {
      return new NonValidatableResult("Processed " + context.value);
    }
  }

  @Test
  void shouldExecuteSuccessfullyWhenValid() {
    NormalExecutor executor = new NormalExecutor();
    ValidResult result = executor.execute(new ValidContext("test"));
    assertThat(result.output).isEqualTo("Processed test");
  }

  @Test
  void shouldSkipValidationWhenNotValidatable() {
    UnconstrainedExecutor executor = new UnconstrainedExecutor();
    // Context and Result do not implement Validatable, should not throw
    NonValidatableResult result = executor.execute(new NonValidatableContext(null));
    assertThat(result.output).isEqualTo("Processed null");
  }

  @Test
  void shouldThrowWhenContextIsNull() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute((ValidContext) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Context cannot be null");
  }

  @Test
  void shouldThrowWhenContextIsInvalid() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute(new ValidContext("  ")))
        .isInstanceOf(InputValidationException.class)
        .hasMessageContaining("Context, ValidContext, is not valid");
  }

  @Test
  void shouldThrowWhenResultIsNull() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute(new ValidContext("null-result")))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Result cannot be null");
  }

  @Test
  void shouldThrowWhenResultIsInvalid() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute(new ValidContext("invalid-result")))
        .isInstanceOf(InputValidationException.class)
        .hasMessageContaining("Result, ValidResult, is not valid");
  }

  @Test
  void shouldPropagateExceptionsFromDoExecute() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute(new ValidContext("error")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Business error");
  }

  @Test
  void shouldExecuteWithConvertible() {
    NormalExecutor executor = new NormalExecutor();
    Convertible<ValidContext> convertible = () -> new ValidContext("convertible");
    ValidResult result = executor.execute(convertible);
    assertThat(result.output).isEqualTo("Processed convertible");
  }

  @Test
  void shouldExecuteWithConverter() {
    NormalExecutor executor = new NormalExecutor();
    Converter<Integer, ValidContext> converter = source -> new ValidContext(String.valueOf(source));
    ValidResult result = executor.execute(123, converter);
    assertThat(result.output).isEqualTo("Processed 123");
  }

  @Test
  void shouldThrowWhenConvertibleIsNull() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute((Convertible<ValidContext>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("convertible context cannot be null");
  }

  @Test
  void shouldThrowWhenConverterIsNull() {
    NormalExecutor executor = new NormalExecutor();
    assertThatThrownBy(() -> executor.execute(123, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("converter cannot be null");
  }
}
