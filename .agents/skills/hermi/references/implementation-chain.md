# Hermi 完整实施链条：6 Stage，31 Steps

每个 Step 定义四要素：**Goal / Input / Output / Verify**。前一 Stage 的 Gate 不通过，后一 Stage 不开始。

---

## 全景

```
Stage 0        Stage 1       Stage 2       Stage 3        Stage 4        Stage 5
Discovery  →   Process   →   Verify   →   Dispatch   →   Realize   →   Deliver
(6 steps)      (8 steps)     (4 steps)     (4 steps)       (5 steps)      (4 steps)

Hermi: —       Hermi: P1     Hermi: P1     Hermi: —        Hermi: P2      Hermi: P2
```

**Stage** = 实施节奏，**Phase** = 架构分层，不冲突。

---

## Stage 0：Discovery — 发现

**目标**：把模糊的业务需求转化为签收的 Strategic Horizon。

> **ANNOUNCEMENT**: "Entering **Stage 0: Discovery**. I will interview you to map the As-Is reality before writing a single line of code."

### 0.1 Role Identification
| | |
|---|---|
| **Goal** | 确定用户角色与职责 |
| **Input** | 用户描述 |
| **Output** | 角色定义 + 职责范围 |
| **Verify** | 用户确认「这就是我」 |

### 0.2 Task Identification
| | |
|---|---|
| **Goal** | 定位核心业务任务（通过具体近期事件） |
| **Input** | 0.1 的角色信息 |
| **Output** | 一个具体任务实例（含触发条件+参与者） |
| **Verify** | 任务有明确触发事件、执行者和结果 |

### 0.3 Process Deep-Dive
| | |
|---|---|
| **Goal** | 映射 As-Is 步骤流 |
| **Input** | 0.2 的任务实例 |
| **Output** | 完整步骤流（Step 1 → Step 2 → Step 3 → ...） |
| **Verify** | 用户说「对，这就是实际流程」 |

### 0.4 Decision Points
| | |
|---|---|
| **Goal** | 揭示隐藏的 IF-THEN 业务规则 |
| **Input** | 0.3 的步骤流 |
| **Output** | IF-THEN 规则清单 |
| **Verify** | 每条规则有明确条件+动作 |

### 0.5 Exceptions & Errors
| | |
|---|---|
| **Goal** | 识别异常场景与边界情况 |
| **Input** | 0.3–0.4 的步骤流+规则 |
| **Output** | 异常场景清单（含触发条件和处理方式） |
| **Verify** | 每个异常有触发条件+响应策略 |

### 0.6 Confirmation & Sign-off
| | |
|---|---|
| **Goal** | 整理并签收 Strategic Horizon |
| **Input** | 0.1–0.5 的所有产出 |
| **Output** | **签收的 Strategic Horizon**：Actions / Events / Use Cases / Business Rules / System Boundary |
| **Verify** | 用户签字确认全部五项 |

**Gate S0 → S1**：Strategic Horizon 签收。未签收 → 不进 Stage 1。

---

## Stage 1：Process — 编码

**目标**：把 Strategic Horizon 转化为完整的、可编译的 Use Case + JIT 发现的 I/O Contracts。

> **ANNOUNCEMENT**: "Stage 0 complete. Entering **Stage 1: Process** — Blueprint-First Orchestration. I will translate the Strategic Horizon into pure Java."

### 1.1 Establish Boundary
| | |
|---|---|
| **Goal** | 创建 `{Action}{Resource}UseCase` 抽象类 + `Context`(implements Validatable) + `Result` record |
| **Input** | Strategic Horizon 的 Actions + Use Cases |
| **Output** | UseCase 抽象类 |
| **Verify** | Context 字段覆盖所有输入；Result 字段覆盖所有输出；编译通过 |

### 1.2 Skeletal Implementation
| | |
|---|---|
| **Goal** | 创建骨架 `Default{Action}{Resource}UseCase`，`doFulfill` 返回 null |
| **Input** | 1.1 的 UseCase 抽象类 |
| **Output** | `Default{Action}{Resource}UseCase` |
| **Verify** | 编译通过；类名 `Default...UseCase`，不是 `...Impl` 或 `...Service` |

### 1.3 Scoped Domain Model
| | |
|---|---|
| **Goal** | 定义 UseCase 专属的 `{Resource}` record，**不跨 UseCase 共享** |
| **Input** | Strategic Horizon + UseCase 字段 |
| **Output** | `{Resource}` record（package-private），只包含本 UseCase 操作的属性 |
| **Verify** | 无无关字段；无 `@Entity`；无 JPA 注解；无跨 UseCase 复用 |

### 1.4 Main Shell
| | |
|---|---|
| **Goal** | 创建可执行的 `main()` 作为持续验证沙箱 |
| **Input** | 1.2 的 DefaultUseCase |
| **Output** | `{Action}{Resource}Main.java`（含 `public static void main`） |
| **Verify** | `main()` 可执行并打印结果（即使为 null） |

### 1.5 JIT Discovery: Client
| | |
|---|---|
| **Goal** | 当 `doFulfill` 需调外部 API 时，立即创建 Client 契约 |
| **Input** | `doFulfill` 叙事中「我需要从外部拿数据」的精确时刻 |
| **Output** | `{Action}{Resource}Client` extends `Client<Context, Result>`。Context/Result **纯 Java 类型**；**Result implements Validatable** |
| **Verify** | 无 HTTP/REST/gRPC 类型在 Context/Result 中。编译通过 |

### 1.6 JIT Discovery: Repository
| | |
|---|---|
| **Goal** | 当 `doFulfill` 需持久化时，立即创建 Repository 契约 |
| **Input** | `doFulfill` 叙事中「我需要存数据」的精确时刻 |
| **Output** | `{Action}{Resource}Repository` extends `Repository<Context, Result>`。纯 Java 类型；**Result implements Validatable** |
| **Verify** | 无 JPA/JDBC/ORM 类型在 Context/Result 中。编译通过 |

### 1.7 JIT Discovery: Messenger
| | |
|---|---|
| **Goal** | 当 `doFulfill` 需发通知时，立即创建 Messenger 契约 |
| **Input** | `doFulfill` 叙事中「我需要通知别人」的精确时刻 |
| **Output** | `Notify{Fact}Messenger` extends `Messenger<Context, Result>`。纯 Java 类型；**Result implements Validatable** |
| **Verify** | 无 Kafka/JMS/broker 类型在 Context/Result 中。编译通过 |

### 1.8 Holistic Orchestration
| | |
|---|---|
| **Goal** | 注入所有 JIT Contracts，完成 `doFulfill` 叙事。**基础设施异常必须捕获并包装为 DomainException** — `SocketTimeoutException`、`DataAccessException` 禁止泄漏出 `doFulfill` |
| **Input** | 1.5–1.7 的所有 Contracts + 骨架 DefaultUseCase |
| **Output** | 完整的 `doFulfill`：构造函数注入 contracts，业务逻辑，领域异常包装 |
| **Verify** | 叙事完整；`throws` 子句无基础设施异常类型。命名审计：Tense Integrity ✓，Prefix Isolation ✓，Single Action Prophecy ✓ |

**Gate S1 → S2**：`doFulfill` 叙事完整。无技术异常泄漏。命名审计通过。Three Pillars：Boundary intact? Protocol validated? → 进 Stage 2。

---

## Stage 2：Verify — 验证

**目标**：用 stateful local adapters 证明 Phase 1 正确性。**严禁 Mock — 只用有状态的本地实现。**

> **ANNOUNCEMENT**: "Stage 1 complete. Entering **Stage 2: Verify** — Phase 1 Gate. I will build local adapters and verify the logic against real state transitions."

### 2.1 Local Adapters
| | |
|---|---|
| **Goal** | 为每个 Contract 建一个 stateful 本地实现 |
| **Input** | Stage 1 的 Contracts（Client、Repository、Messenger） |
| **Output** | `Local{ContractName}`（Map-based）、`InMemory{ContractName}`（Map-based）、`Console{ContractName}`（System.out）。每个有可编程内部状态 |
| **Verify** | 每个适配器编译通过，继承正确的 Contract，有可注入/设置的内部 Map |

### 2.2 Happy Path
| | |
|---|---|
| **Goal** | 验证正常流程产生正确结果 |
| **Input** | Local Adapters + DefaultUseCase |
| **Output** | Main Shell 中通过的 Happy Path 断言 |
| **Verify** | `result.name().equals("John")` && repo 内部状态反映保存操作。无异常抛出 |

### 2.3 Edge Cases
| | |
|---|---|
| **Goal** | 验证边界和错误条件 |
| **Input** | Happy Path 通过的代码 |
| **Output** | 通过的边界断言：null context、外部返回空、repository 失败 |
| **Verify** | 每个错误场景抛 DomainException，绝不抛技术异常。无 silent null return |

### 2.4 Gate Sign-off
| | |
|---|---|
| **Goal** | 锁定 Contracts 接口 — `doFulfill` 签名不再修改 |
| **Input** | 全部测试通过 |
| **Output** | **冻结的 Phase 1 产物**：UseCase + Contracts + 通过的测试 |
| **Verify** | Contracts 签名不再变动。Three Pillars：Semantics preserved? → 进 Stage 3 |

**Gate S2 → S3**：全部测试绿。Contracts 锁定。不再允许修改 Phase 1。

---

## Stage 3：Dispatch — 派发

**目标**：为每个 Contract 生成精确 Work Order，Phase 2 Worker 能直接执行。

> **ANNOUNCEMENT**: "Stage 2 complete. Entering **Stage 3: Dispatch** — generating Work Orders for Phase 2 realization."

### 3.1 Contract Inventory
| | |
|---|---|
| **Goal** | 列出 Stage 1 发现的所有 I/O Contracts |
| **Input** | 冻结的 Phase 1 产物 |
| **Output** | Contract 清单：每个 Client/Repository/Messenger 一条 |
| **Verify** | 清单与 DefaultUseCase 构造函数参数 1:1 对应 |

### 3.2 Work Orders
| | |
|---|---|
| **Goal** | 为每个 Contract 生成 Work Order YAML |
| **Input** | 3.1 的 Contract 清单 |
| **Output** | `WO-{RESOURCE}-{ACTION}-{SEQ}.yaml`。含：Contract 类名、泛型参数、Context/Result 字段定义、所属 UseCase |
| **Verify** | 每个 Contract 一个 WO；每个 Context/Result 字段已记录 |

### 3.3 Constraint Diffusion
| | |
|---|---|
| **Goal** | 将 Core 层的校验注解传播到 Work Order |
| **Input** | Context 和 Contract.Result 上的 `@NotNull`、`@NotBlank` 等注解 |
| **Output** | WO 的 validation 段：哪些字段必填、格式约束 |
| **Verify** | Shell 实现者看 WO 就知道哪些字段需要校验 |

### 3.4 Error Mapping
| | |
|---|---|
| **Goal** | 将 DomainException 映射到技术处理策略 |
| **Input** | Stage 0.5 的异常清单 + Stage 1.8 的 DomainException |
| **Output** | WO 的 error_mapping 段：`UserNotFoundException` → HTTP 404，`VendorTimeoutException` → retry |
| **Verify** | 每个 DomainException 有对应技术策略 |

**Gate S3 → S4**：每个 Contract 一个完整 WO。全部含 validation spec + error mapping。

---

## Stage 4：Realize — 实现

**目标**：按 Work Orders 建造生产级 Shell 适配器。**Worker agents 实现，你审计。**

> **ANNOUNCEMENT**: "Stage 3 complete. Dispatching Work Orders for **Stage 4: Realize** — Phase 2 construction."

### 4.1 Tech Selection
| | |
|---|---|
| **Goal** | 为每个 Contract 选定具体技术 |
| **Input** | Work Orders |
| **Output** | 每个 WO 标注 target technology：RestTemplate / WebClient，JPA / MyBatis，Kafka / RabbitMQ |
| **Verify** | 技术选型匹配团队能力和项目约束 |

### 4.2 Vendor Client
| | |
|---|---|
| **Goal** | 封装原始 API 调用：`{Vendor}{Resource}Client` |
| **Input** | WO + 技术选型 |
| **Output** | Vendor Client（如 `LexisNexisUserClient` extends `hermi.shell.Client`）。处理 HTTP 认证、headers、序列化 |
| **Verify** | Result implements Validatable；编译通过；处理认证和错误响应 |

### 4.3 Mapper
| | |
|---|---|
| **Goal** | 实现 Domain ↔ Vendor 双向转换：`{Vendor}{Resource}Mapper` |
| **Input** | Vendor Client + Domain Contract |
| **Output** | Mapper implements `Mapper<DomainContext, DomainResult, VendorPayload, VendorResponse>` |
| **Verify** | `toPayload(ctx).field.equals(ctx.field)` — 往返不丢字段 |

### 4.4 Production Adapter
| | |
|---|---|
| **Goal** | 组合 Vendor Client + Mapper 实现 Phase 1 Contract 的 `doFulfill` |
| **Input** | Vendor Client + Mapper + WO |
| **Output** | `{Vendor}{ContractName}`（如 `LexisNexisFindUserClient` extends `FindUserClient`） |
| **Verify** | 继承正确的 Phase 1 Contract；`doFulfill` 调用 `client.exchange(mapper.toPayload(ctx))`；编译通过 |

### 4.5 Auditor
| | |
|---|---|
| **Goal** | 如需持久化审计，实现 `PersistentAuditor` |
| **Input** | Production Adapter + 审计需求 |
| **Output** | `{Vendor}{Resource}Auditor` extends `PersistentAuditor` |
| **Verify** | `doRecordContext`、`doRecordResult`、`doRecordError` 全部实现；审计记录写入 DB |

**Gate S4 → S5**：每个 Phase 1 Contract 有对应 Production Adapter。命名审计：Shell 类有 Tech/Vendor 前缀 ✓。Boundary check：Shell 未污染 Core ✓。

---

## Stage 5：Deliver — 交付

**目标**：接入入口点，集成测试，部署上线。

> **ANNOUNCEMENT**: "Stage 4 complete. Entering **Stage 5: Deliver** — wiring entry points and deploying."

### 5.1 Entry Point
| | |
|---|---|
| **Goal** | 构建外部入口：REST Controller、Kafka Consumer、CLI 或 MCP Server |
| **Input** | Stage 4 的 Production Adapters |
| **Output** | `{Action}{Resource}Controller` / `{Action}{Resource}Consumer` / `{Action}{Resource}Cli` |
| **Verify** | 入口正确转换外部协议 → Context → UseCase.fulfill()；编译通过 |

### 5.2 Wire Dependencies
| | |
|---|---|
| **Goal** | 连接完整 Bean 链路 |
| **Input** | Entry Point + Production Adapters |
| **Output** | `@Configuration` 或自动装配：Controller → Service → Production Adapters → Vendor Clients |
| **Verify** | 应用启动无 `NoSuchBeanDefinitionException` |

### 5.3 Integration Test
| | |
|---|---|
| **Goal** | 端到端验证（真实或 test-container 基础设施） |
| **Input** | 完整 wiring |
| **Output** | 通过的集成测试 |
| **Verify** | HTTP 200 → 数据库有记录 → 消息队列有事件。全部断言通过 |

### 5.4 Deploy
| | |
|---|---|
| **Goal** | 部署上线 |
| **Input** | 集成测试通过的制品 |
| **Output** | 运行中的应用 |
| **Verify** | 生产监控无异常；Health check 绿 |

**Gate S5 → Done**：集成测试通过。生产健康。**Use Case 交付完成。**

---

## 跨 Stage 审计

每个 Gate 执行以下检查：

| Gate | 命名审计（Three Golden Rules） | Three Pillars 检查 |
|---|---|---|
| S0→S1 | — | — |
| S1→S2 | `{Action}{Resource}UseCase` 模式。所有 Contracts 命名正确。实现类 `Default...` | Boundary intact? Protocol validated? |
| S2→S3 | 冻结的 Contracts 命名符合规范 | Semantics preserved? |
| S3→S4 | WO 命名符合 `WO-{RESOURCE}-{ACTION}-{SEQ}` | — |
| S4→S5 | Shell 类有 `{Tech|Vendor}` 前缀。Mapper 命名正确 | Boundary intact?（Shell 未污染 Core） |
| S5→Done | — | — |

---

## 命名规范速查

| 组件 | Stage | 模式 | 示例 |
|---|---|---|---|
| Use Case Contract | S1 | `{Action}{Resource}UseCase` | `FindUserUseCase` |
| Use Case Impl | S1 | `Default{Action}{Resource}UseCase` | `DefaultFindUserUseCase` |
| Scoped Model | S1 | `{Resource}` | `User` |
| Client Contract | S1 | `{Action}{Resource}Client` | `FindUserClient` |
| Repository Contract | S1 | `{Action}{Resource}Repository` | `SaveUserRepository` |
| Messenger Contract | S1 | `Notify{Fact}Messenger` | `NotifyUserFoundMessenger` |
| Main Shell | S1–S2 | `{Action}{Resource}Main` | `FindUserMain` |
| Local Adapter | S2 | `Local{ContractName}` | `LocalFindUserClient` |
| InMemory Adapter | S2 | `InMemory{ContractName}` | `InMemorySaveUserRepository` |
| Console Adapter | S2 | `Console{ContractName}` | `ConsoleNotifyUserFoundMessenger` |
| Work Order | S3 | `WO-{RESOURCE}-{ACTION}-{SEQ}` | `WO-USER-FIND-001` |
| Vendor Client | S4 | `{Vendor}{Resource}Client` | `LexisNexisUserClient` |
| Production Adapter | S4 | `{Vendor}{ContractName}` | `LexisNexisFindUserClient` |
| Mapper | S4 | `{Vendor}{Resource}Mapper` | `LexisNexisUserMapper` |
| Auditor | S4 | `{Vendor}{Resource}Auditor` | `LexisNexisUserAuditor` |
| Controller | S5 | `{Action}{Resource}Controller` | `FindUserController` |
| Consumer | S5 | `{Action}{Resource}Consumer` | `FindUserConsumer` |
| Service | S5 | `{Action}{Resource}Service` | `FindUserService` |
