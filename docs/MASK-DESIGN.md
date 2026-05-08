# Mask 模块设计文档

## 设计理念

**声明式脱敏** — 通过注解声明"哪些字段需要脱敏"，而非在业务代码中手动调用脱敏方法。
核心思想是将"脱敏"这个横切关注点从业务逻辑中分离，让开发者只需关心"标不标注解"，无需关心"怎么脱敏"。

```
业务对象                    序列化输出
┌──────────────┐          ┌──────────────────┐
│ User         │  Jackson  │ {                │
│  name: Alice │  ──────>  │   "name": "Alice"│
│ @SSN ssn     │  +Mask   │   "ssn":"***-**-  │
│   :123-45-   │  Module  │   6789"           │
│   6789       │          │ }                 │
└──────────────┘          └──────────────────┘
     │                           ▲
     │ 声明                   自动拦截
     ▼                           │
  @SSN 注解               MaskModule 扫描元注解
  标注了要脱敏            @Constraint → 注入 Serializer
```

## 三层架构

### 1. 注解层 (`hermi-annotations`)

**职责：** 只负责"标记"。标注在 POJO 字段或方法参数上，告诉框架"这个字段要脱敏"。

- `@Mask` — 通用脱敏注解，通过 `@Constraint(maskedBy = MaskSerializer.class)` 桥接
- `@SSN` — 专门用于 SSN 的脱敏注解，同时也是一个 `jakarta.validation` 约束
- `@Constraint` — 元注解，是连接"注解"和"序列化器"的桥梁

### 2. 序列化层 (`hermi-annotations`)

**职责：** 只负责"怎么脱"。实现 Jackson 3 的 `ValueSerializer` 接口，定义具体的脱敏算法。

- `MaskSerializer` — 通用脱敏器：保留首尾字符，中间替换为 `*`（如 `hello` → `h***o`）
- `SSNSerializer` — SSN 脱敏器：从右往左数，仅 mask 超过最后 4 位的数字位（如 `123-45-6789` → `***-**-6789`）

### 3. 注册层 (`hermi-commons`)

**职责：** 负责"何时触发"。在 Jackson 序列化时自动拦截带注解的字段，注入对应的序列化器。

- `MaskModule` — Jackson `SimpleModule`，通过 `ValueSerializerModifier` 在序列化时扫描每个 bean 属性，查找 `@Constraint` 元注解，若找到则替换默认序列化器
- `MaskMapper` — 单例外观，包装注册了 `MaskModule` 的 `JsonMapper`，提供 `mask(Object)` 方法一键输出脱敏 JSON

## 核心机制：元注解桥接

```
@Mask                               @SSN
  │                                   │
  │ @Constraint(                      │ @Constraint(
  │   maskedBy=MaskSerializer.class)  │   maskedBy=SSNSerializer.class)
  │                                   │
  ▼                                   ▼
Constraint.maskedBy() ────────> 实例化 Serializer 并注入 BeanPropertyWriter
```

这种设计借鉴了 `jakarta.validation.Constraint(validatedBy=...)` 的模式，核心优势在于：

- **可扩展：** 任何新注解只需用 `@Constraint` 元注解标记即可接入框架
- **零耦合：** 注解和序列化器之间只有配置关系，没有硬编码映射
- **运行时发现：** `MaskModule` 在编译期不感知具体注解，运行时通过反射读取元注解

## 执行流程

```
MaskMapper.mask(obj)
  │
  ├─ JsonMapper (已注册 MaskModule)
  │     │
  │     └─ ValueSerializerModifier.changeProperties()
  │           │
  │           ├─ 遍历所有 BeanPropertyWriter
  │           │     │
  │           │     ├─ 检查 getter 注解 → 找 @Constraint
  │           │     └─ 检查 field 注解 → 找 @Constraint
  │           │           │
  │           │           └─ 找到则 assignSerializer(序列化器实例)
  │           │
  │           └─ 返回修改后的 properties 列表
  │
  └─ writeValueAsString → 每个字段使用已分配的序列化器输出
```

## 如何添加新注解

添加一个新的脱敏类型只需三步，不需要修改任何现有基础设施代码。

### 步骤一：写 Serializer

在 `hermi-annotations/src/main/java/org/hermi/annotations/serializers/` 下创建：

```java
package org.hermi.annotations.serializers;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class EmailSerializer extends StdSerializer<String> {

  public EmailSerializer() {
    super(String.class);
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializationContext ctxt)
      throws JacksonException {
    if (value == null) {
      gen.writeString((String) null);
      return;
    }
    int at = value.indexOf('@');
    if (at <= 1) {
      gen.writeString(value);
      return;
    }
    // "user@example.com" → "u***@example.com"
    String masked = value.charAt(0) + "*".repeat(at - 1) + value.substring(at);
    gen.writeString(masked);
  }
}
```

**要求：** 必须有一个无参构造函数（`MaskModule` 通过反射实例化时会调用）。

### 步骤二：写注解

在 `hermi-annotations/src/main/java/org/hermi/annotations/` 下创建：

```java
package org.hermi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hermi.annotations.serializers.EmailSerializer;

@org.hermi.annotations.mask.Constraint(maskedBy = EmailSerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailMask {}
```

**关键：** 必须用 `@Constraint(maskedBy = YourSerializer.class)` 元注解标记。

### 步骤三：使用

```java
public class User {
  private String name;
  @EmailMask private String email;
}
```

```java
MaskMapper.instance().mask(user);
// → {"name":"Alice","email":"a***@example.com"}
```

### 可选：同时添加 Bean Validation 支持

参考 `@SSN` 的模式，可以让一个注解同时承担脱敏和校验职责：

```java
@Documented
@jakarta.validation.Constraint(validatedBy = EmailValidator.class)
@org.hermi.annotations.mask.Constraint(maskedBy = EmailSerializer.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailMask {
  String message() default "Invalid email";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
```

## 注意事项

- Serializer 必须是 **无参构造函数**，因为 `MaskModule` 通过反射 `getDeclaredConstructor().newInstance()` 实例化
- Serializer 会被 **缓存**（`ConcurrentHashMap`），每个类只实例化一次，无需担心性能
- 注解的 `@Retention` 必须是 `RUNTIME`，否则运行时无法读取
- 如果字段和 getter 上都标注了解，`MaskModule` 会优先使用 getter 上的
