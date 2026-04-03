package org.hermi.usecase.dispatcher;

import org.hermi.usecase.commons.validation.Validatable;
import org.hermi.usecase.standard.UseCase;

public abstract class Handler<I extends Validatable, O> extends UseCase<I, O> {
  abstract boolean support(I input);
}
