# Hermi Logging 模块 — 功能要求（Final Version v3）

Hermi Logging 是一个 **纯 Java + AspectJ（CTW）** 的日志模块，通过注解驱动实现业务方法日志记录与敏感字段脱敏。

---

## 🎯 1. 模块目标（Module Goals）

Hermi Logging 的核心目标：

- 自动记录方法入参与返回值
- 自动记录异常信息
- 自动对敏感字段进行脱敏
- 输出结构化 JSON 日志
- 支持包范围控制
- 支持方法级业务事件配置
- 支持嵌套对象脱敏
- 支持集合脱敏
- 零侵入业务代码
- 不依赖任何框架（如 Spring）

---

## 📌 2. 日志触发条件（Trigger Conditions）

日志记录需要满足 **任意一个条件**：

### ✔ 条件 1：方法上有 `@HermiLogging`
- 直接记录日志
- event（业务名称）来自注解
- 子方法继承父方法的 event

### ✔ 条件 2：方法所在包在 `@EnableHermiLogging` 范围内
- 即使方法没有 @HermiLogging，也要记录日志
- event 继承自最近的父方法（如果有）
- 如果没有父方法 event，则 event = null

### ✔ 两者关系总结

| 情况 | 是否记录 | event 来源 |
|------|----------|------------|
| 有 @HermiLogging | 是 | 注解 value |
| 无注解，但在 @EnableHermiLogging 范围内 | 是 | 最近父方法的 event |
| 无注解，且不在范围内 | 否 | 无 |

---

## 🧩 3. event（业务名称）继承规则

### ✔ 3.1 子方法继承父方法的 event
如果父类方法有：

```java
@HermiLogging("UserRegister")
public void saveUser() {}
```
```java
@Override
public void saveUser() {}
```
则子类方法的 event = "UserRegister"。

✔ 3.2 子类可以覆盖父类 event

如果子类方法也加了注解，则以子类为准。

🧱 4. 日志内容要求（Log Content Requirements）

每条日志必须包含：

| 字段 | 说明 |
| --- | --- |
| event | 业务名称（来自注解或继承） |
| method | 完整方法名（class.method） |
| args | 入参（脱敏后） |
| result | 返回值（脱敏后） |
| exception | 异常信息（如有） |
| costMs | 方法执行耗时（毫秒） |

🔐 5. 脱敏要求（Masking Requirements）
✔ 5.1 支持的注解

来自 annotation module：

    @SSN

    @Email

    @Phone

✔ 5.2 支持的对象类型

必须支持以下结构的脱敏：

| 类型 | 是否支持 |
| --- | --- |
| POJO | ✔ |
| 嵌套 POJO | ✔ |
| List | ✔ |
| Map | ✔ |
| 数组 | ✔ |
| 非 String 字段（如 int、long） | ✔（如果有注解） |

✔ 5.3 脱敏策略

    SSN → ***-**-1234

    Email → a***@domain.com

    Phone → 138****78

    HerimiSensitive → a***g

🧠 6. AOP 行为要求（Aspect Requirements）

Aspect 必须：

    在方法执行前记录入参

    在方法执行后记录返回值

    捕获异常并记录, 包含所有信息以利于AI和developer去修复问题

    记录执行耗时

    根据包范围和注解决定是否记录日志

    处理 event 继承

    输出 JSON

Aspect 不得：
* 修改返回值
* 吞掉异常
* 改变业务逻辑执行顺序

📦 7. 输出要求（Output Requirements）
✔ 输出方式

必须使用 logger（如 SLF4J）

✔ 输出格式

结构化 JSON，例如：
START: simple args
```json 
{
  "event": "UserRegister",
  "method": "UserService.register", 
  "status": "START",
  "args": {}
}
```
SUCCESS: simple args, result
```json
{
  "event": "UserRegister",
  "method": "UserService.register", 
  "status": "SUCCESS",
  "args": {},
  "result": {},
  "cost": 12
}
```
FAILED: full args
```json
{
  "event": "UserRegister",
  "method": "UserService.register", 
  "status": "FAILED",
  "args": {},
  "exception": "",
  "trace": ""
}
```

---

## 📝 8. 领域特定叙事 (Domain-Specific Narratives)

Hermi Logging 支持使用 **Java 表达式语言 (Java EL)** 将入参动态绑定到日志中，帮助开发者输出“人类与 AI 均友好”的上下文叙事（Narratives），且完全零依赖于 Spring。

### ✔ 8.1 如何使用

通过 `@HermiLogging` 的 `message` 属性，你可以使用 EL 表达式获取方法的运行参数。支持按照参数名或参数索引（`arg0`, `arg1` 等）进行取值：

```java
@HermiLogging(message = "Processing payment of ${amount} for user ${user.id}")
public Payment processPayment(User user, BigDecimal amount) { 
    // ...业务代码
}
```

### ✔ 8.2 输出效果

当应用上述注解后，日志框架会自动计算 EL 表达式。在输出的 JSON 中，`message` 字段将被替换（或者追加辅助状态词）：
- `Started: Processing payment of 50.00 for user 123`
- `Succeeded: Processing payment of 50.00 for user 123`
- `Failed: Processing payment of 50.00 for user 123`

### ✔ 8.3 安全性与降级

如果在表达式中引用了不存在的属性或导致计算失败，框架会进行 **优雅降级**：直接输出原始的模板字符串并在系统执行过程中规避抛出任何异常。这保障了您的业务代码稳定性。

---

## 🤖 9. 调试友好设计 (Debugging-Friendly Design)

Hermi Logging 框架专门针对“人类排查”和“AI 自动分析”进行了双向结构化设计。

### ✔ 9.1 AI 友好性 (AI-Friendly Data Context)
大语言模型和日志分析引擎（如 ELK, Datadog）更擅长解析具备高熵值与上下文关联的结构化字段：
- **一致的关联追踪 (Correlation ID)：** 通过 `MDC.get("event")` 传播全局 `event`，实现全链路追踪。
- **序列化的原始状态 (Input State)：** 以 JSON 数组格式（而不是拼接字符串）保存抛出异常时的 `args` 快照。
- **精确的代码位置 (Traceability)：** `logger`（完整类名）和 `method` 作为独立字段剥离。
- **核心异常栈 (Root Cause Analysis)：** 直接抽离出 `exception` 类型与 `exception_message`，加速故障定界。

### ✔ 9.2 人类友好性 (Human-Friendly Readability)
研发工程师排查问题时，关注的是易读性、业务意图和故障焦点：
- **领域特定叙事 (Domain Narrative)：** 拒绝冰冷的 "Execution of UserService.process() failed"，通过提取 EL 变量将其转换为语义连贯的："Failed: Processing payment of 50.00 for user 123"。
- **耗时感知 (Performance Alerting)：** 当耗时超阈值时，除将 Severity 动态提升至 `WARN` 外，`message` 中还能体现出时间维度感知。
- **高信噪比 (High Signal-to-Noise Ratio)：** 利用灵活的 `@HermiLogging(maxArgLength = x, maxResultLength = y)` 对巨量冗余返回和超长日志参数进行折叠或摒弃。
