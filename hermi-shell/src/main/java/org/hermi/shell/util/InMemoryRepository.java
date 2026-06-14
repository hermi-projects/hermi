package org.hermi.shell.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRepository<E, R> {
  private final Map<E, R> store;
  private final String instanceName;

  public InMemoryRepository() {
    this.store = new ConcurrentHashMap<>();
    this.instanceName = this.getClass().getSimpleName();
  }

  public R process(E entity) {
    R result = store.get(entity);
    System.out.printf("[%s] process(%s) => %s%n", instanceName, entity, result);
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

  // ---- AI Debugging: trace store state to file ----

  /**
   * Writes the full store contents as a JSON trace file into {@code target/traces/} with a unique
   * filename.
   *
   * <p>After running a Main Shell, AI can read this file to inspect every entry that was saved
   * during execution.
   */
  public void trace() {
    traceToFile(
        new File("target/traces", instanceName + "-" + System.currentTimeMillis() + ".json"));
  }

  /** Writes the store state as JSON to a specific file path. */
  public void trace(File file) {
    traceToFile(file);
  }

  private void traceToFile(File file) {
    file.getParentFile().mkdirs();
    try (FileWriter w = new FileWriter(file)) {
      w.write("{\n");
      w.write("  \"timestamp\": \"" + Instant.now() + "\",\n");
      w.write("  \"class\": \"" + instanceName + "\",\n");
      w.write("  \"size\": " + store.size() + ",\n");
      w.write("  \"store\": {\n");
      boolean first = true;
      for (Map.Entry<E, R> entry : store.entrySet()) {
        if (!first) {
          w.write(",\n");
        }
        w.write("    \"" + jsonEscape(String.valueOf(entry.getKey())) + "\": ");
        w.write("\"" + jsonEscape(String.valueOf(entry.getValue())) + "\"");
        first = false;
      }
      w.write("\n  }\n}\n");
    } catch (IOException e) {
      System.err.println(
          "[ERROR] Failed to write trace for " + instanceName + ": " + e.getMessage());
    }
    System.out.println("[TRACE] " + instanceName + " → " + file.getAbsolutePath());
  }

  private static String jsonEscape(String s) {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
