# Shell Messenger

# Shell Messenger (Vendor Messenger)

在 **Hermi 架构框架**中，**Shell Messenger**（也被称为 **Vendor Messenger** ）是位于 **Phase 2（Shell 层 / 基础设施层）** 中负责处理底层消息发布、事件广播与通知发送的技术组件。

它是 Hermi 实现“异步通信细节与业务核心解耦”的重要落地支撑。以下是对 Shell Messenger 的全面介绍：

---

### 1. 核心职责：定义“技术实现”而非“业务意图”

在执行真实的消息中间件或通知分发操作时，Shell 必须处理具体的消息协议和底层载荷结构（例如使用 Spring Kafka 的 `KafkaTemplate`、RabbitMQ 的 `AmqpTemplate`、或第三方邮件/短信 SDK）。

* **技术导向**：`Shell Messenger` 专注于“使用什么框架、什么中间件（如 Kafka 的 `ProducerRecord`、RabbitMQ、SNS）去投递消息”**以及**“处理消息中间件特定的底层载荷与发送元数据（Metadata）”。
* **隔离底层异构性**：它将所有的连接配置、序列化策略、主题/队列命名、以及消息中间件的异常处理细节全部封装在 Shell 层内部。

---

### 2. 生命周期与触发时机

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 此阶段**完全不需要**编写 Shell Messenger。业务核心只关心抽象的 `Messenger` 契约，并通过内存或控制台的模拟实现（如 `ConsoleNotifyUserFoundMessenger`）完成全链路验证。


* **Phase 2（基础设施实现阶段）**：
* 当业务逻辑成熟并需要接入真实消息中间件或通知服务时，才开始编写 Shell Messenger。
* 它通过生产实现类（Production Implementation）与 Mapper 配合，作为适配器去实现 Use Case 定义的 Messenger 契约。



---

### 3. 典型代码形态

在基础设施层中，它表现为一个特定于技术或中间件的发送组件：

```java
// 基础设施层（Shell）中封装底层消息发送操作的 Vendor Messenger
@Component
public class KafkaUserMessenger {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaUserMessenger(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public RecordMetadata publish(ProducerRecord<String, String> payload) {
        // 使用底层消息模板发起真实的异步消息投递
        try {
            return kafkaTemplate.send(payload).get().getRecordMetadata();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish message", e);
        }
    }
}

```

---

### 4. 协同工作机制（桥接 Use Case 与 Shell）

在 Shell 层中，Shell Messenger 通常不直接暴露给业务核心，而是与 **Mapper** 和生产实现类（Production Implementation）协同工作，共同落地 Use Case Messenger 契约：

```java
@Component
public class KafkaNotifyUserFoundMessenger extends NotifyUserFoundMessenger {
    private final KafkaUserMessenger vendorMessenger;
    private final KafkaUserMapper mapper;

    @Autowired
    public KafkaNotifyUserFoundMessenger(
            KafkaUserMessenger vendorMessenger, 
            KafkaUserMapper mapper) {
        this.vendorMessenger = vendorMessenger;
        this.mapper = mapper;
    }

    @Override
    protected Result doExecute(Context context) {
        // 1. 通过 Mapper 将业务上下文转换为底层消息记录（如 ProducerRecord）
        ProducerRecord<String, String> record = mapper.toPayload(context);
        
        // 2. 调用 Shell Messenger 执行底层消息投递
        RecordMetadata metadata = vendorMessenger.publish(record);
        
        // 3. 通过 Mapper 将发送元数据转换回业务核心结果
        return mapper.toResult(metadata);
    }
}

```

---

### 5. 核心优势

* **消息中间件无感切换**：如果将来需要将消息队列从 Kafka 迁移到 RabbitMQ 或 AWS SNS，**业务核心层的 Use Case 和 Messenger 契约完全不用修改**，只需在 Shell 层编写一个新的 RabbitMQ 消息发送器及其对应的适配器和 Mapper 即可。
* **技术栈灵活演进**：如果底层消息投递客户端需要从同步阻塞发送调整为响应式（Reactive）发送，只需修改 Shell Messenger 内部的实现，外层的业务逻辑和契约不会受到任何波及。