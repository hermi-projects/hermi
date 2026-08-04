# Use Case Messenger

在 **Hermi 架构框架**中，**Use Case Messenger**（通常简称为 **Messenger**）是核心业务层（Use Case）用于向外部消息中间件、事件总线或通知系统（如 Kafka、RabbitMQ、SNS 等）发布事件或发送通知的抽象契约。

它是 Hermi 确保业务核心能够解耦“异步事件通知与通信基础设施”的关键组件。以下是对 Use Case Messenger 的全面介绍：

---

### 1. 核心职责：定义“通知与事件意图”而非“技术实现”

在执行纯业务逻辑的过程中，当某个业务动作达成时（例如用户创建成功、密码重置、状态变更等），往往需要向外界广播事实（Fact）。

* **语义导向**：`Messenger` 契约严格遵循语义命名（如 `Notify{Fact}Messenger`），它只关心“业务上需要传递什么通知上下文（Context）”**以及**“消息发送成功后返回什么确认结果（Result）”（例如返回消息 ID 或元数据）。
* **隔离底层技术**：它完全不知道底层使用的是 Kafka、RabbitMQ、AWS SQS 还是简单的邮件发送服务，也不关心序列化方式是 JSON 还是 Protobuf。

---

### 2. 双阶段演变生命周期

遵循 Hermi 的“Discovery Lifecycle”：

* **Phase 1（纯 Java 核心发现阶段）**：
* 在 Use Case 模块中定义抽象类（例如 `NotifyUserFoundMessenger`）。
* 在编写业务编排逻辑时直接注入并调用它。
* 本地测试（Main Shell）时，编写一个简单的控制台追踪实现（例如 `ConsoleNotifyUserFoundMessenger`，内部直接打印 `System.out`），**无需启动任何消息队列服务器或容器**即可完成全链路验证。


* **Phase 2（基础设施实现阶段）**：
* 在外层 Shell（如 Spring Boot）中编写真正的技术实现。
* 通过 **Mapper** 和 **Vendor Messenger**（如 Spring Kafka 的 `KafkaTemplate`）将业务契约落地到真实的消息分发管道中。



---

### 3. 典型代码形态

在业务核心层中，它表现为一个继承自框架基类的抽象类：

```java
// 纯业务核心层定义的 Messenger 契约
public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
    public record Context(String email, String message) {}
    public record Result(String messageId) implements Validatable {}
}

```

---

### 4. Phase 2 的具体实现：协同工作机制

在基础设施层（Shell），为了适配具体的消息中间件（如 Kafka），`Messenger` 的落地依赖于**生产实现类**、**Vendor Messenger** 和 **Mapper** 的协同：

* **`KafkaUserMessenger`（Vendor Messenger）**：特定于框架的底层消息发送器（例如封装了 `KafkaTemplate<String, String>`），直接负责与消息中间件建立连接并发送原始的生产者记录（`ProducerRecord`）。
* **`KafkaUserMapper`（Mapper）**：负责数据转换：
* 将 Use Case 的 `Messenger.Context` 映射为底层消息载荷（如 Kafka 的 `ProducerRecord`）。
* 将消息发送后的元数据（如 `RecordMetadata`）映射回 Use Case 所需的 `Messenger.Result`。


* **`KafkaNotifyUserFoundMessenger`（生产实现类）**：继承自 `NotifyUserFoundMessenger`，作为适配器将上述两部分串联起来：
```java
@Component
public class KafkaNotifyUserFoundMessenger extends NotifyUserFoundMessenger {
    private final Messenger<ProducerRecord<String, String>, RecordMetadata> messenger;
    private final Mapper<Context, Result, ProducerRecord<String, String>, RecordMetadata> mapper;

    // 构造函数注入...

    @Override
    protected Result doExecute(Context context) {
        ProducerRecord<String, String> record = mapper.toPayload(context);
        RecordMetadata metadata = messenger.publish(record);
        return mapper.toResult(metadata);
    }
}

```



---

### 5. 核心优势

* **零外部依赖的事件验证**：在 Phase 1 中，通过控制台或内存 Shell，你可以轻松验证业务逻辑是否在正确的时机触发了正确的通知内容，**不需要部署 Kafka/RabbitMQ 等中间件**。
* **消息中间件无感替换**：如果未来业务决定将消息队列从 Kafka 迁移到 Pulsar 或 AWS SNS，**业务核心层的 Use Case 和 `NotifyUserFoundMessenger` 契约完全不需要改动**，只需在 Shell 层实现一套新的 Pulsar/SNS 适配器即可。