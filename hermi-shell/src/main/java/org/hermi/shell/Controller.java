package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.commons.audit.Auditor;

public abstract class Controller<C, R> extends Executor<C, R> {
  protected Controller(Auditor<C, R> auditor) {
    setAuditor(auditor);
  }
}
