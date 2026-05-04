package org.hermi.logging.aop;

import jakarta.el.ELProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hermi.logging.annotations.EnableHermiLogging;
import org.hermi.logging.annotations.HermiLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <pre>
 * 规则：
 *   1. {@code @EnableHermiLogging} — 放在入口类上，圈定根包（只 trace 该包及子包内的非 private 方法）
 *   2. {@code @HermiLogging} — 放在方法/类上，trace 链的唯一入口；可自定义 message
 *   3. message 支持 EL 表达式（{@code ${paramName}}），变量为方法参数名
 *   4. 链式传播 — 入口方法内部调用的根包内非 private 方法自动 trace，label 默认 ClassName.methodName()
 *
 * 示例：
 *   {@code @EnableHermiLogging}
 *   public class App {
 *       public static void main(String[] args) {
 *           new PaymentService().refund(new RefundRequest("R-001"));
 *       }
 *   }
 *
 *   // 方法级：只 trace refund()
 *   class PaymentService {
 *       {@code @HermiLogging}(message = "退款单 ${request.orderId}")
 *       public Result refund(RefundRequest request) {
 *           validate(request);  // 自动 trace: PaymentService.validate()
 *           return new Result("OK");
 *       }
 *   }
 *
 *   // 类级：所有方法都是入口
 *   {@code @HermiLogging}(message = "支付操作")
 *   class PaymentService {
 *       public Result refund(RefundRequest request) { ... }
 *       public Result pay(PayRequest request) { ... }
 *   }
 * </pre>
 */
@Aspect
public class HermiLoggingAspect {

  private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

  private static final List<String> ROOT_PACKAGES = new ArrayList<>();
  private static volatile boolean rootInitialized;

  private static final ThreadLocal<Integer> TRACE_DEPTH = ThreadLocal.withInitial(() -> 0);

  // ------------------------------------------------------------------
  // Entry point
  // ------------------------------------------------------------------

  @Around("execution(!private * *(..))")
  public Object traceEntry(ProceedingJoinPoint jp) throws Throwable {
    Class<?> declaringType = jp.getSignature().getDeclaringType();
    initRootIfNeeded(declaringType);

    HermiLogging herm = resolveHermiLogging(jp);
    int depth = TRACE_DEPTH.get();

    // @HermiLogging → trace & starts a chain (if within root package)
    if (herm != null && shouldTrace(declaringType)) {
      TRACE_DEPTH.set(depth + 1);
      try {
        return proceed(jp, herm.message());
      } finally {
        TRACE_DEPTH.set(depth);
      }
    }

    // Inside a trace chain → trace downstream calls within root package
    if (depth > 0 && shouldTrace(declaringType)) {
      return proceed(jp, "");
    }

    return jp.proceed();
  }

  // ------------------------------------------------------------------
  // Root package initialization
  // ------------------------------------------------------------------

  private void initRootIfNeeded(Class<?> clazz) {
    if (rootInitialized) return;

    EnableHermiLogging enable = clazz.getAnnotation(EnableHermiLogging.class);
    if (enable == null) return;

    synchronized (HermiLoggingAspect.class) {
      if (rootInitialized) return;

      if (enable.value().isEmpty()) {
        ROOT_PACKAGES.add(clazz.getPackageName());
      } else {
        for (String pkg : enable.value().split(";")) {
          String trimmed = pkg.trim();
          if (!trimmed.isEmpty() && !ROOT_PACKAGES.contains(trimmed)) {
            ROOT_PACKAGES.add(trimmed);
          }
        }
      }
      rootInitialized = true;
    }
  }

  private boolean shouldTrace(Class<?> clazz) {
    String pkg = clazz.getPackageName();
    for (String root : ROOT_PACKAGES) {
      if (pkg.startsWith(root)) {
        return true;
      }
    }
    return false;
  }

  // ------------------------------------------------------------------
  // Annotation resolution
  // ------------------------------------------------------------------

  private HermiLogging resolveHermiLogging(ProceedingJoinPoint jp) {
    MethodSignature sig = (MethodSignature) jp.getSignature();
    java.lang.reflect.Method method = sig.getMethod();

    HermiLogging herm = method.getAnnotation(HermiLogging.class);
    if (herm != null) return herm;

    return (HermiLogging) sig.getDeclaringType().getAnnotation(HermiLogging.class);
  }

  // ------------------------------------------------------------------
  // Core tracing logic
  // ------------------------------------------------------------------

  private Object proceed(ProceedingJoinPoint jp, String message) throws Throwable {
    Class<?> targetClass = jp.getTarget().getClass();
    Logger log = LoggerFactory.getLogger(targetClass);

    String methodName = jp.getSignature().getName();
    String simpleName = targetClass.getSimpleName();
    String label =
        resolveMessage(
            message, jp.getArgs(), (MethodSignature) jp.getSignature(), simpleName, methodName);

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

  // ------------------------------------------------------------------
  // EL message resolution
  // ------------------------------------------------------------------

  private String resolveMessage(
      String message, Object[] args, MethodSignature sig, String simpleName, String methodName) {
    if (message == null || message.isEmpty()) {
      return String.format("%s.%s()", simpleName, methodName);
    }

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
