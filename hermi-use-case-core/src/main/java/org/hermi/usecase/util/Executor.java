package org.hermi.usecase.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
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
        command, getSimpleName() + ": Command, " + getGenericTypeName(0) + ", cannot be null");
    Validator.validate(command);
    if ((command instanceof Validatable) && !((Validatable) command).isValid()) {
      throw new InputValidationException(
          getSimpleName() + ": Command, " + getGenericTypeName(0) + ", is not valid");
    }
  }

  protected void validateResult(R result) {
    Objects.requireNonNull(
        result, getSimpleName() + ": Result, " + getGenericTypeName(1) + ", cannot be null");
    Validator.validate(result);
    if ((result instanceof Validatable) && !((Validatable) result).isValid()) {
      throw new InputValidationException(
          getSimpleName() + ": Result, " + getGenericTypeName(1) + ", is not valid");
    }
  }

  protected String getGenericTypeName(int index) {
    Type superclass = getClass().getGenericSuperclass();
    if (superclass instanceof ParameterizedType) {
      Type[] actualTypeArguments = ((ParameterizedType) superclass).getActualTypeArguments();
      if (actualTypeArguments != null && index < actualTypeArguments.length) {
        Type type = actualTypeArguments[index];
        if (type instanceof Class<?>) {
          return ((Class<?>) type).getSimpleName();
        } else {
          return type.getTypeName();
        }
      }
    }
    return "UnknownType";
  }

  protected String getSimpleName() {
    return getClass().getSimpleName();
  }
}
