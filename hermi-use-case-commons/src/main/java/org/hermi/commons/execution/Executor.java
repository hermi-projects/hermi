package org.hermi.commons.execution;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Set;
import org.hermi.commons.validation.InputValidationException;
import org.hermi.commons.validation.Validatable;
import org.hermi.commons.validation.Validator;

public abstract class Executor<C, R> {
  protected abstract R doRun(C command);

  protected R run(C command) {
    validateCommand(command);
    R result = doRun(command);
    validateResult(result);
    return result;
  }

  protected void validateCommand(C command) {
    Objects.requireNonNull(
        command, getSimpleClassName() + ": Command, " + getCommandTypeName() + ", cannot be null");
    if (!(command instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<C>> violations = Validator.validate(command);
    if (!violations.isEmpty()) {
      throw new InputValidationException(
          getSimpleClassName() + ": Command, " + getCommandTypeName() + ", is not valid",
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

  protected String getCommandTypeName() {
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
