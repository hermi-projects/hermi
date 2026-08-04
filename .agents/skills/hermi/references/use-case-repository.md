# Use Case Repository

在 **Hermi 架构框架**中，**Use Case Repository**（通常简称为 **Repository**）是核心业务层（Use Case）用于与数据持久化层（如数据库、缓存等）进行交互的抽象契约。

它是 Hermi 实现“业务核心与基础设施解耦”的关键支柱。以下是对 Use Case Repository 的全面介绍：

---

### 1. 核心职责：定义“持久化意图”而非“技术实现”

在执行纯业务逻辑时，业务经常需要保存数据、查询历史状态或更新记录。

* **业务导向**：`Repository` 契约只关心“业务上需要持久化什么上下文（Context）”**以及**“持久化操作完成后返回什么结果（Result）”（例如返回新生成的主键 ID 或确认状态）。
* **隔离底层技术**：它完全不知道底层使用的是 MySQL、MongoDB、Redis 还是 JPA、JDBC，也不知道使用的是关系型数据库还是文档型数据库。

---

### 2. 双阶段演变生命周期

遵循 Hermi 的“Discovery Lifecycle”：

* **Phase 1（纯 Java 核心发现阶段）**：
* 在 Use Case 模块中定义抽象类（例如 `SaveUserRepository`）。
* 编写业务逻辑时直接调用它。
* 本地测试（Main Shell）时，编写一个简单的内存实现（例如 `InMemorySaveUserRepository`，内部用标准的 `HashMap` 存数据），**无需启动任何数据库或容器**即可完成快速验证。


* **Phase 2（基础设施实现阶段）**：
* 在外层 Shell（如 Spring Boot）中编写真正的技术实现。
* 通过 **Mapper** 和 **Vendor Repository**（如 Spring Data JPA 的 `JpaRepository`）将业务契约落地到真实的持久化存储中。



---

### 3. 典型代码形态

在业务核心层中，它表现为一个继承自框架基类的抽象类：

```java
// 纯业务核心层定义的 Repository 契约
public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
    public record Context(String name, String email) {}
    public record Result(String id){}
}

```

---

### 4. Phase 2 的具体实现：协同工作机制

在基础设施层（Shell），为了适配具体的持久化技术（如 JPA），`Repository` 的落地依赖于**生产实现类**、**Vendor Repository** 和 **Mapper** 的协同：

* **`JpaUserRepository`（Vendor Repository）**：特定于框架的底层数据访问接口（例如 Spring Data JPA 的 `JpaRepository<UserEntity, Long>`），直接操作数据库实体。
* **`JpaUserMapper`（Mapper）**：负责数据转换：
* 将 Use Case 的 `Repository.Context` 映射为数据库实体 (`UserEntity`)。
* 将数据库保存后返回的实体映射回 Use Case 所需的 `Repository.Result`（如带有 ID 的结果）。


* **`JpaSaveUserRepository`（生产实现类）**：继承自 `SaveUserRepository`，作为适配器将上述两部分串联起来：
```java
@Component
public class JpaSaveUserRepository extends SaveUserRepository {
    private final JpaUserRepository jpaRepository;
    private final Mapper<Context, Result, UserEntity, UserEntity> mapper;

    // 构造函数注入...

    @Override
    protected Result doExecute(Context context) {
        UserEntity entity = mapper.toPayload(context);
        UserEntity savedEntity = jpaRepository.save(entity);
        return mapper.toResult(savedEntity);
    }
}

```

---

### 5. 核心优势

* **内存级毫秒级测试**：在 Phase 1 中，利用内存 `Map` 作为适配器，所有的业务状态变更测试都在内存中完成，**不需要 H2 内存数据库、不需要 Docker 容器、更不需要 Mock 框架**。
* **技术栈灵活替换**：如果将来需要将关系型数据库（JPA/MySQL）换成 NoSQL（MongoDB），**业务核心层的 Use Case 和 `SaveUserRepository` 契约完全不用修改**，只需在 Shell 层实现一套新的 MongoDB 适配器即可。