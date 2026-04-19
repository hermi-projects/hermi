package org.hermi.logging.commons;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hermi.logging.annotations.HermiLogging;
import org.hermi.logging.usecase.util.ElUtil;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LogUseCase<C extends LogUseCase.Context> {
  private static final Logger log = LoggerFactory.getLogger(LogUseCase.class);

  // 引用全局共享的规格化组件
  protected final LogNormalizer normalizer = LogNormalizer.getInstance();

  public interface Context {
    ProceedingJoinPoint proceedingJoinPoint();

    HermiLogging trace();
  }

  protected abstract void doLog(C context);

  public final void log(C context) {
    HermiLogging trace = context.trace();
    String event = (trace != null) ? trace.event() : null;
    String previousEvent = (String) MDC.get("event");
    boolean eventChanged = StringUtils.isNotBlank(event);

    try {
      if (eventChanged) MDC.put("event", event);
      doLog(context);
    } catch (Exception e) {
      // 确保日志本身的失败不会抛出给主业务流
      log.error("Structured logging execution failed. Event: {}", event, e);
    } finally {
      if (eventChanged) {
        if (previousEvent != null) MDC.put("event", previousEvent);
        else MDC.remove("event");
      }
    }
  }

  /**
   * 解析动态标题。
   *
   * @return 解析后的标题字符串
   */
  protected String resolveTitle(C context) {
    HermiLogging trace = context.trace();
    String titleTemplate = (trace != null) ? trace.title() : null;

    if (StringUtils.isNotBlank(titleTemplate)) {
      try {
        return ElUtil.resolve(
            titleTemplate,
            (MethodSignature) context.proceedingJoinPoint().getSignature(),
            context.proceedingJoinPoint().getArgs());
      } catch (Exception e) {
        log.warn("Title resolution failed for template: {}", titleTemplate);
        return titleTemplate;
      }
    }
    return "Execution of " + context.proceedingJoinPoint().getSignature().toShortString();
  }
}
