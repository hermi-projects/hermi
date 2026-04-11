package org.hermi.usecase.dispatcher;

import org.hermi.commons.validation.Validatable;
import org.hermi.usecase.standard.UseCase;

public abstract class Handler<C extends Validatable, R> extends UseCase<C, R> {
  abstract boolean support(C context);
}
