# Hermi 架构框架：核心组件全景总结

在 **Hermi 架构框架**中，整个系统由一组高度解耦、职责分明的组件构成。它们通过严格的单向依赖与契约隔离，实现了“业务核心与技术基础设施的完美解耦”。

Hermi 的主要核心组件可划分为以下几大板块：

---

### 一、 核心业务层（Core & Use Case Layer）

* **Domain（领域模型）**
* **职责**：表达纯粹的业务规则、不变量和状态计算。
* **特点**：**基于 Use Case 构建，Use Case 之间不共享 Domain**，彻底告别传统全局“胖实体（God Entity）”的污染。


* **Use Case（用例 / 应用服务）**
* **职责**：定义系统的 ***What*（做什么）**，编排具体的业务流程与执行路径。
* **特点**：代表一个独立的业务切片，是整个核心业务逻辑的入口与编排者。


* **Context / Result（用例数据契约）**
* **职责**：封装用例专有的输入上下文（`Context`）与输出结果（`Result`）。
* **特点**：作为用例与外界沟通的纯净数据传输结构，绝对不包含任何框架属性或技术注解。



---

### 二、 基础设施层：入站驱动适配器（Inbound Shell Layer）

* **Shell API**
* **职责**：驱动适配器。负责处理外部符合 RESTful 标准的 HTTP 请求、路由映射与协议转换，将其转化为对 Use Case 的调用。


* **Shell Consumer**
* **职责**：事件驱动适配器。负责监听外部消息队列（如 Kafka、RabbitMQ），在接收到异步事件时触发对应的 Use Case。



---

### 三、 基础设施层：出站被动适配器（Outbound Shell Layer）

* **Shell Client**
* **职责**：外部系统客户端。封装对远程第三方 REST/gRPC API 的调用细节。


* **Shell Repository**
* **职责**：持久化仓库。封装对底层数据库（如 MySQL、JPA）的数据读写操作。


* **Shell Messenger**
* **职责**：消息发布器。封装底层消息中间件的投递逻辑，实现异步事件的发送。



---

### 四、 本地验证与探索组件（Local & Verification Layer）

* **Shell Test**
* **职责**：自动化测试套件。配合内存模拟适配器，在完全脱离容器和框架的环境下对 Use Case 进行极速的单元与集成验证。


* **Shell Main（`public static void main`）**
* **职责**：本地引导与运行沙箱。用于 Phase 1 阶段的纯 Java 手动探索与冒烟测试。


* **Local Stubs（`LocalClient`, `InMemoryRepository`, `ConsoleMessenger`）**
* **职责**：内存替身。在 Phase 1 阶段提供零依赖的轻量级运行时实现，支撑核心业务的独立闭环。