package org.hermi.shell.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsoleMessenger<M, R> {
  private final Map<M, R> store;

  public ConsoleMessenger() {
    this.store = new ConcurrentHashMap<>();
  }

  public R publish(M message) {
    System.out.printf(
        "[%s] INFO: Publishing message -> %s%n", this.getClass().getSimpleName(), message);
    R result = store.get(message);
    System.out.printf(
        "[%s] INFO: Publish completed  -> %s%n", this.getClass().getSimpleName(), result);
    return result;
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
