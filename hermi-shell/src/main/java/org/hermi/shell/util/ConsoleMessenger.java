package org.hermi.shell.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hermi.shell.Messenger;

public class ConsoleMessenger<M, R> extends Messenger<M, R> {
  private final Map<M, R> store;

  public ConsoleMessenger() {
    this.store = new ConcurrentHashMap<>();
  }

  @Override
  protected void saveMessage(M message) {
    System.out.printf(
        "[%s] INFO: Publishing message -> %s%n", this.getClass().getSimpleName(), message);
  }

  @Override
  protected R doPublish(M message) {
    return store.get(message);
  }

  @Override
  protected void saveResult(M message, R result) {
    System.out.printf(
        "[%s] INFO: Publish completed  -> %s%n", this.getClass().getSimpleName(), result);
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
