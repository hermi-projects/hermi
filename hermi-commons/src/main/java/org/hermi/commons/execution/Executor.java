package org.hermi.commons.execution;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Set;
import org.hermi.commons.conversion.Converter;
import org.hermi.commons.conversion.Convertible;
import org.hermi.commons.validation.InputValidationException;
import org.hermi.commons.validation.Validatable;
import org.hermi.commons.validation.Validator;

/**
 * Abstract base class for components that execute logic based on a context and return a result.
 *
 * <p>The Executor handles:
 *
 * <ul>
 *   <li>Pre-execution validation of the input context.
 *   <li>Post-execution validation of the returned result.
 *   <li>Generic type resolution for error reporting.
 *   <li>Convenience methods for conversion-based execution.
 * </ul>
 *
 * @param <C> the type of the execution context
 * @param <R> the type of the execution result
 */
public abstract class Executor<C, R> {
  /**
   * Implements the core execution logic.
   *
   * @param context the validated execution context
   * @return the execution result
   */
  protected abstract R doExecute(C context);

  /**
   * Executes the logic with the given context.
   *
   * @param context the execution context
   * @return the execution result
   * @throws InputValidationException if the context or result is invalid
   * @throws NullPointerException if the context or result is null
   */
  public R execute(C context) {
    validateContext(context);
    R result = doExecute(context);
    validateResult(result);
    return result;
  }

  /**
   * Executes the logic by first converting the source context.
   *
   * @param convertibleContext the source context that can be converted to {@code C}
   * @return the execution result
   * @throws NullPointerException if convertibleContext is null
   */
  public R execute(Convertible<C> convertibleContext) {
    Objects.requireNonNull(
        convertibleContext, getSimpleClassName() + ", convertible context cannot be null");
    return execute(convertibleContext.convert());
  }

  /**
   * Executes the logic by converting a source instance using the provided converter.
   *
   * @param <S> the type of the source instance
   * @param source the source instance
   * @param converter the converter to transform {@code S} to {@code C}
   * @return the execution result
   * @throws NullPointerException if source or converter is null
   */
  public <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return execute(converter.convert(source));
  }

  /**
   * Validates the input context.
   *
   * @param context the context to validate
   * @throws NullPointerException if context is null
   * @throws InputValidationException if context is {@link Validatable} and fails validation
   */
  protected void validateContext(C context) {
    Objects.requireNonNull(
        context, getSimpleClassName() + ": Context, " + getContextTypeName() + ", cannot be null");
    if (!(context instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<C>> violations = Validator.validate(context);
    if (!violations.isEmpty()) {
      throw new InputValidationException(
          getSimpleClassName() + ": Context, " + getContextTypeName() + ", is not valid",
          violations);
    }
  }

  /**
   * Validates the execution result.
   *
   * @param result the result to validate
   * @throws NullPointerException if result is null
   * @throws InputValidationException if result is {@link Validatable} and fails validation
   */
  protected void validateResult(R result) {
    Objects.requireNonNull(
        result, getSimpleClassName() + ": Result, " + getResultTypeName() + ", cannot be null");
    if (!(result instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<R>> violations = Validator.validate(result);
    if (!violations.isEmpty()) {
      throw new InputValidationException(
          getSimpleClassName() + ": Result, " + getResultTypeName() + ", is not valid", violations);
    }
  }

  /**
   * Gets the simple name of the context type for error reporting.
   *
   * @return context type name
   */
  protected String getContextTypeName() {
    return getGenericTypeName(0);
  }

  /**
   * Gets the simple name of the result type for error reporting.
   *
   * @return result type name
   */
  protected String getResultTypeName() {
    return getGenericTypeName(1);
  }

  /**
   * Gets the simple name of the current class.
   *
   * @return simple class name
   */
  protected String getSimpleClassName() {
    return getClass().getSimpleName();
  }

  private String getGenericTypeName(int index) {
    Type type = resolveGenericType(getClass(), index);
    if (type instanceof Class<?>) {
      return ((Class<?>) type).getSimpleName();
    }
    return type.getTypeName();
  }

  private Type resolveGenericType(Class<?> clazz, int index) {
    Type superclass = clazz.getGenericSuperclass();

    if (superclass instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) superclass;
      Type[] args = pt.getActualTypeArguments();
      if (index < args.length) {
        return unwrap(args[index]);
      }
    }

    // Walk up the hierarchy
    if (clazz.getSuperclass() != null) {
      return resolveGenericType(clazz.getSuperclass(), index);
    }

    return Object.class;
  }

  private Type unwrap(Type type) {
    if (type instanceof ParameterizedType) {
      return ((ParameterizedType) type).getRawType();
    }
    if (type instanceof TypeVariable<?>) {
      Type[] bounds = ((TypeVariable<?>) type).getBounds();
      return bounds.length > 0 ? unwrap(bounds[0]) : Object.class;
    }
    return type;
  }
}
