package org.hermi.shell.audit;

import java.util.UUID;

/**
 * A no-op {@link PersistentAuditor} for Phase 1 validation.
 *
 * <p>Does not persist anything — all lifecycle hooks return immediately. Use this when you need to
 * satisfy the {@link PersistentAuditor} contract but don't need actual persistence (e.g., local
 * testing with {@link org.hermi.shell.util.LocalClient} or {@link
 * org.hermi.shell.util.ConsoleMessenger}).
 *
 * @param <P> payload type
 * @param <R> result type
 */
public class NoopPersistentAuditor<P, R> extends PersistentAuditor<P, R> {
  private final UUID uuid = new UUID(0, 0);

  @Override
  protected UUID doRecordContext(P payload) {
    return uuid;
  }

  @Override
  protected void doRecordResult(UUID trackingId, R result) {}

  @Override
  protected void doRecordError(UUID trackingId, P payload, Exception exception) {}
}
