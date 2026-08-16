# Shell Test

# Shell Test 完整描述与规范

在 **Hermi 架构框架**中，**Shell Test** 是位于 **Phase 1（纯 Java 核心发现阶段）与 Phase 2 过渡阶段**的核心自动化验证组件。

它旨在通过轻量级的本地内存适配器（Local Stubs），在完全脱离重量级框架（如 Spring 容器、真实数据库、远程网络）的环境下，对纯业务核心（Use Case）进行极速、稳定且高覆盖率的自动化验证。

---

### 一、 核心职责与设计理念

1. **验证纯业务意图**：
* Shell Test 专注于测试 Use Case 的业务逻辑、分支流转、异常处理及边界条件，而不是测试框架的配置或组件扫描是否正确。


2. **解耦外部基础设施**：
* 彻底隔离真实的网络 API、关系型数据库和消息中间件。通过内存替身（如 `LocalClient`、`InMemoryRepository`、`ConsoleMessenger`）模拟外部行为。


3. **极致的执行反馈**：
* 不加载复杂的 IoC 容器，测试启动和运行通常在毫秒级内完成，为开发者提供类似“保存即测试”的敏捷体验。



---

### 二、 在 Hermi 生命周期中的定位

* **Phase 1（纯 Java 核心发现阶段）**：
* 这是 Shell Test 的主战场。当开发者刚写完 Use Case、Domain 和抽象契约时，便编写 Shell Test 配合内存模拟适配器来推敲和验证业务逻辑。


* **Phase 2（基础设施实现阶段）**：
* 随着生产级 Shell 组件（如 `SpringJpaRepository`、`KafkaMessenger`）的引入，Shell Test 可以继续演变为集成测试或契约测试的基础，或者作为核心业务的回归安全网。



---

### 三、 完整代码示例

以下是一个标准的 Shell Test 示例，它完整展示了如何使用 JUnit 5 配合 Phase 1 的内存模拟适配器对 Use Case 进行全链路单元验证：

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

// ==========================================
// 1. Shell Test 类定义
// ==========================================
class FindUserShellTest {

    private FindUserUseCase useCase;
    private LocalFindUserClient localClient;
    private InMemoryFindUserRepository localRepo;
    private ConsoleFindUserMessenger localMessenger;

    @BeforeEach
    void setUp() {
        // 初始化 Phase 1 的轻量级内存模拟适配器 (Local Stubs)
        localClient = new LocalFindUserClient();
        localRepo = new InMemoryFindUserRepository();
        localMessenger = new ConsoleFindUserMessenger();

        // 手动将模拟适配器装配到纯业务 Use Case 中（无框架依赖）
        useCase = new DefaultFindUserUseCase(localClient, localRepo, localMessenger);
    }

    @Test
    void should_find_user_and_persist_successfully() {
        // 准备测试上下文
        FindUserUseCase.Context context = new FindUserUseCase.Context("123-45-6789");

        // 执行业务核心
        FindUserUseCase.Result result = useCase.fulfill(context);

        // 断言业务结果
        assertNotNull(result);
        assertEquals("123-45-6789", result.getSsn());
        assertEquals("Test Mock User", result.getName());

        // 验证持久化替身中是否成功保存了状态
        Optional<UserEntity> savedEntity = localRepo.findBySsn("123-45-6789");
        assertTrue(savedEntity.isPresent());
    }

    @Test
    void should_throw_exception_when_user_not_found() {
        // 模拟异常场景：指定一个会触发错误或空结果的输入
        FindUserUseCase.Context context = new FindUserUseCase.Context("invalid-ssn");

        // 验证业务边界与异常分支
        assertThrows(UserNotFoundException.class, () -> {
            useCase.fulfill(context);
        });
    }
}

// ==========================================
// 2. 配套的 Phase 1 内存模拟适配器 (Local Stubs)
// ==========================================

class LocalFindUserClient extends FindUserClient {
    @Override
    protected ExternalUserData doFulfill(ExternalUserPayload payload) {
        if ("invalid-ssn".equals(payload.getSsn())) {
            return null; // 模拟未找到
        }
        return new ExternalUserData(payload.getSsn(), "Test Mock User");
    }
}

class InMemoryFindUserRepository extends FindUserRepository {
    private final Map<String, UserEntity> database = new ConcurrentHashMap<>();

    @Override
    protected UserEntity doSave(UserEntity entity) {
        database.put(entity.getSsn(), entity);
        return entity;
    }

    @Override
    protected Optional<UserEntity> doFindBySsn(String ssn) {
        return Optional.ofNullable(database.get(ssn));
    }
}

class ConsoleFindUserMessenger extends FindUserMessenger {
    @Override
    protected void doPublish(UserFoundEvent event) {
        // 打印到控制台以供本地调试观察
        System.out.println("[SHELL TEST CONSOLE] Event Published: " + event.getSsn());
    }
}

```

---

### 四、 核心价值与优势

1. **绝对隔离**：避免了因测试数据库断开、网络延迟或第三方 API 挂掉而导致的测试不稳定性（Flaky Tests）。
2. **逼迫解耦设计**：如果业务代码强依赖了 Spring 容器或第三方 SDK，将无法轻易写出这种干净的 Shell Test。它从反向督促开发者保持 Use Case 的纯净性。
3. **零配置、高便携**：任何开发者拉取代码后，不需要配置任何环境变量、Docker 容器或外部服务，直接运行 `mvn test` 即可全绿通过。