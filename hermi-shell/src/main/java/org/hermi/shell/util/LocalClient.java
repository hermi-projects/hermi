package org.hermi.shell.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hermi.shell.Client;
import org.hermi.shell.audit.NoopPersistentAuditor;

public class LocalClient<P, R> extends Client<P, R> {
  private final Map<P, R> store;

  public LocalClient() {
    super(new NoopPersistentAuditor<>());
    this.store = new ConcurrentHashMap<>();
  }

  @Override
  protected R doExchange(P payload) {
    return store.get(payload);
  }

  public LocalClient<P, R> put(P payload, R response) {
    store.put(payload, response);
    return this;
  }

  public R get(P payload) {
    return store.get(payload);
  }

  public LocalClient<P, R> remove(P payload) {
    store.remove(payload);
    return this;
  }

  public boolean containsKey(P payload) {
    return store.containsKey(payload);
  }

  public int size() {
    return store.size();
  }

  public boolean isEmpty() {
    return store.isEmpty();
  }

  public LocalClient<P, R> clear() {
    store.clear();
    return this;
  }

  public Map<P, R> getStore() {
    return Collections.unmodifiableMap(store);
  }
}
