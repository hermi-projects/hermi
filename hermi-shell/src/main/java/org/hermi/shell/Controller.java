package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.commons.audit.PersistentAuditor;

public abstract class Controller<C, R> extends Executor<C, R> {

  protected Controller() {
    super();
  }

  protected Controller(PersistentAuditor<C, R> auditor) {
    super(auditor);
  }
}
