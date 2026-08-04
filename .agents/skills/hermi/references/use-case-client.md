# Use Case Client

在 Hermi 架构框架中，**Use Case Client**（通常简称为 **Client**）是核心业务层（Use Case）用于向**外部系统、第三方服务或远程 API** 发起数据搜寻和交互的抽象契约。

它是 Hermi 实现“业务核心与基础设施解耦”的核心组件。以下是对 Use Case Client 的全面整合介绍：

---

### 1. 核心职责：定义“意图”而非“技术”

在执行纯业务逻辑（Use Case）时，经常需要从外部获取数据（例如调用实名认证 API、查询远程用户服务等）。

* **业务导向**：`Client` 只关心“业务上我需要向外部索取什么数据（Context）”**以及**“外部会返回给我什么格式的数据（Result）”。
* **隔离复杂性**：它完全不知道底层是由什么技术实现的（如 `RestTemplate`、`WebClient` 或 `Feign`），也不知道通信协议是 HTTP 还是 gRPC。

---

### 2. 双阶段演变生命周期

Hermi 的“Discovery Lifecycle”赋予了 Client 极高的灵活性：

* **Phase 1（纯 Java 核心发现阶段）**：
* 在 Use Case 模块中定义抽象类 `FindUserClient`。
* 编写业务逻辑时直接调用它。
* 本地测试（Main Shell）时，编写一个简单的 `LocalFindUserClient`（例如用 `HashMap` 模拟返回），**无需任何网络或外部依赖**即可验证核心业务。


* **Phase 2（基础设施实现阶段）**：
* 在外层 Shell（如 Spring Boot）中编写真正的技术实现。
* 通过 **Mapper** 和 **Vendor Client** 完成与特定第三方供应商的对接。



---

### 3. 典型代码形态

在业务核心层中，它表现为一个继承自框架基类的抽象类：

```java
// 纯业务核心层定义的 Client 契约
public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
    public record Context(String ssn) {}
    public record Result(String name, String email) implements Validatable {}
}

```

---

### 4. Phase 2 的具体实现：协同工作机制

在基础设施层（Shell），为了适配具体的第三方供应商（如 LexisNexis），`Client` 的落地需要通过**生产实现类**、**Vendor Client** 和 **Mapper** 三者的协同来完成：

* **`LexisNexisUserClient`（Vendor Client）**：负责纯粹的底层网络通信（如封装 `RestTemplate` 发送 HTTP 请求），处理供应商特定的协议与传输。
* **`LexisNexisUserMapper`（Mapper）**：负责数据转换：
* 将 Use Case 的 `Client.Context` 映射为供应商特定的 `LexisNexisPayload`。
* 将供应商返回的 `LexisNexisResponse` 映射回 Use Case 所需的 `Client.Result`。


* **`LexisNexisFindUserClient`（生产实现类）**：继承自 `FindUserClient`，作为适配器将上述两者串联起来：
```java
@Component
public class LexisNexisFindUserClient extends FindUserClient {
    private final Client<LexisNexisPayload, LexisNexisResponse> client;
    private final Mapper<Context, Result, LexisNexisPayload, LexisNexisResponse> mapper;

    // 构造函数注入...

    @Override
    protected Result doExecute(Context context) {
        LexisNexisPayload apiRequest = mapper.toPayload(context);
        LexisNexisResponse apiResponse = client.exchange(apiRequest);
        return mapper.toResult(apiResponse);
    }
}

```



---

### 5. 核心优势

* **极佳的可测试性**：在没有真实第三方 API 的情况下，Phase 1 即可通过内存模拟完成全链路验证。
* **供应商无感替换**：如果将来业务需要将供应商从 LexisNexis 替换为 Experian，**业务核心层的 Use Case 和 `FindUserClient` 契约代码一行都不用改**，只需在 Shell 层新增一套对应的实现和 Mapper 即可。