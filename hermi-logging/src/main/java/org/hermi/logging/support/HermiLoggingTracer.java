package org.hermi.logging.support;

import jakarta.el.ELProcessor;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hermi.constraint.mask.MaskMapper;
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
  // Tracing
  // ------------------------------------------------------------------

  /** 用默认 label（{@code ClassName.methodName()}）执行 trace。 */
  public Object trace(ProceedingJoinPoint jp) throws Throwable {
    return trace(jp, "");
  }

  /** 用 message 执行 trace（message 为空时使用默认 label，非空时解析 EL 表达式）。 */
  public Object trace(ProceedingJoinPoint jp, String message) throws Throwable {
    Class<?> targetClass = jp.getTarget().getClass();
    String label =
        message == null || message.isBlank()
            ? targetClass.getSimpleName() + "." + jp.getSignature().getName() + "()"
            : resolveMessage(message, jp);
    return doTrace(jp, label, targetClass);
  }

  private Object doTrace(ProceedingJoinPoint jp, String label, Class<?> targetClass)
      throws Throwable {
    Logger log = LoggerFactory.getLogger(targetClass);

    log.atInfo().log("{} - STARTED", label);
    long start = System.nanoTime();
    try {
      Object result = jp.proceed();
      log.atInfo()
          .addKeyValue("duration", Duration.ofNanos(System.nanoTime() - start).toMillis())
          .log("{} - FINISHED", label);
      return result;
    } catch (Throwable ex) {
      log.atError()
          .addKeyValue("args", MaskMapper.mask(jp.getArgs()))
          .addKeyValue("duration", Duration.ofNanos(System.nanoTime() - start).toMillis())
          .addKeyValue("exceptionClass", ex.getClass().getName())
          .addKeyValue("exceptionMessage", ex.getMessage())
          .setCause(ex)
          .log("{} - FAILED", label);
      throw ex;
    }
  }

  // ------------------------------------------------------------------
  // EL message resolution
  // ------------------------------------------------------------------

  private String resolveMessage(String message, ProceedingJoinPoint jp) {
    if (!message.contains("${")) return message;

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
}
