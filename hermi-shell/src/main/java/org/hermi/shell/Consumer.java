package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.commons.audit.PersistentAuditor;
import org.hermi.constraint.validation.Validatable;

public abstract class Consumer<E extends Validatable, R> extends Executor<E, R> {
  public abstract void consume(E context);

  protected Consumer() {
    super();
  }

  protected Consumer(PersistentAuditor<E, R> auditor) {
    super(auditor);
  }
}
