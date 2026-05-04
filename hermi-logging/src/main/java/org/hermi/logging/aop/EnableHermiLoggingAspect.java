package org.hermi.logging.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hermi.logging.support.RootPackageRegistry;

/** 拦截 {@code @EnableHermiLogging} 标注的类，初始化根包注册。 */
@Aspect
public class EnableHermiLoggingAspect {

  private final RootPackageRegistry packageRegistry = RootPackageRegistry.instance();

  @Around("@within(org.hermi.logging.annotations.EnableHermiLogging) && execution(* *(..))")
  public Object init(ProceedingJoinPoint jp) throws Throwable {
    packageRegistry.initIfNeeded(jp.getSignature().getDeclaringType());
    return jp.proceed();
  }
}
