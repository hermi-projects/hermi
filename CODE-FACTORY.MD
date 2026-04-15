# Hermi Code Factory

这是一份针对 **AI 驱动代码工厂 (Hermi Code Factory)** 的完整设计构想。这套设计通过严密的工程约束，将不可控的 AI 生成转变为可预测的工业化流水线。

---

## 🏗️ 1. 核心架构：三层金字塔模型

整个工厂由一个常驻大脑、一个动态任务池和一套物理隔离的执行环境组成。

### **塔尖：总控层 (Intake Agent)**
* **角色身份**：常驻 CEO / 首席产品经理 / 软件架构师。
* **核心职责**：负责与人类进行高维语义沟通，确认原始商业意图，并进行战略级的工程解构。
* **动作**：
    1. **领域划分 (Bounded Contexts)**：面对庞大系统，避免将需求直接打散为无序代码，优先界定系统的业务边界，拆分为低耦合的子域（如订单域、客户域）。
    2. **用例编排 (Use Case Generation)**：基于领域边界，再向下将业务流程精准细化为高内聚的 **Use Cases**。
    3. **工单下发**：在 Maven 体系下规划整体骨架拓扑，向下推入初始架构工单。

### **中层：编排层 (Worker - Orchestration Mode)**
* **角色身份**：逻辑设计者 (Designer)。
* **核心职责**：在特定 Maven Module 内编写业务内核 (`DefaultUseCase`)。
* **动态发现**：在编码过程中“即时发现”并定义 I/O 契约（`Repository/Client/Messenger`），并将这些零件的填充任务以**子工单**形式抛回队列。

### **底层：执行层 (Worker - Implementation Mode)**
* **角色身份**：零件加工员 (Implementer)。
* **核心职责**：无状态的“填空题”专家。只负责实现编排层定义的抽象接口。
* **双态切换**：Phase 1 负责内存态 Mock，Phase 2 负责生产态基础设施落地。

---

## 📜 2. 核心运行机制：README 驱动演进 (RDD)

该设计弃用了 AI 的“长短期记忆”，改用**物理文档**作为唯一记忆体。

1.  **纯函数式演进范式**：每一次工程轮询都不再是聊天上下文中的无序“打补丁”，而是被严格封装为一次纯函数执行。其中当前的代码库状态 ($code$)、唯一的真理文档 ($readme$) 和本次验证反馈 ($feedback$) 组成严格的三元入参。在此模型下，LLM 仅仅扮演函数 $f()$ 的推演引擎。

$$
code_{n+1} = f(code_n, readme, feedback_n)
$$

2.  **README 即最终态契约**：`README.md` 并不记录“如何走”，只记录“必须是什么” (所有 I/O 契约、流程分支和边界条件)。
3.  **系统级 Gap 消除机制**：流水线的脉动完全由这三元入参驱动。Worker 的任务永远是：计算 $readme$ (期望) 与 $code$ (现实) 之间的误差向量 $\rightarrow$ 全量推演并输出 $code$ 以消除鸿沟。
4.  **异步清单 (Manifest) 脱困机制**：AI 在执行 $f()$ 时遇到信息缺失，不中断流水线，而是基于最佳猜测构建逻辑，并将不确定性暴露至清单（转化为下一轮的负向 $feedback$ 等待人类排险）。


---

## 🤝 3. 人机同构的工单调度系统 (Human-AI Isomorphic Dispatch)

在处理复杂的业务逻辑时，将人类抽象为系统中的一种特殊 Worker，与 AI Worker 共享同一个底层契约和调度生命周期。

1. **统一接口与异步非阻塞调度**：工单拥有统一的状态机。当 AI Worker 遇到无法推理的业务歧义（如外部鉴权逻辑缺失），绝不原地阻塞等待。它将保存当前的代码中间态 ($code_n$)，生成一个 `HUMAN_INTERVENTION` 类型的子工单抛入消息队列，自身进入挂起状态并释放算力去处理其他解耦任务。
2. **结构化的人类交互契约**：交给人类 Worker 的工单不是自由对话框，而是“附带上下文的结构化选项或表单”（即 Manifest）。人类的批复动作被限制为做出决策，并在底层被系统序列化为合法的 $feedback$ 入参或引发 $readme$ 的直接修改。
3. **代理升级与异常兜底**：在这套工厂流水线中，人类主要扮演“最高权限的 Exception Handler”。只有当系统触发不可修复的重构震荡，或遇到战略维度的逻辑矛盾时，任务才会被路由到人类的专属工作队列 (Queue)。从系统宏观视角看，人类批阅清单的过程，本质上就是触发并完成了 Worker 的 `execute()` 回调，重新唤醒被挂起的下游 AI Worker。

---

## 🏁 4. 阶段性交付与验证

* **Phase 1：内核验证 (Pure Java)**
    * 在完全不依赖 Spring 等基础设施的情况下，通过 `MainShell` 跑通所有业务流。
    * **终点**：达成人类满意的收敛状态，打下 **Git Tag: `phase-1-final`**。
* **Phase 2：外壳落地 (Infrastructure)**
    * 基于已冻结的内核标签，机械化填充具体技术实现。

---

## ⚠️ 5. 关键 AI 坑点与防御策略

虽然架构精妙，但在实施过程中需警惕以下 AI 特有的陷阱：

### **A. 语义陷阱 (Semantic Drifts)**
* **坑点**：AI 定义的字段名虽然符合 Java 语法，但与外部真实 API 语义不合。
* **防御**：在契约中强制使用显式枚举 (Enums) 或状态码定义，减少 `String` 或 `Integer` 的滥用。

### **B. 懒惰倾向 (Lazy Coding)**
* **坑点**：随着迭代次数增加，AI 可能只输出部分代码或留下 `// ... code as before` 注释。
* **防御**：**全量覆写策略**。强制 Worker 每次必须输出完整的类源文件；利用 AST 检查代码是否完整覆盖了 README 定义。

### **C. 依赖蔓延 (Dependency Creep)**
* **坑点**：AI 惯性地在 Phase 1 模块中引入外部库（如 Apache Commons, Spring Utils）。
* **防御**：在 Maven 中配置 **Enforcer Plugin**。严格限制 Phase 1 的类路径，除 `java.base` 外禁止引入任何第三方包。

### **D. 重构震荡 (Oscillation)**
* **坑点**：在多轮迭代中，AI 为了满足新反馈而破坏了旧逻辑，导致问题在不同版本间反复。
* **防御**：**回归测试闭环**。每一轮通过的 `MainShell` 用例必须沉淀为自动化断言；Intake Agent 必须对比 README 历史差异，明确告知本次修改的增量目标。



---

## 💡 设计总结

你的设计本质上是建立了一个**“自校准的逻辑过滤器”**：
1.  **Intake Agent** 过滤了需求的模糊性。
2.  **README** 锚定了逻辑的确定性。
3.  **Maven Module** 限制了副作用的扩散。
4.  **MainShell** 验证了运行的准确性。

**最终产出**：不仅是代码，而是一套**与文档 100% 同步、通过物理验证、且不依赖特定技术框架的纯净业务资产。**

```mermaid
graph TD
    %% 强制全局直角连线

    %% 1. 内廷决策层：皇帝与中书省（定旨）
    subgraph Inner_Palace [内廷: 决策与定旨]
        User((皇帝/用户)) -- "1.原始意图" --> Intake[中书省: Intake Agent]
    end

    %% 2. 监察层：门下省（审核与验封）
    subgraph Menxia_Audit [门下省: 独立监察/质检]
        Rules_Audit{准入审核/Rules}
        MainShell[物理验封/MainShell]
        Final{判定}
    end

    %% 3. 中枢调度层：尚书省与任务池（管理）
    subgraph Central_Hub [中枢: 调度与任务池]
        Works[尚书省: 调度总管]
        Pool[(任务池: Task Pool)]
    end

    %% 4. 外朝执行层：六部一体（生产）
    subgraph Six_Ministries [外朝: 六部生产线]
        direction TB
        Minister_Core[吏部: UseCase/逻辑立法]
        Repo[户部: Repository/存储]
        Client[礼部: Client/外部接口]
        Msg[工部: Messenger/通讯基建]
        Other[刑/兵部: Security/Error]
    end

    %% --- 核心公文流转逻辑 ---

    %% 决策流向监察
    Intake -- "2.起草README" --> Rules_Audit
    Rules_Audit -- "封驳" --> Intake

    %% 监察过审流向中枢
    Rules_Audit -- "3.盖章下发" --> Works
    Works -- "4.登记并入库" --> Pool

    %% 中枢分发任务至外朝
    Pool -. "5.派发UseCase任务" .-> Minister_Core
    Minister_Core -- "6.签发能力契约" --> Works

    %% 中枢二次调度
    Works -- "7.生成零件工单" --> Pool
    Pool -. "8.派发实现任务" .-> Repo
    Pool -. "8.派发实现任务" .-> Client
    Pool -. "8.派发实现任务" .-> Msg

    %% 外朝复命流向监察验封
    Six_Ministries -- "9.各部提交实现" --> Works
    Minister_Core -- "10.提交核心逻辑" --> MainShell
    Works -- "11.装配零件" --> MainShell

    %% 最终判定
    MainShell -- "12.运行断言" --> Final
    Final -- "通过" --> Delivery[正式发布/交付]
    Final -- "封驳" --> Works

    %% 样式美化
    style Inner_Palace fill:none,stroke:#333,stroke-width:2px
    style Menxia_Audit fill:none,stroke:#cf1322,stroke-width:2px,stroke-dasharray: 5 5
    style Central_Hub fill:none,stroke:#333,stroke-width:2px,stroke-dasharray: 5 5
    style Six_Ministries fill:none,stroke:#333,stroke-width:2px

    style User fill:#fff,stroke:#333,stroke-width:2px
    style Rules_Audit fill:#fff,stroke:#333,stroke-width:2px
    style MainShell fill:#fff,stroke:#333,stroke-width:2px
    style Works fill:#fff,stroke:#333,stroke-width:2px
    style Minister_Core fill:#f9f9f9,stroke:#333,stroke-width:2px
```

```mermaid
sequenceDiagram
    autonumber
    participant E as 皇帝 (User)
    participant ZSL as 中书令 (主官)
    participant ZSR as 中书舍人 (负责人)
    participant SZ as 侍中 (主官)
    participant LGSZ as 吏科给事中 (负责人)
    participant HGSZ as 户科给事中 (负责人)
    participant L1GSZ as 礼科给事中 (负责人)
    participant GGSZ as 工科给事中 (负责人)
    participant XGSZ as 刑科给事中 (负责人)
    participant BGSZ as 兵科给事中 (负责人)
    participant SSL as 尚书令 (主官)
    participant LBBS as 吏部尚书 (负责人)
    participant HBBS as 户部尚书 (负责人)
    participant L1BBS as 礼部尚书 (负责人)
    participant GBBS as 工部尚书 (负责人)
    participant XBBS as 刑部尚书 (负责人)
    participant BBBS as 兵部尚书 (负责人)

    Note over E, ZSR: 【Phase 0: 定旨 - 统一出口】
    E->>ZSL: 1. 传达意图
    ZSL->>ZSR: 2. 【分发】指派编写 README
    ZSR-->>ZSL: 3. 提交 README 草案
    ZSL->>SZ: 4. 【正式外发】移交草案至门下省

    Note over SZ, GSZ: 【Phase 1: 检察 - 垂直管理】
    SZ->>GSZ: 5. 【分发】要求各科编写断言并检察
    GSZ-->>SZ: 6. 提交 TestShell 报告与意见
    alt 逻辑封驳
        SZ-->>ZSL: 7a. 【正式封驳】发回重修
    else 检察通过
        SZ->>SSL: 7b. 【正式下发】颁布实施诏令
    end

    Note over SSL, BBS: 【Phase 2: 实施 - 集中调度】
    rect rgb(245, 255, 250)
        SSL->>SSL: 8. 初始化 MainShell 自证环境
        SSL->>BBS: 9. 【分发】指派各部具体编码任务
        Note over BBS: 吏部实现契约，户礼工实现逻辑
        BBS-->>SSL: 10. 【汇总】向尚书令复命
        SSL->>SSL: 11. 亲自执行 MainShell 集成自测
    end
    SSL->>SZ: 12. 【呈报】提交集成产物

    Note over SZ, GSZ: 【Phase 3: 终审 - 独立审计】
    SZ->>GSZ: 13. 【分发】要求启动终审断言
    GSZ-->>SZ: 14. 提交最终审计报告
    alt 断言失败
        SZ-->>SSL: 15a. 【封驳】依据罪证，命令重修
    else 通过
        SZ->>E: 15b. 颁行天下 (Delivery)
    end
```

```mermaid
sequenceDiagram
    autonumber
    participant E as 皇帝 (User)
    participant ZS as 中书省 (设计)
    participant MX as 门下省 (监察)
    participant SS as 尚书省 (实施)

    Note over E, ZS: 【Phase 0: 需求规格化】
    E->>ZS: 1. 传达意图 (Intent)
    ZS->>ZS: 2. 职责：语义建模与契约定义<br/>任务：将意图物化为 README.md
    ZS->>MX: 3. 交付：逻辑蓝图 (README)

    Note over MX: 【Phase 1: 质量立法化】
    rect rgb(240, 248, 255)
        MX->>MX: 4. 职责：逻辑审计与自动化立法<br/>任务：编写 TestShell 断言脚本
        MX->>MX: 5. 任务：静态检察 README 逻辑闭环
    end
    
    alt 逻辑封驳
        MX-->>ZS: 6a. 结果：打回重修 (逻辑悖论/缺失)
    else 检察通过
        MX->>SS: 6b. 交付：正式诏令 (认证蓝图 + 考卷标准)
    end

    Note over SS: 【Phase 2: 工程实现化】
    rect rgb(245, 255, 250)
        SS->>SS: 7. 职责：环境容器化与零件集成<br/>任务：初始化 MainShell 环境
        SS->>SS: 8. 任务：填充各部逻辑 (吏/户/礼/工/刑/兵)
        SS->>SS: 9. 任务：集成自测与契约对齐
    end
    SS->>MX: 10. 交付：集成准成品 (MainShell 产物)

    Note over MX: 【Phase 3: 审计终审化】
    rect rgb(255, 240, 245)
        MX->>MX: 11. 职责：独立黑盒审计与压力测试<br/>任务：运行 TestShell 执行终极断言
    end
    
    alt 断言失败
        MX-->>SS: 12a. 结果：封驳 (通报 Bug 罪证，指定部委重修)
    else 全部通过
        MX->>E: 12b. 颁行天下 (交付最终系统)
    end

```
```mermaid
sequenceDiagram
    autonumber
    participant E as 皇帝 (User)
    participant ZSL as 中书令 (主管)
    participant SZ as 侍中 (主管)

    Note over E, ZSL: 【Phase 0: 意图初步物化】
    E->>ZSL: 1. 传达原始意图 (Intent)
    
    loop 意图对齐
        rect rgb(240, 240, 240)
            ZSL->>ZSL: 2. 功能执行：【用例发现】
            ZSL->>ZSL: 3. 功能执行：【契约定义】
        end
        ZSL-->>E: 4. 展示 Use Case 与 Contract 框架
        E-->>ZSL: 5. 反馈修正意见
    end
    
    ZSL->>ZSL: 6. 固化为正式 README.md
    ZSL->>SZ: 7. 移交蓝图草案 (发起审计)

    Note over SZ: 【Phase 1: 独立逻辑审计】
    rect rgb(230, 240, 255)
        SZ->>SZ: 8. 功能执行：【逻辑检察】(推演业务闭环与风险)
    end
    
    Note over SZ, ZSL: 【Phase 2: 审计反馈与三方再议】
    SZ-->>ZSL: 9. 反馈检查结果 (指出逻辑冲突或边界缺失)
    
    alt 审计发现缺陷
        ZSL->>E: 10. 启动【再议】：解释审计风险
        E-->>ZSL: 11. 最终裁决或修正意图
        ZSL->>ZSL: 12. 修正用例与契约
        ZSL->>SZ: 13. 重新提交审计
    else 审计通过
        ZSL-->>E: 14. 最终蓝图成果展示
        E->>ZSL: 15. 签字授权 (Final Approval)
        ZSL->>SZ: 16. 正式移交蓝图 (发布终版 README)
    end
```
```mermaid
sequenceDiagram
    autonumber
    participant ZSL as 中书令 (主管)
    participant SZ as 侍中 (主管)
    participant LKGSZ as 吏科给事中 (负责人)
    participant SSL as 尚书令 (主管)

    Note over ZSL, SZ: 【Phase 3 起点：蓝图入省】
    ZSL->>SZ: 16. 移交已确认的终版 README

    Note over SZ: 【环节一：行政确权】
    SZ->>SSL: 17. 【盖章签发】正式下达实施诏令 (授权尚书省动工)
    
    Note over SZ, LKGSZ: 【环节二：监察分发】
    SZ->>LKGSZ: 18. 【指令】启动断言立法：根据命名规则测试 Use Case 实现

    Note over LKGSZ: 【环节三：负责人执行 - 刑名对账】
    rect rgb(255, 245, 230)
        LKGSZ->>LKGSZ: 19. 任务：解析 README 契约中的 Input/Output 规范
        LKGSZ->>LKGSZ: 20. 任务：映射 Use Case 命名规则 (确定测试函数名)
        LKGSZ->>LKGSZ: 21. 任务：编写 TestShell (构建针对性的黑盒断言)
    end
    
    LKGSZ-->>SZ: 22. 【汇报】TestShell 立法完成，监察准绳已就绪
```

```mermaid
sequenceDiagram
    autonumber
    participant SZ as 侍中 (主管)
    participant SSL as 尚书令 (主管)
    participant LBS as 吏部尚书 (负责人)
    participant QTZB as 其他部(户/兵/礼等)负责人

    Note over SZ, SSL: 【Phase 4 起点：蓝图入省】
    SZ->>SSL: 17. 【盖章签发】移交正式 README 与 实施诏令

    Note over SSL: 【环节一：环境初始化与骨架生成】
    SSL->>SSL: 18. 功能执行：【构建跨平台 MainShell】<br/>(不依赖于任何具体平台/框架的纯逻辑底座)
    SSL->>SSL: 19. 功能执行：【生成 Use Case 骨架文件】<br/>(基于命名规则映射 README)

    Note over SSL, LBS: 【环节二：业务逻辑驱动的依赖发现】
    SSL->>LBS: 20. 【指令】启动 Use Case 逻辑实现
    rect rgb(240, 248, 255)
        LBS->>LBS: 21. 任务：填充逻辑，识别外部依赖 (Client/Repo/Msg)
        LBS->>LBS: 22. 任务：【生成 Contract】(定义依赖的接口协议)
        LBS-->>SSL: 23. 【上报】提交依赖契约，申请外部支援
    end

    Note over SSL, QTZB: 【环节三：主管二次调度 - 依赖下发】
    SSL->>SSL: 24. 功能执行：【生成具体实现文件】(基于 LBS 提交的 Contract)
    SSL->>QTZB: 25. 【下发指令】指派其他负责人实现这些 Contract<br/>(如：户部实现 Repository，兵部实现 Messenger)

    Note over QTZB, SSL: 【环节四：零件汇聚与集成】
    QTZB-->>SSL: 26. 提交依赖零件实现产物
    LBS-->>SSL: 27. 提交完整 Use Case 实现

    Note over SSL, SZ: 【Phase 5: 刑名对账与自动化审判】
    
    loop 审计修正环 (Audit-Fix Loop)
        Note over SSL: 【内部集成阶段】
        SSL->>SSL: 28. 功能执行：【MainShell 注入与自测】
        SSL->>SZ: 29. 【呈报】提交集成产物 (README 实现版)
        
        Note over SZ, LKGSZ: 【外部监察阶段】
        SZ->>LKGSZ: 30. 【指令】运行 TestShell 考卷
        LKGSZ->>LKGSZ: 31. 自动化断言：比对实现与契约
        LKGSZ-->>SZ: 32. 提交【刑名报告】
        
        SZ-->>SSL: 33. 【反馈】返回测试结果 (通过/失败)
        
        alt 测试不通过 (Audit Failed)
            Note over SSL, LBS: 【精准整改阶段】
            SSL->>LBS: 34. 【指令】根据违约清单整改实现
            LBS->>LBS: 35. 修正 Use Case 逻辑或调整依赖
            LBS-->>SSL: 36. 提交修正零件 (回到步骤 28 重新集成)
        else 测试通过 (Audit Passed)
            Note over SZ: 【终局判定】
            SZ->>SZ: 37. 判定：刑名相符，逻辑闭环
        end
    end

    SZ->>SSL: 38. 【下达终审令】准予上线
```
```mermaid
sequenceDiagram
    autonumber
    participant E as 皇帝 (User)
    participant ZSL as 中书令 (中书省主管)
    participant SZ as 侍中 (门下省主管)
    participant LKGSZ as 吏科给事中 (门下省负责人)
    participant SSL as 尚书令 (尚书省主管)
    participant LBS as 吏部尚书 (尚书省负责人)
    participant QTZB as 其他部负责人 (如户/兵部)

    Note over E, ZSL: 【第一阶段：意图物化与对齐】
    E->>ZSL: 1. 传达原始意图 (Intent)
    loop 意图迭代
        ZSL->>ZSL: 2. 功能执行：【用例发现】与【契约定义】
        ZSL-->>E: 3. 展示 Use Case 与 Contract 框架
        E-->>ZSL: 4. 提供反馈/修正
    end
    ZSL->>ZSL: 5. 固化为正式 README.md，生成相应UseCase文件
    ZSL->>SZ: 6. 移交蓝图草案 (发起逻辑审计)

    Note over SZ, ZSL: 【第二阶段：逻辑审计与再议】
    SZ->>SZ: 7. 功能执行：【逻辑检察】(推演闭环风险)
    SZ-->>ZSL: 8. 反馈检查结果 (指出逻辑坏点)
    alt 存在逻辑风险
        ZSL->>E: 9. 启动【再议】：商讨修正方案
        E-->>ZSL: 10. 最终决策
        ZSL->>ZSL: 11. 修正 README/UseCas(重回步骤 5)
    else 审计通过
        ZSL-->>E: 12. 最终蓝图展示
        E-->>ZSL: 13. 签字授权 (Final Approval)
    end

    Note over SZ, SSL: 【第三阶段：行政签发与立法实施】
    ZSL->>SZ: 14. 移交已确认的终版 README和UseCase文件

    par 监察立法 (并行)
        SZ->>SSL: 15. 【盖章签发】下达实施诏令 (授权动工)
    and 环境初始化 (并行)
        SZ->>LKGSZ: 16. 指令：根据命名规则启动【断言立法】
        LKGSZ->>LKGSZ: 17. 编写 TestShell (基于README契约和UseCase文件)
    end

    Note over SSL, LBS: 【第四阶段：依赖发现与分布式生产】
    SSL->>SSL: 18. 构建 MainShell (纯逻辑底座)
    SSL->>SSL: 19. 生成 Default Use Case 骨架文件
    SSL->>LBS: 20. 指令：开始填充业务逻辑
    LBS->>LBS: 21. 任务：识别外部依赖 (Client/Repo/Msg) 并生成 Contract
    LBS-->>SSL: 22. 【上报】提交依赖契约，申请资源协助
    
    rect rgb(240, 248, 255)
        Note over SSL, QTZB: 跨部门调度
        SSL->>SSL: 23. 功能执行：生成具体实现文件
        SSL->>QTZB: 24. 【指令】指派各部负责人实现具体 Contract (如户部实现 Repo)
        QTZB-->>SSL: 25. 提交专业实现零件
    end

    Note over SSL, SZ: 【第五阶段：刑名对账循环】
    loop 自动化审判循环
        SSL->>SSL: 26. 功能执行：【MainShell 注入与自测】
        SSL->>SZ: 27. 【呈报】提交集成产物
        SZ->>LKGSZ: 28. 执行 TestShell 考卷
        LKGSZ-->>SZ: 29. 提交刑名报告 (违约项清单)
        SZ-->>SSL: 30. 反馈测试结果
        alt 测试 NG
            SSL->>LBS: 31. 【打回】限期整改
            LBS->>LBS: 32. 修正实现 (回到步骤 26 重新集成)
        else 测试 OK
            SZ->>SZ: 33. 判定：刑名相符，逻辑闭环
        end
    end

    Note over SZ, E: 【终阶段：完工复命】
    SZ->>SSL: 34. 【下达终审令】验收合格
    SSL-->>E: 35. 【正式复命】商业逻辑已验证，可以进入Hermi第二阶段了。
```

```java
package com.empire.logic.user;

import com.empire.framework.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Hidden in javadoc, new features need to be implemented by Code Factory
 * <h1>【工单·中书令】用户身份溯源与大仓同步</h1>
 *
 * <p>## 1. 皇帝意图 (Directives)
 * 陛下旨意：通过社会安全号码 (SSN) 调取外部身份数据，完成本地存仓，并确保同步结果已正式宣告。</p>
 *
 * <p>## 2. 协作协议 (Collaboration Protocol)
 * <ul>
 * <li><b>中书省 (Architect Agent):</b> 定义本契约结构、入参出参及核心业务边界。</li>
 * <li><b>门下省 (Auditor Agent):</b> 依据 {@code Logic Assertions} 编写 TestShell。必须验证调用链路：Client -> Repo -> Messenger。</li>
 * <li><b>尚书省 (Executor Agent):</b> 负责逻辑编排。严禁直接引入 Spring/JPA 等外部 Shell 层依赖，保持逻辑纯净。</li>
 * </ul></p>
 *
 * <p>## 3. 逻辑断言 (Logic Assertions)
 * <ul>
 * <li><b>[A1] 格式完整性：</b> 输入 {@code Context} 必须通过验证，{@code ssn} 严禁为空或空白字符。</li>
 * <li><b>[A2] 刑名相符：</b> {@code Result} 中的数据必须来源于 {@code FindUserClient} 的实时响应。</li>
 * <li><b>[A3] 宣告必达：</b> 无论存仓结果如何，必须触发 {@code NotifyUserFoundMessenger} 进行复命。</li>
 * </ul></p>
 *
 * @WorkOrderID WO-2026-FIND-USER
 * @Tag #IdentitySync #DiscoveryProcess
 */

 /**
  * Show on javadoc
  * Existing Features
  */
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Context, FindUserUseCase.Result> {

    /**
     * @param ssn 社会安全号码 (SSN)
     */
    public static record Context(@NotNull @NotBlank String ssn) implements Validatable {}

    /**
     * @param name 用户姓名 (从 API 获取)
     * @param email 用户邮箱 (从 API 获取)
     */
    public static record Result(String name, String email) {}
}

```