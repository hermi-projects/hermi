package org.hermi.logging.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记“trace 根”的入口类（通常是 main class）。
 *
 * <p>同一个 org 前缀下的所有非 private 方法都会被 trace。
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableHermiLogging {

  /** 可选：手动指定根包。 默认：使用标注类所在的包。 */
  String value() default "";
}
