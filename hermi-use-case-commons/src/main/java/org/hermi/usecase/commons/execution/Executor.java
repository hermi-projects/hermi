package org.hermi.usecase.commons.execution;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Set;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.validation.InputValidationException;
import org.hermi.usecase.commons.validation.Validatable;
import org.hermi.usecase.commons.validation.Validator;

public abstract class Executor<C, R> {
  protected abstract R doExecute(C context);

  public R execute(C context) {
    validateContext(context);
    R result = doExecute(context);
    validateResult(result);
    return result;
  }

  public R execute(Convertible<C> convertibleContext) {
    Objects.requireNonNull(
        convertibleContext, getSimpleClassName() + ", convertible context cannot be null");
    return execute(convertibleContext.convert());
  }

  public <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return execute(converter.convert(source));
  }

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

  protected String getContextTypeName() {
    return getGenericTypeName(0);
  }

  protected String getResultTypeName() {
    return getGenericTypeName(1);
  }

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
