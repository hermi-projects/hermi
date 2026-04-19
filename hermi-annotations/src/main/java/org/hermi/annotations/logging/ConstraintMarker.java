package org.hermi.annotations.logging;

import java.lang.annotation.Annotation;

/**
 * 约束标记器接口。 *
 *
 * <p><b>实现指南：</b> 1. 实现类必须提供一个公共无参构造函数。 2. <b>无状态要求：</b> 实现类必须是线程安全的、无状态的。框架会根据注解配置缓存实例，
 * 并在多线程环境下复用。请勿在类成员变量中存储业务数据或请求上下文。 3. {@link #initialize(Annotation)} 仅用于根据注解属性设置转换逻辑的基础配置。
 * * @param <A> 关联的注解类型
 *
 * @param <T> 被标记字段的类型
 */
public interface ConstraintMarker<A extends Annotation, T> {

  default void initialize(A constraintAnnotation) {}

  String mark(T value);
}
