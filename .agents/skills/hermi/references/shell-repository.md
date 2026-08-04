# Shell Repository

# Shell Repository (Vendor Repository)

在 **Hermi 架构框架**中，**Shell Repository**（也被称为 **Vendor Repository**）是位于 **Phase 2（Shell 层 / 基础设施层）** 中负责处理底层数据持久化与数据库交互的技术组件。

它是 Hermi 实现“持久化技术细节与业务核心解耦”的重要落地支撑。以下是对 Shell Repository 的全面介绍：

---

### 1. 核心职责：定义“技术实现”而非“业务意图”

在执行真实的数据库操作时，Shell 必须处理具体的持久化框架和底层数据表结构（例如使用 Spring Data JPA、JDBC、MongoDB 驱动等）。

* **技术导向**：`Shell Repository` 专注于“使用什么框架、什么数据源（如 Spring Data JPA 的 `JpaRepository`、Hibernate、MongoDB Template）去读写数据”**以及**“处理数据库特定的实体（Entity）与数据模型”。
* **隔离底层异构性**：它将所有的 SQL 语句、数据库方言、表结构映射细节以及 ORM 框架的复杂性全部封装在 Shell 层内部。

---

### 2. 生命周期与触发时机

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 此阶段**完全不需要**编写 Shell Repository。业务核心只关心抽象的 `Repository` 契约，并通过内存中的模拟实现（如使用标准 `HashMap` 的 InMemory 适配器）完成全链路验证。


* **Phase 2（基础设施实现阶段）**：
* 当业务逻辑成熟并需要接入真实数据库时，才开始编写 Shell Repository。
* 它通过生产实现类（Production Implementation）与 Mapper 配合，作为适配器去实现 Use Case 定义的 Repository 契约。



---

### 3. 典型代码形态

在基础设施层中，它表现为一个特定于技术或框架的数据访问接口/组件：

```java
// 基础设施层（Shell）中封装底层数据库操作的 Vendor Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    // 直接操作数据库实体（Entity）的底层方法
    Optional<UserEntity> findByEmail(String email);
}

```

---

### 4. 协同工作机制（桥接 Use Case 与 Shell）

在 Shell 层中，Shell Repository 通常不直接暴露给业务核心，而是与 **Mapper** 和生产实现类（Production Implementation）协同工作，共同落地 Use Case Repository 契约：

```java
@Component
public class JpaSaveUserRepository extends SaveUserRepository {
    private final JpaUserRepository vendorRepository;
    private final Mapper<Context, Result, UserEntity, UserEntity> mapper;

    @Autowired
    public JpaSaveUserRepository(
            JpaUserRepository vendorRepository, 
            JpaUserMapper mapper) {
        this.vendorRepository = vendorRepository;
        this.mapper = mapper;
    }

    @Override
    protected Result doExecute(Context context) {
        // 1. 通过 Mapper 将业务上下文转换为数据库实体
        UserEntity entity = mapper.toPayload(context);
        
        // 2. 调用 Shell Repository 执行底层持久化操作
        UserEntity savedEntity = vendorRepository.save(entity);
        
        // 3. 通过 Mapper 将数据库实体转换回业务核心结果
        return mapper.toResult(savedEntity);
    }
}

```

---

### 5. 核心优势

* **数据库无感切换**：如果将来需要将关系型数据库（MySQL/JPA）替换为 NoSQL（MongoDB），**业务核心层的 Use Case 和 Repository 契约完全不用修改**，只需在 Shell 层编写一个新的 MongoDB 仓库及其对应的适配器和 Mapper 即可。
* **技术栈灵活演进**：如果底层数据访问方式需要从 Spring Data JPA 升级为纯 JDBC 或 jOOQ，只需修改 Shell Repository 内部的实现，外层的业务逻辑和契约不会受到任何波及。