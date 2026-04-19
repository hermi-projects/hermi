package org.hermi.logging.usecase;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hermi.logging.annotations.HermiLogging;
import org.hermi.logging.commons.LogUseCase;
import org.hermi.logging.usecase.entity.FailedLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LogFailedLogUseCase extends LogUseCase<LogFailedLogUseCase.Context> {
  public static record Context(
      ProceedingJoinPoint proceedingJoinPoint, HermiLogging trace, Throwable throwable)
      implements LogUseCase.Context {}

  @Override
  protected void doLog(Context context) {
    ProceedingJoinPoint pjp = context.proceedingJoinPoint();
    Throwable throwable = context.throwable();

    FailedLog failedLog = new FailedLog();
    failedLog.setEvent((String) MDC.get("event"));
    failedLog.setTitle(resolveTitle(context));
    failedLog.setArgs(resolveArgs(pjp.getArgs()));
    failedLog.setExceptionClass(throwable.getClass().getName());
    failedLog.setExceptionMessage(throwable.getMessage());
    failedLog.setStackTrace(
        Arrays.stream(throwable.getStackTrace()).map(StackTraceElement::toString).toList());
    Logger logger = LoggerFactory.getLogger(pjp.getTarget().getClass());
    logger.error(failedLog.toString(), throwable);
  }

  /**
   * Resolve args to json string 1. if args is null, return null; 2. if args is empty, return null;
   * 3. by default,if args is not empty, return json string of args. For all fileds annotated as
   * Mask, mask them.
   *
   * @param args
   * @return
   */
  private String resolveArgs(Object[] args) {
    return normalizer.normalize(args, false).toString();
  }
}
