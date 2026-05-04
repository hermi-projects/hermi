Marker和Validator实现类必须是线程安全的（Thread-Safe）且无状态的（Stateless）。
框架会缓存并多线程复用此实例。

Same Reason to Change原则拆分文件

以下是为您生成的 Markdown 源码（Raw TXT 格式）。您可以将其完整复制并保存为 `.md` 或 `.txt` 文件。

```text
# Spring Boot 3 全链路安全与可观测性集成方案

本方案集成了 **Hibernate Validator (参数校验)**、**Jackson (自动脱敏)**、**Micrometer (链路追踪)** 以及 **Logstash Encoder (结构化日志)**。

---

## 1. 核心注解：@SSN (组合注解)

通过 `@JacksonAnnotationsInside` 将业务校验与脱敏逻辑打包，实现“一处声明，全局生效”。
```java
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * SSN 组合注解
 * 1. 自动触发 Hibernate Validator 格式检查
 * 2. 自动触发 Jackson 序列化脱敏
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SSNValidator.class)
@JacksonAnnotationsInside
@JsonSerialize(using = SSNJsonSerializer.class)
public @interface SSN {
    String message() default "Invalid SSN format (Expected: 000-00-0000)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

---

## 2. 校验逻辑实现 (Validator)
```java
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SSNValidator implements ConstraintValidator<SSN, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // 建议配合 @NotNull 使用
        // 匹配 000-00-0000 格式
        return value.matches("^\\d{3}-\\d{2}-\\d{4}$");
    }
}
```

---

## 3. 脱敏序列化器 (Serializer)

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class SSNJsonSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null && value.length() >= 4) {
            // 结果示例: ***-**-6789
            gen.writeString("***-**-" + value.substring(value.length() - 4));
        } else {
            gen.writeString("******");
        }
    }
}
```

---

## 4. 日志与链路追踪配置

### A. TraceId 注入 (Micrometer Tracing)
Spring Boot 3 默认使用 Micrometer。在 `application.yml` 中配置后，TraceId 会自动进入 MDC：
```yaml
management:
  tracing:
    sampling:
      probability: 1.0 # 采样率
```

### B. 结构化输出 (Logback)
在 `logback-spring.xml` 中配置 JSON 编码器，方便 Splunk 解析：
```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
    <providers>
        <timestamp/><message/><logLevel/><mdc/><arguments/>
    </providers>
</encoder>
```

---

## 5. 业务实战示例

### DTO 定义
```java
public class UserDTO {
    private String name;

    @SSN // 校验入参，并确保出参(API)和日志均已打码
    private String ssn;
    
    // Getters and Setters
}
```

### Controller 使用
```java
import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@Slf4j
public class UserController {

    @PostMapping("/users")
    public UserDTO createUser(@Valid @RequestBody UserDTO user) {
        // 此时日志输出中的 args.ssn 已被自动脱敏
        log.info("Creating new user", kv("args", user));
        
        return user; // 返回给前端的 JSON 同样已脱敏
    }
}
```

---

## 6. 方案总结

1. **绝对合规**：数据在离开 JVM 边界（打印到磁盘或发送到网络）前已自动打码。
2. **零冗余**：无需手动编写 `mask()` 工具类调用。
3. **可追溯**：通过 Splunk 中的 TraceId 可定位全链路请求，通过 JSON 结构化字段可精确搜索业务参数。
```</String></SSN,>