package org.hermi.logging.usecase;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hermi.logging.annotations.HermiLogging;
import org.hermi.logging.commons.LogUseCase;
import org.hermi.logging.usecase.entity.StartedLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogStartedLogUseCase extends LogUseCase<LogStartedLogUseCase.Context> {
  public static record Context(ProceedingJoinPoint proceedingJoinPoint, HermiLogging trace)
      implements LogUseCase.Context {}

  @Override
  protected void doLog(Context context) {

    ProceedingJoinPoint pjp = context.proceedingJoinPoint();

    StartedLog startedLog = new StartedLog();
    startedLog.setTitle(resolveTitle(context));
    startedLog.setArgs(resolveArgs(pjp.getArgs()));
    Logger logger = LoggerFactory.getLogger(pjp.getTarget().getClass());
    logger.info(startedLog.toString());
  }

  /**
   * Resolve args to json string 1. if args is null, return null; 2. if args is empty, return null;
   * 3.for each arg, if arg is Loggable, use toHermiLogString method; 4. if fields of arg have
   * HermiLoggingRequired annotation, keep all fields; 5. by default,if args is not empty, truncate
   * each arg to 50 chars, bind them togeter, return json string of args. For all fileds annotated
   * as Mask, mask them.
   *
   * @param args
   * @return
   */
  private Object resolveArgs(Object[] args) {
    return normalizer.normalize(args);
  }
}
