package org.hermi.usecase.commons.execution;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Set;
import org.hermi.usecase.commons.validation.InputValidationException;
import org.hermi.usecase.commons.validation.Validatable;
import org.hermi.usecase.commons.validation.Validator;

public abstract class Executor<I, O> {
  protected abstract O doRun(I input);

  protected O run(I input) {
    validateInput(input);
    O output = doRun(input);
    validateOutput(output);
    return output;
  }

  protected void validateInput(I input) {
    Objects.requireNonNull(
        input, getSimpleClassName() + ": Input, " + getInputTypeName() + ", cannot be null");
    if (!(input instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<I>> violations = Validator.validate(input);
    if (!violations.isEmpty()) {
      throw new InputValidationException(
          getSimpleClassName() + ": Input, " + getInputTypeName() + ", is not valid", violations);
    }
  }

  protected void validateOutput(O output) {
    Objects.requireNonNull(
        output, getSimpleClassName() + ": Output, " + getOutputTypeName() + ", cannot be null");
    if (!(output instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<O>> violations = Validator.validate(output);
    if (!violations.isEmpty()) {
      throw new InputValidationException(
          getSimpleClassName() + ": Output, " + getOutputTypeName() + ", is not valid", violations);
    }
  }

  protected String getInputTypeName() {
    return getGenericTypeName(0);
  }

  protected String getOutputTypeName() {
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
