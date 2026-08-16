# Shell Main

# Shell Main (`public static void main`)

在 **Hermi 架构框架**中，**Shell Main**（通常直接对应 `public static void main` 入口类）是位于 **Phase 1（纯 Java 核心发现阶段）与 Phase 2 桥梁**中的本地运行与引导组件。

它是 Hermi 实现“零框架负担的纯业务探索”与“本地快速自测”的重要支撑。以下是对 Shell Main 的全面介绍：

---

### 1. 核心职责：提供纯 Java 的应用启动与运行沙箱

在传统的企业应用中，启动应用往往依赖 Spring Boot 或其他重型 IoC 容器。而 Shell Main 专注于通过标准的 Java 入口来驱动整个业务核心：

* **技术导向**：`Shell Main` 专注于“通过编写标准的 `public static void main(String[] args)` 方法，手动组装 Use Case 与内存适配器（InMemory Stubs），并在纯 Java 环境下直接运行程序”。
* **隔离底层异构性**：它在不依赖任何 Web 容器、不连接真实数据库、不启动消息队列的情况下，提供了一个最简、最纯粹的本地执行上下文。

---

### 2. 生命周期与触发时机

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 这是 Shell Main 的主战场。当开发者刚写完 Use Case 和领域逻辑时，会立即写一个 `Main` 类，用硬编码或本地模拟数据去跑通业务流程，验证业务设计的正确性。


* **Phase 2（基础设施实现阶段）**：
* 随着应用演进到生产阶段，Shell Main 可以逐步退场，或者演变为本地集成测试的引导程序；而生产环境的流量则接管给 **Shell API** 和 **Shell Consumer**。



---

### 3. 典型代码形态

在开发或验证阶段，它表现为一个标准的 Java 应用程序入口：

```java
// 纯 Java 核心探索阶段的引导程序：Shell Main
public class FindUserShellMain {
    public static void main(String[] args) {
        System.out.println("=== Starting Hermi Phase 1: Local Core Execution ===");

        // 1. 手动实例化内存中的模拟适配器 (Local Stubs)
        FindUserClientInMemory localClient = new FindUserClientInMemory();
        FindUserRepositoryInMemory localRepo = new FindUserRepositoryInMemory();
        FindUserMessengerInMemory localMessenger = new FindUserMessengerInMemory();

        // 2. 将纯业务 Use Case 与模拟适配器进行装配
        FindUserUseCase useCase = new DefaultFindUserUseCase(localClient, localRepo, localMessenger);

        // 3. 构造测试 Context 并执行
        FindUserUseCase.Context context = new FindUserUseCase.Context("123-45-6789");
        FindUserUseCase.Result result = useCase.fulfill(context);

        // 4. 输出执行结果，完成闭环验证
        System.out.println("Execution Result: " + result);
        System.out.println("=== Phase 1 Execution Completed Successfully ===");
    }
}

```

---

### 4. 协同工作机制（驱动纯 Java 核心）

在 Hermi 架构中，Shell Main 不加载任何复杂的框架配置，它直接面向纯 Java 核心（Use Case）进行依赖注入（Manual Dependency Injection）：

```java
// 手动组装纯 Java 核心与 Shell 层的本地替身
DefaultFindUserUseCase useCase = new DefaultFindUserUseCase(
    new LocalUserClient(), 
    new LocalUserRepository(), 
    new LocalUserMessenger()
);

```

---

### 5. 核心优势

* **极致的启动速度**：无需等待 Spring 容器初始化或扫描组件，`public static void main` 可以实现毫秒级启动，为开发者提供极高的反馈闭环。
* **摆脱框架束缚**：确保开发者在早期设计阶段能够全神贯注于业务逻辑（Use Case）本身，而不会陷入框架配置、注解地狱或繁琐的依赖注入 XML/Config 中。