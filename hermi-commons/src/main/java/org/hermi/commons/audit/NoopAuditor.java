package org.hermi.commons.audit;

import java.util.UUID;

public class NoopAuditor<C, R> extends Auditor<C, R> {
  private final UUID uuid = new UUID(0, 0);

  @Override
  protected UUID doRecordContext(C context) {
    return uuid;
  }

  @Override
  protected void doRecordResult(UUID trackingId, R result) {}

  @Override
  protected void doRecordError(UUID trackingId, C context, Exception exception) {}
}
