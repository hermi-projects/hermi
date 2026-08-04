# Shell Consumer

# Shell Consumer (Inbound Message Adapter)

在 **Hermi 架构框架**中，**Shell Consumer**（也被称为 **Vendor Consumer**）是位于 **Phase 2（Shell 层 / 基础设施层）** 中负责处理底层消息队列消费、事件监听与异步消息接入的技术组件。

它是 Hermi 实现“消息中间件细节与业务核心解耦”的重要落地支撑。以下是对 Shell Consumer 的全面介绍：

---

### 1. 核心职责：定义“技术实现”而非“业务意图”

在接收真实的消息中间件事件时，Shell 必须处理具体的消息协议和框架注解（例如使用 Spring Kafka 的 `@KafkaListener`、RabbitMQ 的 `@RabbitListener` 或 AWS SQS 监听器）。

* **技术导向**：`Shell Consumer` 专注于“使用什么框架、连接什么主题/队列（如 Kafka Topics、RabbitMQ Queues、消费组 Group ID）去拉取或接收流量”**以及**“处理消息中间件特定的原始载荷（Payload）与元数据”。
* **隔离底层异构性**：它将所有的连接重试策略、反序列化异常、消息确认（Ack/Nack）机制以及框架特定的监听器逻辑全部封装在 Shell 层内部。

---

### 2. 生命周期与触发时机

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 此阶段**完全不需要**编写 Shell Consumer。业务核心只关心抽象的 Use Case 及其 `Context`，并通过本地测试（Shell Test / Main Shell）或内存事件模拟完成全链路验证。


* **Phase 2（基础设施实现阶段）**：
* 当业务逻辑成熟并需要接入真实的消息中间件（如 Kafka、RabbitMQ）时，才开始编写 Shell Consumer。
* 它作为事件驱动的驱动适配器，负责接收外部异步消息并将其转化为对 Use Case 或中间服务（Service）的调用。



---

### 3. 典型代码形态

在基础设施层中，它表现为一个特定于消息中间件的监听器组件：

```java
// 基础设施层（Shell）中封装底层消息队列消费与事件监听的 Shell Consumer
@Component
public class FindUserConsumerShell {
    private final FindUserService findUserService;

    @Autowired
    public FindUserConsumerShell(FindUserService findUserService) {
        this.findUserService = findUserService;

    @KafkaListener(topics = "user.find.requests", groupId = "user-service-group")
    public void consumeUserFindEvent(String rawPayload) {
        // 1. 将底层的原始消息载荷解析并组装为业务 Use Case 的 Context
        FindUserUseCase.Context context = new FindUserUseCase.Context(rawPayload);
        
        // 2. 委托给服务层执行
        findUserService.findUser(context);
    }
}

```

---

### 4. 协同工作机制（桥接外部异步事件与 Use Case）

在 Shell 层中，Shell Consumer 通常不直接包含核心业务编排，而是与 **Service 层**（负责处理事务等框架横切关注点）协同工作，最终触发 Use Case 的执行：

```java
@Service
@Transactional
public class FindUserService {
    private final FindUserUseCase findUserUseCase;

    @Autowired
    public FindUserService(LexisNexisFindUserClient client, 
                           JpaSaveUserRepository repo, 
                           KafkaNotifyUserFoundMessenger messenger) {
        // 将 Use Case 与生产环境的基础设施实现进行装配
        this.findUserUseCase = new DefaultFindUserUseCase(client, repo, messenger);
    }

    public FindUserUseCase.Result findUser(FindUserUseCase.Context context) {
        // 执行纯业务核心逻辑
        return findUserUseCase.execute(context);
    }
}

```

---

### 5. 核心优势

* **异步通道交付与无感扩展**：如果将来需要将相同的核心业务通过不同的通道暴露（例如同时支持 Shell API 和 Shell Consumer），**业务核心层的 Use Case 完全不用修改**，只需在 Shell 层编写一个新的消息消费适配器即可。
* **技术栈灵活演进**：如果底层消息队列需要从 Kafka 迁移到 RabbitMQ 或 AWS SQS，只需修改 Shell Consumer 内部的实现，外层的业务逻辑和契约不会受到任何波及。