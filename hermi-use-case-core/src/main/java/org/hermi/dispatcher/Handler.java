package org.hermi.dispatcher;

import org.hermi.commons.validation.Validatable;
import org.hermi.usecase.UseCase;

public abstract class Handler<C extends Validatable, R> extends UseCase<C, R> {
  abstract boolean support(C input);
}
