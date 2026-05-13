# Hermi 日志系统 PRD

## 1. 产品目标

让 AI（和人类）通过日志**快速定位 bug 根因**。两套系统各司其职：

- **LogAuditor**：告诉 AI "哪个执行单元崩了，输入是什么，输出了什么"
- **HermiLogging**：告诉 AI "崩在执行单元内部的哪个方法，参数是什么，调了多久"

两个系统共享同一个 `traceId`（由外部 tracing library 注入 MDC），可以串联分析。

## 2. 系统架构

```
请求进入
  │  traceId 由外部 tracing（OpenTelemetry 等）注入 MDC
  │
  ├─ LogAuditor（自动，每个 Executor 都有）
  │   ├─ STARTED:   {contextId, context(masked)}
  │   ├─ SUCCEEDED: {contextId, result(masked)}
  │   └─ FAILED:    {contextId, context(masked), exceptionClass, exceptionMessage, stackTrace}
  │
  └─ HermiLogging（opt-in，@HermiLogging 注解标记入口）
      ├─ STARTED:  "{label} - STARTED"
      ├─ FINISHED: {duration} "{label} - FINISHED"
      └─ FAILED:   {args(masked), duration, exceptionClass, exceptionMessage, stackTrace} "{label} - FAILED"
```

## 3. LogAuditor — Executor 级 I/O 审计

### 3.1 定位

| 维度 | 说明 |
|------|------|
| 消费方 | AI（开发时自修复回路） |
| 触发方式 | 自动，每个 `Executor.execute()` 都触发 |
| 日志级别 | INFO（STARTED/SUCCEEDED），ERROR（FAILED） |
| 携带字段 | 技术字段（contextId, context, result, exception） |

### 3.2 生命周期事件

| 事件 | 级别 | 数据 |
|------|------|------|
| STARTED | INFO | `contextId`（UUID），`context`（MaskMapper 序列化后的 JSON） |
| SUCCEEDED | INFO | `contextId`（同 STARTED），`result`（MaskMapper 序列化后的 JSON） |
| FAILED | ERROR | `contextId`，`context`（全量），`exceptionClass`，`exceptionMessage`，stackTrace（via setCause） |

### 3.3 安全机制

- 敏感字段自动掩码：通过 `MaskMapper`（Jackson + `@Mask`/`@SSN` 注解）
- 审计异常不影响主流程：`Auditor` 基类的 `recordContext/recordResult/recordError` 都是 `final`，内部 try-catch 吃掉审计异常

### 3.4 实现位置

| 文件 | 职责 |
|------|------|
| `hermi-commons/.../audit/Auditor.java` | 抽象基类，定义生命周期 + 安全兜底 |
| `hermi-commons/.../audit/LogAuditor.java` | 默认实现，SLF4J 结构化输出 |
| `hermi-commons/.../Executor.java` | 基类，持有 LogAuditor，`execute()` 编排生命周期 |
| `hermi-shell/.../audit/PersistentAuditor.java` | 持久化审计扩展点（生产合规审计用） |

## 4. HermiLogging — 方法级业务追踪

### 4.1 定位

| 维度 | 说明 |
|------|------|
| 消费方 | 人 + AI（运维、业务分析、debug） |
| 触发方式 | opt-in，`@HermiLogging` 注解标记入口方法/类 |
| 日志级别 | INFO（STARTED/FINISHED），ERROR（FAILED） |
| 携带字段 | 技术字段（args, duration, exception）+ 业务标签（EL 表达式） |

### 4.2 注解系统

**`@EnableHermiLogging`** — 圈定追踪范围
- 放在入口类（main class）上
- `value()` 可选：分号分隔的根包列表。默认用标注类所在的包
- 只有根包及子包内的非 private 方法才会被追踪

**`@HermiLogging`** — 标记追踪入口
- 放在方法上：只追踪该方法（及其调用链上的下游方法）
- 放在类上：该类的所有非 private 方法都是入口
- `message()` 可选：支持 EL 表达式 `${paramName}`，用于注入业务标签（如 `"退款单 ${request.orderId}"`）

### 4.3 链式传播

```
@HermiLogging(message="退款单 ${request.orderId}")  ← 入口，自定义 label
  └─ validate(request)                                ← 自动追踪，label="PaymentService.validate()"
       └─ checkBalance(account)                       ← 自动追踪，label="PaymentService.checkBalance()"
```

- 只有 `@HermiLogging` 注解的方法启动追踪链
- 链内所有下游方法（同根包、非 private）自动追踪，无需注解
- 使用 `ThreadLocal<Integer>` 计数器（非 boolean），支持嵌套 `@HermiLogging` 调用
- **限制**：ThreadLocal 在异步边界（CompletableFuture 等）会断链。现阶段只支持同步调用

### 4.4 生命周期事件

| 事件 | 级别 | 数据 |
|------|------|------|
| STARTED | INFO | `"{label} - STARTED"` |
| FINISHED | INFO | `duration`（毫秒）`"{label} - FINISHED"` |
| FAILED | ERROR | `args`（MaskMapper 掩码后的方法参数），`duration`（毫秒），`exceptionClass`，`exceptionMessage`，stackTrace（via setCause）`"{label} - FAILED"` |

### 4.5 实现位置

| 文件 | 职责 |
|------|------|
| `hermi-logging/.../annotations/EnableHermiLogging.java` | 根包范围注解 |
| `hermi-logging/.../annotations/HermiLogging.java` | 追踪入口注解 |
| `hermi-logging/.../aop/HermiLoggingAspect.java` | AspectJ 切面，拦截非 private 方法 |
| `hermi-logging/.../aop/EnableHermiLoggingAspect.java` | 辅助切面，惰性初始化根包注册表 |
| `hermi-logging/.../support/RootPackageRegistry.java` | 根包注册表单例 |
| `hermi-logging/.../support/HermiLoggingTracer.java` | 核心引擎：注解解析、EL 消息渲染、链深度管理、日志输出 |

## 5. 横切关注点

### 5.1 数据掩码

`MaskMapper`（Jackson `JsonMapper` + `MaskModule`）统一处理敏感字段脱敏。`LogAuditor` 和 `HermiLogging` 都通过它序列化数据，`@Mask`/`@SSN` 注解的字段自动脱敏。

### 5.2 traceId

由外部 tracing library（OpenTelemetry 等）通过 MDC 注入。LogAuditor 自产 `contextId`（UUID）作为 fallback。两层通过 `traceId` 串联同一次执行的所有日志。

### 5.3 日志格式

使用 SLF4J 2 fluent API（`addKeyValue`）附加结构化字段。配合 `LoggingEventCompositeJsonEncoder`（Logback）输出为 JSON，供 Kibana/Loki/AI 消费。

## 6. 当前实现状态

| 功能 | LogAuditor | HermiLogging |
|------|-----------|-------------|
| 输入记录 | ✅ STARTED/FAILED（context, masked） | ✅ FAILED（args, masked） |
| 输出记录 | ✅ SUCCEEDED（result, masked） | —（设计决定：不记录返回值） |
| 异常信息 | ✅ exceptionClass/Message/stackTrace | ✅ 同 |
| duration | — | ✅ Instant.now() 计时，毫秒 |
| traceId 从 MDC 读 | —（自产 UUID） | — |
| 业务标签（EL） | — | ✅ |
| 链式传播 | — | ✅ |
| 异步支持 | — | —（ThreadLocal，不同步处理） |

## 7. 待办（按优先级）

1. **LogAuditor 加 duration** — `doRecordResult`/`doRecordError` 计算已用时间
2. **traceId 从 MDC 读取** — 两个系统都从 `MDC.get("traceId")` 拿，fallback 自产 UUID，并作为 key-value 写入结构化日志
3. **堆栈截断** — FAILED 日志的 stackTrace 截断为"项目帧优先 + 前 10 帧"
4. **业务字段**（event, domain, resource, initiator, idempotencyKey）— 生产排查需要时再加
5. **logback-spring.xml** — 配置 `LoggingEventCompositeJsonEncoder` 输出 JSON
