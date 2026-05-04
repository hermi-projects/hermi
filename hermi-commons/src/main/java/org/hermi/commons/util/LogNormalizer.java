package org.hermi.commons.util;

import java.lang.reflect.Field;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Thread-safe object normalizer that converts arbitrary Java objects into structured {@link
 * JsonNode} or JSON strings for safe log serialization.
 *
 * <p>Safety guarantees:
 *
 * <ul>
 *   <li>Max recursion depth of 5 — prevents stack overflow on deeply nested objects.
 *   <li>String truncation at 500 characters — bounds log event size.
 *   <li>Collection / Map size capped at 100 elements.
 *   <li>Circular reference detection via {@code ThreadLocal&lt;Set&gt;} of identity hash codes.
 *   <li>Unloggable type filtering — streams, sockets, {@code java.sql.*} classes replaced with
 *       placeholder.
 * </ul>
 *
 * <p>Field masking: when an object's field carries a {@code @Constraint}-meta-annotated annotation
 * (e.g. {@code @Mask}, {@code @SSN}), the corresponding {@link
 * org.hermi.annotations.logging.ConstraintMarker} is invoked, and the masked value replaces the
 * original in the output.
 *
 * <p>Singleton — use {@link #getInstance()}.
 */
public class LogNormalizer {
  private final ObjectMapper mapper = new ObjectMapper();
  private final ReflectionService reflectionService = ReflectionService.getInstance();

  private static final int MAX_DEPTH = 5;
  private static final int MAX_STRING_LENGTH = 500;
  private static final int MAX_COLLECTION_SIZE = 100;

  private final ThreadLocal<Set<Integer>> seenObjects = ThreadLocal.withInitial(HashSet::new);

  private LogNormalizer() {}

  public static LogNormalizer getInstance() {
    return Holder.INSTANCE;
  }

  private static class Holder {
    private static final LogNormalizer INSTANCE = new LogNormalizer();
  }

  /** Normalize and return as {@link JsonNode}. */
  public JsonNode normalize(Object arg) {
    try {
      return normalizeRecursive(arg, 0);
    } finally {
      seenObjects.get().clear();
    }
  }

  /** Normalize and serialize to a JSON string. */
  public String normalizeToString(Object arg) {
    try {
      return mapper.writeValueAsString(normalize(arg));
    } catch (Exception e) {
      return String.valueOf(arg);
    }
  }

  private JsonNode normalizeRecursive(Object arg, int depth) {
    if (arg == null) return mapper.nullNode();

    if (depth > MAX_DEPTH) {
      return mapper.valueToTree("...[Max Depth Reached]");
    }

    if (isPrimitive(arg)) {
      return mapper.valueToTree(arg instanceof String s ? truncate(s) : arg);
    }

    if (isUnloggable(arg)) {
      return mapper.valueToTree("[" + arg.getClass().getSimpleName() + "]");
    }

    int identityHash = System.identityHashCode(arg);
    if (seenObjects.get().contains(identityHash)) {
      return mapper.valueToTree("[Circular Reference]");
    }

    seenObjects.get().add(identityHash);
    try {
      if (arg instanceof Object[] arr) {
        return normalizeArray(Arrays.asList(arr), depth);
      }
      if (arg instanceof Collection<?> col) {
        return normalizeArray(col, depth);
      }
      if (arg instanceof Map<?, ?> map) {
        return normalizeMap(map, depth);
      }

      return extractFields(arg, depth);
    } finally {
      seenObjects.get().remove(identityHash);
    }
  }

  private JsonNode extractFields(Object arg, int depth) {
    ObjectNode node = mapper.createObjectNode();
    List<Field> fields = reflectionService.getFields(arg.getClass());

    for (Field field : fields) {
      try {
        Object val = field.get(arg);
        Object marked = reflectionService.applyMarker(field, val);
        node.set(field.getName(), normalizeRecursive(marked != null ? marked : val, depth + 1));
      } catch (Exception ignored) {
      }
    }

    return node.isEmpty() ? mapper.valueToTree(truncate(arg.toString())) : node;
  }

  private ArrayNode normalizeArray(Collection<?> col, int depth) {
    ArrayNode array = mapper.createArrayNode();
    for (Object item : col) {
      if (array.size() >= MAX_COLLECTION_SIZE) break;
      array.add(normalizeRecursive(item, depth));
    }
    return array;
  }

  private ObjectNode normalizeMap(Map<?, ?> map, int depth) {
    ObjectNode node = mapper.createObjectNode();
    int count = 0;
    for (Map.Entry<?, ?> e : map.entrySet()) {
      if (count++ >= MAX_COLLECTION_SIZE) break;
      JsonNode valueNode = normalizeRecursive(e.getValue(), depth);
      node.set(String.valueOf(e.getKey()), valueNode);
    }
    return node;
  }

  private boolean isPrimitive(Object o) {
    return o instanceof String
        || o instanceof Number
        || o instanceof Boolean
        || o instanceof Character;
  }

  private boolean isUnloggable(Object arg) {
    return arg instanceof java.io.InputStream
        || arg instanceof java.io.OutputStream
        || arg instanceof java.nio.ByteBuffer
        || arg instanceof java.net.Socket
        || arg.getClass().getName().startsWith("java.sql.");
  }

  private String truncate(String s) {
    return s != null && s.length() > MAX_STRING_LENGTH
        ? s.substring(0, MAX_STRING_LENGTH) + "..."
        : s;
  }
}
