package org.hermi.logging.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hermi.logging.support.RootPackageRegistry;

/** 在 {@code @EnableHermiLogging} 标注的类被加载时触发一次，注册根包。 */
@Aspect
public class EnableHermiLoggingAspect {

  private final RootPackageRegistry packageRegistry = RootPackageRegistry.instance();

  @Before("staticinitialization(@org.hermi.logging.annotations.EnableHermiLogging *)")
  public void init(JoinPoint.StaticPart jp) {
    packageRegistry.initIfNeeded(jp.getSignature().getDeclaringType());
  }
}
