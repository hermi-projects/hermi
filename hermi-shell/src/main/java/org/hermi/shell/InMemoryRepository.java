package org.hermi.shell;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRepository<E, R> {
  private final Map<E, R> store;

  public InMemoryRepository() {
    this.store = new ConcurrentHashMap<>();
  }

  public R process(E entity) {
    System.out.printf(
        "[%s] INFO: Processing entity -> %s%n", this.getClass().getSimpleName(), entity);
    R result = store.get(entity);
    System.out.printf(
        "[%s] INFO: Process completed -> %s%n", this.getClass().getSimpleName(), result);
    return result;
  }

  public InMemoryRepository<E, R> put(E entity, R result) {
    store.put(entity, result);
    return this;
  }

  public R get(E entity) {
    return store.get(entity);
  }

  public InMemoryRepository<E, R> remove(E entity) {
    store.remove(entity);
    return this;
  }

  public boolean containsKey(E entity) {
    return store.containsKey(entity);
  }

  public int size() {
    return store.size();
  }

  public boolean isEmpty() {
    return store.isEmpty();
  }

  public InMemoryRepository<E, R> clear() {
    store.clear();
    return this;
  }

  public Map<E, R> getStore() {
    return Collections.unmodifiableMap(store);
  }
}
