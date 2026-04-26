package org.hermi.shell.util;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hermi.shell.Auditor;
import org.hermi.shell.Messenger;

public class ConsoleMessenger<M, R> extends Messenger<M, R> {
  private final Map<M, R> store;

  public ConsoleMessenger() {
    super(new ConsoleMessengerAuditor<>());
    this.store = new ConcurrentHashMap<>();
  }

  private static class ConsoleMessengerAuditor<M, R> extends Auditor<M, R> {
    @Override
    protected UUID doSave(M input) {
      System.out.printf("[ConsoleMessenger] INFO: Publishing message -> %s%n", input);
      return UUID.randomUUID();
    }

    @Override
    protected void doSave(UUID trackingId, R output) {
      System.out.printf("[ConsoleMessenger] INFO: Publish completed  -> %s%n", output);
    }

    @Override
    protected void doError(UUID trackingId, Exception exception) {
      System.out.printf("[ConsoleMessenger] ERROR: Publish failed -> %s%n", exception.getMessage());
    }
  }

  @Override
  protected R doPublish(M message) {
    return store.get(message);
  }

  public ConsoleMessenger<M, R> put(M message, R result) {
    store.put(message, result);
    return this;
  }

  public R get(M message) {
    return store.get(message);
  }

  public ConsoleMessenger<M, R> remove(M message) {
    store.remove(message);
    return this;
  }

  public boolean contains(M message) {
    return store.containsKey(message);
  }

  public int size() {
    return store.size();
  }

  public boolean isEmpty() {
    return store.isEmpty();
  }

  public ConsoleMessenger<M, R> clear() {
    store.clear();
    return this;
  }

  public Map<M, R> getStore() {
    return Collections.unmodifiableMap(store);
  }
}
