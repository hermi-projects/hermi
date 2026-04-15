package org.hermi.usecase.dispatcher;

import org.hermi.usecase.standard.UseCase;
import org.hermi.validation.Validatable;

public abstract class Handler<C extends Validatable, R> extends UseCase<C, R> {
  abstract boolean support(C context);
}
