package org.hermi.commons.validation;

public abstract class Validator {
  public abstract <T> void validate(T value);
}
