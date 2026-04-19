package org.hermi.logging.commons;

import java.lang.reflect.Field;
import java.util.*;
import org.hermi.logging.annotations.HermiLoggingRequired;
import org.hermi.logging.usecase.api.Loggable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** 日志规格化处理器 职责：将任意 Java 对象转换为结构化的 JsonNode。 具备循环引用保护、深度限制及大对象过滤功能。 */
public class LogNormalizer {
  private final ObjectMapper mapper = new ObjectMapper();
  private final ReflectionService reflectionService = ReflectionService.getInstance();

  // 限制递归深度，防止极端嵌套下的栈溢出
  private static final int MAX_DEPTH = 5;
  // 字符串截断阈值
  private static final int MAX_STRING_LENGTH = 50;

  // 使用 ThreadLocal 记录当前解析路径上的对象，用于检测循环引用
  private final ThreadLocal<Set<Integer>> seenObjects = ThreadLocal.withInitial(HashSet::new);

  private LogNormalizer() {}

  public static LogNormalizer getInstance() {
    return Holder.INSTANCE;
  }

  private static class Holder {
    private static final LogNormalizer INSTANCE = new LogNormalizer();
  }

  public JsonNode normalize(Object arg) {
    return normalize(arg, true);
  }

  /** 对外暴露的主入口 */
  public JsonNode normalize(Object arg, boolean requiredOnly) {
    try {
      return normalizeRecursive(arg, requiredOnly, 0);
    } finally {
      // 必须清理 ThreadLocal，防止在线程池环境下发生内存泄漏
      seenObjects.get().clear();
    }
  }

  private JsonNode normalizeRecursive(Object arg, boolean requiredOnly, int depth) {
    if (arg == null) return mapper.nullNode();

    // 1. 深度保护
    if (depth > MAX_DEPTH) {
      return mapper.valueToTree("...[Max Depth Reached]");
    }

    // 2. 基础类型（String, Number, Boolean等）直接返回
    if (isPrimitive(arg)) {
      return mapper.valueToTree(
          arg instanceof String ? truncate((String) arg, MAX_STRING_LENGTH) : arg);
    }

    // 3. 过滤不可记录/危险的大对象（如流、连接等）
    if (isUnloggable(arg)) {
      return mapper.valueToTree("[" + arg.getClass().getSimpleName() + "]");
    }

    // 4. 循环引用检测
    int identityHash = System.identityHashCode(arg);
    if (seenObjects.get().contains(identityHash)) {
      return mapper.valueToTree("[Circular Reference]");
    }

    // --- 开始处理复杂对象 ---
    seenObjects.get().add(identityHash);
    try {
      // 5. 处理数组和集合
      if (arg instanceof Object[] arr) {
        return normalizeArray(Arrays.asList(arr), requiredOnly, depth);
      }
      if (arg instanceof Collection<?> col) {
        return normalizeArray(col, requiredOnly, depth);
      }

      // 6. 处理 Loggable 接口自定义输出
      if (arg instanceof Loggable l) {
        return parseLoggable(l);
      }

      // 7. 处理带有 @HermiLoggingRequired 注解的对象字段提取
      if (hasRequiredAnnotation(arg)) {
        return extractFields(arg, requiredOnly, depth);
      }

      // 8. 默认兜底：直接返回对象的 toString() 截断值
      return mapper.valueToTree(truncate(arg.toString(), MAX_STRING_LENGTH));

    } finally {
      // 退出当前递归层级时移除，以便同级的其他分支能访问（Tree结构访问路径是独立的）
      seenObjects.get().remove(identityHash);
    }
  }

  private JsonNode extractFields(Object arg, boolean requiredOnly, int depth) {
    ObjectNode node = mapper.createObjectNode();
    List<Field> fields = reflectionService.getFields(arg.getClass());

    for (Field field : fields) {
      boolean isRequired = field.isAnnotationPresent(HermiLoggingRequired.class);
      if (requiredOnly && !isRequired) continue;

      try {
        // 已经在 ReflectionService 中设为 accessible
        Object val = field.get(arg);

        // 应用 Marker 脫敏处理
        Object marked = reflectionService.applyMarker(field, val);

        // 递归处理结果值（深度+1）
        node.set(
            field.getName(),
            normalizeRecursive(marked != null ? marked : val, requiredOnly, depth + 1));
      } catch (Exception ignored) {
        // 单个字段获取失败不影响整体
      }
    }

    // 若提取完依然为空（如配置了注解但字段全是 null），则降级为 toString
    return node.isEmpty() ? mapper.valueToTree(truncate(arg.toString(), MAX_STRING_LENGTH)) : node;
  }

  private JsonNode normalizeArray(Collection<?> col, boolean requiredOnly, int depth) {
    ArrayNode array = mapper.createArrayNode();
    for (Object item : col) {
      // 集合元素深度不加 1，因为物理层级依然在同一级
      array.add(normalizeRecursive(item, requiredOnly, depth));
    }
    return array;
  }

  private boolean isPrimitive(Object o) {
    return o instanceof String
        || o instanceof Number
        || o instanceof Boolean
        || o instanceof Character;
  }

  /** 过滤掉不适合在日志中序列化的类型 */
  private boolean isUnloggable(Object arg) {
    return arg instanceof java.io.InputStream
        || arg instanceof java.io.OutputStream
        || arg instanceof java.nio.ByteBuffer
        || arg instanceof java.net.Socket
        || arg.getClass().getName().startsWith("java.sql.");
  }

  private JsonNode parseLoggable(Loggable l) {
    String raw = l.toHermiLoggingString();
    try {
      JsonNode parsed = mapper.readTree(raw);
      return (parsed.isObject() || parsed.isArray()) ? parsed : mapper.valueToTree(raw);
    } catch (Exception e) {
      return mapper.valueToTree(raw);
    }
  }

  private boolean hasRequiredAnnotation(Object obj) {
    return reflectionService.getFields(obj.getClass()).stream()
        .anyMatch(f -> f.isAnnotationPresent(HermiLoggingRequired.class));
  }

  private String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() > max ? s.substring(0, max) + "..." : s;
  }
}
