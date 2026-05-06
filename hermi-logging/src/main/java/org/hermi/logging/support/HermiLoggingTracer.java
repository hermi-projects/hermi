package org.hermi.logging.support;

import jakarta.el.ELProcessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hermi.logging.annotations.HermiLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 负责 {@code @HermiLogging} 的注解解析、trace 链、日志输出。 */
public class HermiLoggingTracer {

  private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

  // 用计数器而非 boolean：@HermiLogging 方法可能嵌套调用另一个 @HermiLogging 方法，
  // boolean 在内层退出时会把链状态清掉，导致外层后续调用丢失 trace。
  private final ThreadLocal<Integer> chainDepth = ThreadLocal.withInitial(() -> 0);

  /** 进入 trace 链，返回进入前的深度用于退出时恢复。 */
  public int enterChain() {
    int prev = chainDepth.get();
    chainDepth.set(prev + 1);
    return prev;
  }

  /** 退出 trace 链，恢复到进入前的深度。 */
  public void leaveChain(int previousDepth) {
    chainDepth.set(previousDepth);
  }

  /** 当前是否在 trace 链中。 */
  public boolean isInChain() {
    return chainDepth.get() > 0;
  }

  // ------------------------------------------------------------------
  // Annotation resolution
  // ------------------------------------------------------------------

  public HermiLogging resolveConfig(ProceedingJoinPoint jp) {
    MethodSignature sig = (MethodSignature) jp.getSignature();
    java.lang.reflect.Method method = sig.getMethod();

    HermiLogging herm = method.getAnnotation(HermiLogging.class);
    if (herm != null) return herm;

    Class<?> declaringType = sig.getDeclaringType();
    return declaringType.getAnnotation(HermiLogging.class);
  }

  // ------------------------------------------------------------------
  // Label building (called by aspect)
  // ------------------------------------------------------------------

  /** 解析 message 中的 EL 表达式。 */
  public String resolveMessage(String message, ProceedingJoinPoint jp) {
    if (message == null || message.isEmpty()) return "";

    MethodSignature sig = (MethodSignature) jp.getSignature();
    Object[] args = jp.getArgs();

    ELProcessor el = new ELProcessor();

    String[] paramNames = sig.getParameterNames();
    for (int i = 0; i < args.length; i++) {
      String name = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
      el.setValue(name, args[i]);
    }

    Matcher m = EL_PATTERN.matcher(message);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      String expr = m.group(1);
      try {
        Object val = el.eval(expr);
        m.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val.toString()) : "null");
      } catch (Exception e) {
        m.appendReplacement(sb, "{" + expr + "}");
      }
    }
    m.appendTail(sb);
    return sb.toString();
  }

  // ------------------------------------------------------------------
  // Tracing
  // ------------------------------------------------------------------

  /** 用默认 label 执行 trace。 */
  public Object trace(ProceedingJoinPoint jp) throws Throwable {
    Class<?> targetClass = jp.getTarget().getClass();
    String label =
        String.format("%s.%s()", targetClass.getSimpleName(), jp.getSignature().getName());
    return trace(jp, label);
  }

  /** 用预解析的 label 执行 trace。 */
  public Object trace(ProceedingJoinPoint jp, String label) throws Throwable {
    return trace(jp, label, jp.getTarget().getClass());
  }

  private Object trace(ProceedingJoinPoint jp, String label, Class<?> targetClass)
      throws Throwable {
    Logger log = LoggerFactory.getLogger(targetClass);

    log.atInfo().addKeyValue("args", jp.getArgs()).log("{} - started", label);

    try {
      Object result = jp.proceed();
      log.atInfo().addKeyValue("result", result).log("{} - finished", label);
      return result;
    } catch (Throwable ex) {
      log.atError()
          .addKeyValue("args", jp.getArgs())
          .addKeyValue("exceptionClass", ex.getClass().getName())
          .addKeyValue("exceptionMessage", ex.getMessage())
          .setCause(ex)
          .log("{} - failed: {}", label);
      throw ex;
    }
  }
}
