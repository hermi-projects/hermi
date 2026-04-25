package org.hermi.shell.util;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hermi.shell.Auditor;
import org.hermi.shell.Client;

public class LocalClient<I, O> extends Client<I, O> {
  private final Map<I, O> store;

  public LocalClient() {
    super(new LocalClientAuditor<>());
    this.store = new ConcurrentHashMap<>();
  }

  private static class LocalClientAuditor<I, O> implements Auditor<I, O> {
    @Override
    public UUID save(I input) {
      System.out.printf("[LocalClient] INFO: Exchanging input -> %s%n", input);
      return UUID.randomUUID();
    }

    @Override
    public void save(UUID trackingId, O output) {
      System.out.printf("[LocalClient] INFO: Exchange completed -> %s%n", output);
    }
  }

  @Override
  protected O doExchange(I resuest) {
    return store.get(resuest);
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
