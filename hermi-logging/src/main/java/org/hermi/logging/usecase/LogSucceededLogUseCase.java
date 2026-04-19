package org.hermi.logging.usecase;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hermi.logging.annotations.HermiLogging;
import org.hermi.logging.commons.LogUseCase;
import org.hermi.logging.usecase.entity.SucceededLog;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogSucceededLogUseCase extends LogUseCase<LogSucceededLogUseCase.Context> {
  public static record Context(
      ProceedingJoinPoint proceedingJoinPoint, HermiLogging trace, Object result, long startedAt)
      implements LogUseCase.Context {}

  public static final int SLOW_THRESHOLD = 2000;

  @Override
  protected void doLog(Context context) {
    long duration = (System.nanoTime() - context.startedAt()) / 1_000_000;

    SucceededLog succeededLog = new SucceededLog();
    succeededLog.setEvent((String) MDC.get("event"));
    succeededLog.setTitle(resolveTitle(context));
    succeededLog.setDuration(duration);
    succeededLog.setResult(resolveResult(context.result()));

    Logger logger = LoggerFactory.getLogger(context.proceedingJoinPoint().getTarget().getClass());
    if (duration > SLOW_THRESHOLD) {
      logger.warn(succeededLog.toString());
    } else {
      logger.info(succeededLog.toString());
    }
  }

  /**
   * Resolve result to json string 1. if result is null, return null; 2. if result is Loggable, use
   * toHermiLoggingString method; 3. if fields of arg have HermiLoggingRequired annotation, keep all
   * fields; 4. by default,if result is not null, truncate result to 50 chars, return json string of
   * result. For all fileds annotated as Mask, mask them.
   *
   * @param result
   * @return
   */
  private Object resolveResult(Object result) {
    return normalizer.normalize(result);
  }
}
