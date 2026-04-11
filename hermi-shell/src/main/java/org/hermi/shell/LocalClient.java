package org.hermi.shell;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalClient<I, O> {
  private final Map<I, O> store;

  public LocalClient() {
    this.store = new ConcurrentHashMap<>();
  }

  public O exchange(I input) {
    System.out.printf(
        "[%s] INFO: Exchanging input -> %s%n", this.getClass().getSimpleName(), input);
    O result = store.get(input);
    System.out.printf(
        "[%s] INFO: Exchange completed -> %s%n", this.getClass().getSimpleName(), result);
    return result;
  }

  public LocalClient<I, O> put(I input, O output) {
    store.put(input, output);
    return this;
  }

  public O get(I input) {
    return store.get(input);
  }

  public LocalClient<I, O> remove(I input) {
    store.remove(input);
    return this;
  }

  public boolean containsKey(I input) {
    return store.containsKey(input);
  }

  public int size() {
    return store.size();
  }

  public boolean isEmpty() {
    return store.isEmpty();
  }

  public LocalClient<I, O> clear() {
    store.clear();
    return this;
  }

  public Map<I, O> getStore() {
    return Collections.unmodifiableMap(store);
  }
}
