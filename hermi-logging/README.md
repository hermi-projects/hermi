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
