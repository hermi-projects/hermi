# Use Case

在 **Hermi 架构框架**中，**Use Case**（用例）是核心业务层的基石，代表了系统所能执行的一个个**具体的业务动作（Action）**。

它是 Hermi “Intent-Driven Architecture”（意图驱动架构）的核心载体，定义了系统 ***What*（做什么）**，而将所有技术细节推迟到外层的 Shell 去实现。

以下是对 Use Case 的全面介绍：

---

### 1. 核心职责：业务的“主权者”

在 Hermi 中，Use Case 拥有绝对的统治地位（The Use Case is the Sovereign of the domain）。它的职责包括：

* **边界定义**：明确规定动作的**输入上下文（Context）**与**执行结果（Result）**。
* **业务编排**：以纯 Java 编写核心的业务逻辑和处理流程，协调各种 I/O 契约（Client、Repository、Messenger）。
* **协议与边界警察**：强制对所有进入 Use Case 边界的数据进行契约校验（通过 `Validatable` 接口），确保业务核心永远处理安全、合法的数据。

---

### 2. 双阶段演变生命周期

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 编写纯 Java 的抽象类继承自 `UseCase`。
* 采用 **Blueprint-First**（蓝图优先）方式，先通过 Narrative-First 叙事式开发把整个业务流程和所需的依赖（Client/Repository/Messenger）串联起来。
* 配合 **Main Shell**（纯 Java 的 `main` 方法），在没有任何框架、没有数据库、没有 Mock 工具的情况下，直接在内存中运行并验证业务场景。


* **Phase 2（基础设施实现阶段）**：
* 将写好的 Use Case 核心接入外层 Shell（如 Spring Boot、Quarkus 等）。
* 通过服务层（Service）或直接在 API 入口（Entry Point）中进行装配，绑定真实的数据库、第三方 API 和消息队列。



---

### 3. 典型代码形态

在 Hermi 中，一个标准的 Use Case 通常由一个抽象类（定义边界）和一个具体的实现类组成：

```java
// 1. 定义 Use Case 边界（Context 和 Result）
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Context, FindUserUseCase.Result> {
    public static record Context(@NotNull @NotBlank String ssn) implements Validatable {}
    public static record Result(String name, String email) {}
}

// 2. 实现核心业务逻辑
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    private final SaveUserRepository saveUserRepository;

    public DefaultFindUserUseCase(FindUserClient findUserClient, SaveUserRepository saveUserRepository) {
        this.findUserClient = findUserClient;
        this.saveUserRepository = saveUserRepository;
    }

    @Override
    protected Result doExecute(Context context) {
        // 核心业务编排
        var apiResult = findUserClient.execute(new FindUserClient.Context(context.ssn()));
        saveUserRepository.execute(new SaveUserRepository.Context(apiResult.name(), apiResult.email()));
        
        return new Result(apiResult.name(), apiResult.email());
    }
}

```

---

### 4. 命名与结构规范

* **命名模式**：`{Action}{Resource}UseCase`（例如 `FindUserUseCase`），实现类为 `Default{Action}{Resource}UseCase`。
* **独立模块**：在多模块项目中，它通常位于独立的纯 Java 模块中（如 `cdn-find-user-use-case`），其 `pom.xml` 中**绝对不能引入任何 Spring、Hibernate、Kafka 等基础设施或框架的依赖**，从而确保核心业务的纯粹性与长久的可维护性。