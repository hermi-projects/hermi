# AI Code Factory — 研究报告

> 版本: v0.6
> 日期: 2026-04-30
> 状态: 概念设计阶段

---

## 1. 核心观点

AI Agent 不写新代码，只做两件事：**实现**和**组合**。

### 1.1 Harness = Template Method Pattern

Hermi 的所有基类都是 **代码级 Harness（安全笼）**。它们基于 Template Method Pattern 设计：

```
Base class (harness):
  public final execute(C context) {
      validate(context);               ← 框架处理，Agent 不可改
      R result = doExecute(context);   ← Agent 填入这里
      validate(result);                ← 框架处理，Agent 不可改
      return result;
  }
```

Agent 只需要在 `doExecute` 中填空。最终的 `execute` 方法、自动校验、审计生命周期都是框架提供的**防护栏**，防止 Agent 偏离轨道。

### 1.2 Agent 的工作只有两种

```
模式 1: 实现 (Implement)
  拿到一个 harness → extends → 填写 doExecute → 提交
  例子: LexisNexisFindUserClient extends FindUserClient {
      protected Result doExecute(Context ctx) { ... }
  }

模式 2: 组合 (Compose)  
  拿到多个已实现的 harness → 通过 Mapper 串联 → 注入到 UseCase → 提交
  例子: DefaultFindUserUseCase(client, repo, messenger) {
      protected Result doExecute(Context ctx) {
          var data = client.execute(...);
          repo.execute(...);
          messenger.execute(...);
          return result;
      }
  }
```

不需要 AI 理解架构全貌，不需要 AI 设计类结构。Harness 已经把路铺好了，Agent 只负责走。

---

## 2. Harness 总览

### 2.1 Use Case 层 Harness

| Harness (Type) | 抽象类 | Agent 填什么 |
|----------------|--------|-------------|
| `USE_CASE` | `UseCase<C, R>` | 填写 `doExecute` — 编排业务叙事 |
| `CLIENT` | `Client<C, R>` | 填写 `doExecute` — 实现外部请求逻辑 |
| `REPOSITORY` | `Repository<C, R>` | 填写 `doExecute` — 实现数据存取逻辑 |
| `MESSENGER` | `Messenger<C, R>` | 填写 `doExecute` — 实现通知发送逻辑 |
| `DISPATCHER` | `DispatcherUseCase<C, R>` | 注册 Handler（组合模式） |
| `HANDLER` | `Handler<C, R>` | 填写 `support` + `doExecute` |

每个 Harness 自带：
- `final execute()` — 不可重写的执行入口
- `Validatable` 校验 — 自动检查输入输出
- 输入输出声明 — `Context` / `Result`

### 2.2 Shell 层 Harness

| Harness | Agent 填什么 |
|---------|-------------|
| `shell.Client<P, R>` | 填写 `doExchange` — 具体协议调用 |
| `shell.Messenger<P, R>` | 填写 `doPublish` — 具体消息发送 |
| `Auditor<P, R>` | 填写 `doRecordPayload/Response/Error` — 审计逻辑 |
| `SecureClient<P, R>` | 填写 `doExchange(String)` — 加密后的协议调用 |
| `Mapper<UCtx, URes, SPayload, SRes>` | 填写 `toPayload` + `toResult` — 数据翻译 |

### 2.3 Agent 不做的事

| 不需要做什么 | 为什么不需要 |
|-------------|-------------|
| 设计类结构 | Harness 已经定义好了 |
| 写 try-catch | Harness 的 final execute 已处理 |
| 写日志 | Auditor 已处理 |
| 写 null 检查 | Validatable 已处理 |
| 创建数据库表 | 那是 DBA / 迁移脚本的事 |
| 写配置 | 那是 DevOps / 部署的事 |
| 写 CI 文件 | 那是平台的事 |

Agent 不做任何框架、基础设施、运维相关的工作。它的所有注意力都集中在 **业务逻辑的填空** 上。

---

## 3. 工具箱：Harness 的层次

```
Layer 1: JDK + Hermi 框架
  └── Executor, Validatable, 自动校验

Layer 2: Use Case Harness
  └── UseCase, Client, Repository, Messenger, DispatcherUseCase, Handler

Layer 3: Shell Harness  
  └── Client, Messenger, Mapper, Auditor, Cryptor, SecureClient

Layer 4: 测试 Harness
  └── LocalClient, ConsoleMessenger, InMemoryRepository
```

每一层为上一层提供 Harness。Agent 在某层工作时，只关心这一层暴露的 `doExecute`。

---

## 4. 工单系统

### 4.1 工单定义

```java
WorkOrder {
    String id;          // WO-FINDUSER-CLIENT-IMPL
    Action action;      // IMPLEMENT | TEST | REVIEW | DECISION
    Type tool;          // CLIENT | REPOSITORY | MESSENGER | USE_CASE | ...
    Class<?> contract;  // FindUserClient.class
    String domain;      // "find-user"
}
```

### 4.2 三权分立

| Action | Agent | 做什么 | 不做什么 |
|--------|-------|--------|----------|
| `IMPLEMENT` | Implementer | 填空 + 组合 Harness | 不写测试、不写审查规则 |
| `TEST` | Tester | 验证 Harness 的正确性 | 不写业务逻辑 |
| `REVIEW` | Reviewer | 写 ArchUnit 门禁 | 不参与具体实现 |
| `DECISION` | Human | 做判断和决策 | 不写常规代码 |

### 4.3 一个功能的工单示例

"通过 SSN 从第三方获取用户、存库、发通知":

```
Architect 完成业务分析后，生成以下工单：

[IMPLEMENT] FindUserClient           → Implementer
[TEST]      FindUserClient           → Tester
[IMPLEMENT] SaveUserRepository       → Implementer
[TEST]      SaveUserRepository       → Tester
[IMPLEMENT] NotifyUserFoundMessenger → Implementer
[TEST]      NotifyUserFoundMessenger → Tester
[IMPLEMENT] DefaultFindUserUseCase   → Implementer (组合)
[TEST]      DefaultFindUserUseCase   → Tester
```

每个工单互不依赖，并行执行。

---

## 5. Agent Pool

```
所有工单 → 线程池 → Agent 各自完成 → 各自提交 → 小 PR
```

```java
ExecutorService pool = Executors.newFixedThreadPool(N);

for (WorkOrder wo : workOrders) {
    pool.submit(() -> {
        Skill skill = loadSkill(wo.action);    // 加载技能
        Harness harness = loadHarness(wo.tool); // 加载 Harness
        File output = fillHarness(skill, harness, wo.contract); // 填空
        commit(wo, output);
        createPR(wo);
    });
}
```

反馈不直接传递。CI 失败 → 新工单 → 重新分配。

---

## 6. 关键原则

| 原则 | 说明 |
|------|------|
| **Harness 铺路** | 所有基类都是 Template Method，Agent 只填空 |
| **不写新代码** | Agent 要么 extends，要么 compose |
| **三权分立** | 实现、测试、审查三个角色互不重叠 |
| **文件即隔离** | 不同 Agent 写不同文件，不需要锁或 worktree |
| **小 PR 交付** | 一个工单 = 一个 PR |
| **Human 只决策** | Human 是 Exception Handler |
| **CI 是反馈** | feedback 来自测试失败，不直接传递 |

---

## 7. 实施路线图

| Phase | 内容 | 产出 |
|-------|------|------|
| **A** | 定义 Type 枚举 + Harness 目录自动生成 | 机器可读的 Harness 索引 |
| **B** | 创建 3 个新 Skill | implementer / tester / reviewer |
| **C** | ArchUnit 验证套件 | 自动门禁 |
| **D** | Agent Pool 调度 | 工单分发 + 小 PR |

---

## 8. 开放问题

1. **组合结果的归属**: `DefaultFindUserUseCase` 组合了多个工具，这是 Implementer 的职责还是需要专门的 Assembler？
2. **新 Harness 审批**: Agent 发现没有合适的 Harness 需要新建时，直接创建还是需要 Human 确认？
3. **跨功能复用**: 两个功能用同一个 Mapper 模式，如何共享？
4. **DECISION 触发**: 什么情况下生成决策工单？

---

## 9. 总结

### 9.1 问题本质

传统 AI 辅助编程的困境不在于 AI 写不出代码，而在于**缺少约束**。AI 在空白画布上创作时容易偏离架构轨道、发明不存在的模式、混入基础设施逻辑。Hermi 的解法是：**不给 AI 画布，给它轨道**。

### 9.2 核心答案：Harness

Hermi 的所有基类 (`UseCase`, `Client`, `Repository`, `Messenger`, `DispatcherUseCase`, `Handler`) 本质上是 **Template Method Pattern 的代码级 Harness**。它们通过 `final execute()` 锁死了执行路径，通过 `Validatable` 自动校验输入输出，通过 `doExecute()` 暴露唯一的填空点。

Agent 的工作被严格限定在两个动作内：

| 动作 | 含义 | 代码模式 |
|------|------|----------|
| **实现** | extends Harness → 填写 doExecute | `LexisNexisClient extends Client { doExchange() }` |
| **组合** | 选取多个实现 → 注入到 UseCase | `DefaultFindUserUseCase(client, repo, messenger)` |

不属于这两个动作的事情，Agent 不做——日志、异常处理、空值检查、配置、运维等都属于框架或平台的责任。

### 9.3 分工模型

```
                    ┌─────────────────────────┐
                    │     Architect Agent      │
                    │  理解意图 → 定义契约       │
                    │  生成工单清单              │
                    └──────────┬──────────────┘
                               │
                    ┌──────────▼──────────────┐
                    │      WorkOrder 清单      │
                    │  IMPLEMENT x N          │
                    │  TEST x N               │
                    │  REVIEW x N             │
                    └──────────┬──────────────┘
                               │
                    ┌──────────▼──────────────┐
                    │       Agent Pool         │
                    │  (线程池，并行执行)        │
                    └──────────┬──────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
     Implementer          Tester           Reviewer
     实现 Harness         验证行为          写门禁规则
     产出 PR              产出 PR           产出 PR
              └────────────────┼────────────────┘
                               ▼
                          CI 合并
                       功能交付完成
```

三权分立确保没有人能同时做裁判和运动员。反馈不直接传递，而是通过 CI 失败 → 新工单的闭环完成。

### 9.4 为什么不需要复杂调度

| 问题 | 答案 |
|------|------|
| 文件冲突怎么办？ | 每个 Agent 写不同的文件，天然隔离 |
| 需要分布式锁吗？ | 不需要，没有共享可写状态 |
| 需要 WorkTree 吗？ | 不需要，文件即隔离边界 |
| Agent 之间怎么通信？ | 不通信，通过工单和 CI 间接协作 |
| 需要消息队列吗？ | 不需要，线程池就够了 |
| 工单谁生成？ | Architect Agent 在完成业务分析后 |

### 9.5 一句话总结

```
Agent 不写代码 → Agent 实现和组合 Harness
Harness 约束行为 → 代码质量可预测
工单驱动分工 → 多个小 PR 并行交付
```

Hermi 的 AI Code Factory 不是要让 AI 更聪明——而是让 AI 在**正确的轨道上**工作。Harness 是轨道，工单是路标，Agent Pool 是列车。轨道铺到哪里，列车就开到哪里。不会脱轨，不需要调度员，每一趟车都到达同一个终点：**符合架构约束的可交付代码**。
