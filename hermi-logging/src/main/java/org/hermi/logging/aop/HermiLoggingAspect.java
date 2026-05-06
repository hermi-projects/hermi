package org.hermi.logging.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hermi.logging.annotations.HermiLogging;
import org.hermi.logging.support.HermiLoggingTracer;
import org.hermi.logging.support.RootPackageRegistry;

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

  private final RootPackageRegistry packageRegistry = RootPackageRegistry.instance();
  private final HermiLoggingTracer tracer = new HermiLoggingTracer();

  @Around("execution(!private * *(..))")
  public Object traceEntry(ProceedingJoinPoint jp) throws Throwable {
    Class<?> declaringType = jp.getSignature().getDeclaringType();
    if (!packageRegistry.shouldTrace(declaringType)) {
      return jp.proceed();
    }
    HermiLogging hermiLogging = tracer.resolveConfig(jp);

    if (hermiLogging != null) {
      int prev = tracer.enterChain();
      try {
        return tracer.trace(jp, hermiLogging.message());
      } finally {
        tracer.leaveChain(prev);
      }
    }

    if (tracer.isInChain()) {
      return tracer.trace(jp);
    }

    return jp.proceed();
  }
}
