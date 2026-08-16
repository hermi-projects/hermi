# Shell Client

在 **Hermi 架构框架**中，**Shell Client**（也被称为 **Vendor Client**）是位于 **Phase 2（Shell 层 / 基础设施层）** 中负责处理底层网络通信与外部第三方服务交互的技术组件。

它是 Hermi 实现“基础设施细节与业务核心解耦”的重要落地支撑。以下是对 Shell Client 的全面介绍：

---

### 1. 核心职责：定义“技术实现”而非“业务意图”

在执行真实的外部系统调用时，Shell 必须处理具体的网络协议和第三方契约（例如调用 LexisNexis、Stripe 支付网关或远程 API）。

* **技术导向**：`Shell Client` 专注于“使用什么协议、什么库（如 `RestTemplate`、`WebClient`、`Feign`）发送请求”**以及**“处理供应商特定的底层载荷（Payload）与响应（Response）”。
* **隔离底层异构性**：它将所有的网络异常、HTTP 状态码、JSON 序列化细节以及第三方 SDK 的复杂性全部封装在 Shell 层内部。

---

### 2. 生命周期与触发时机

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 此阶段**完全不需要**编写 Shell Client。业务核心只关心抽象的 `Client` 契约，并通过内存中的模拟实现（Local Client）完成全链路验证。


* **Phase 2（基础设施实现阶段）**：
* 当业务逻辑成熟并需要接入真实外部环境时，才开始编写 Shell Client。
* 它通过生产实现类（Production Implementation）与 Mapper 配合，作为适配器去实现 Use Case 定义的 Client 契约。



---

### 3. 典型代码形态

在基础设施层中，它表现为一个特定于技术或供应商的客户端组件：

```java
// 基础设施层（Shell）中封装底层网络通信的 Vendor Client
@Component
public class LexisNexisUserClient {
    private final RestTemplate restTemplate;

    @Autowired
    public LexisNexisUserClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LexisNexisResponse exchange(LexisNexisPayload payload) {
        // 使用底层 HTTP 库发起真实的远程网络请求
        return restTemplate.postForObject("/api/users", payload, LexisNexisResponse.class);
    }
}

```

---

### 4. 协同工作机制（桥接 Use Case 与 Shell）

在 Shell 层中，Shell Client 通常不直接暴露给业务核心，而是与 **Mapper** 协同工作，共同落地 Use Case Client 契约：

```java
@Component
public class LexisNexisFindUserClient extends FindUserClient {
    private final LexisNexisUserClient vendorClient;
    private final LexisNexisUserMapper mapper;

    @Autowired
    public LexisNexisFindUserClient(LexisNexisUserClient vendorClient, LexisNexisUserMapper mapper) {
        this.vendorClient = vendorClient;
        this.mapper = mapper;
    }

    @Override
    protected Result doFulfill(Context context) {
        // 1. 通过 Mapper 将业务上下文转换为供应商特定的请求负载
        LexisNexisPayload apiRequest = mapper.toPayload(context);
        
        // 2. 调用 Shell Client 执行底层网络通信
        LexisNexisResponse apiResponse = vendorClient.exchange(apiRequest);
        
        // 3. 通过 Mapper 将供应商响应转换回业务核心结果
        return mapper.toResult(apiResponse);
    }
}

```

---

### 5. 核心优势

* **供应商无感切换**：如果将来需要将第三方服务从 LexisNexis 替换为 Experian，**业务核心层的 Use Case 和 Client 契约完全不用修改**，只需在 Shell 层编写一个新的 `ExperianUserClient` 及其对应的适配器和 Mapper 即可。
* **技术栈灵活演进**：如果底层网络库需要从 `RestTemplate` 升级为 `WebClient` 或 gRPC，只需修改 Shell Client 内部的实现，外层的业务逻辑和契约不会受到任何波及。