
# Shell Controller

在 **Hermi 架构框架**中，**Shell Controller** 是位于 **Phase 2（Shell 层 / 基础设施层）** 中负责处理外部 HTTP 请求、遵循 RESTful API 标准进行 Web 协议接入与路由映射的技术组件。

它是 Hermi 实现“Web 框架与底层协议细节与业务核心解耦”的重要落地支撑。以下是对 Shell API 的全面介绍：

---

### 1. 核心职责：定义“技术实现”而非“业务意图”

在接收真实的外部 Web 请求时，Shell API 必须处理具体的网络协议和框架注解（例如使用 Spring MVC 的 `@RestController`、Quarkus REST 或 Micronaut HTTP），并严格遵循 RESTful API 设计规范。

* **技术导向**：`Shell API` 专注于“如何映射 RESTful 资源路径、使用正确的 HTTP 动词（GET/POST/PUT/DELETE）、解析 URI 路径参数（Path Variable）、查询参数（Query Param）及请求体（Request Body）”**以及**“返回符合标准的 HTTP 状态码与响应载荷”。
* **隔离底层异构性**：它将所有的 Servlet 容器细节、HTTP 协议异常、JSON 序列化注解（如 Jackson）以及框架特定的绑定逻辑全部封装在 Shell 层内部。

---

### 2. 生命周期与触发时机

遵循 Hermi 的探索式生命周期：

* **Phase 1（纯 Java 核心发现阶段）**：
* 此阶段**完全不需要**编写 Shell API。业务核心只关心抽象的 Use Case 及其 `Context`，并通过本地测试（Shell Test / Main Shell）完成全链路验证。


* **Phase 2（基础设施实现阶段）**：
* 当业务逻辑成熟并需要对外暴露符合 RESTful 标准的 Web 服务时，才开始编写 Shell API。
* 它作为驱动适配器，负责接收外部 HTTP 请求并将其转化为对 Use Case 或中间服务（Service）的调用。



---

### 3. 典型代码形态

在基础设施层中，它表现为一个符合 RESTful 规范的 Web 框架控制器组件：

```java
// 基础设施层（Shell）中封装符合 RESTful 标准的 Web 路由与 HTTP 请求处理的 Shell API
@RestController
@RequestMapping("/api/v1/users")
public class FindUserApiShell {
    private final FindUserService findUserService;

    @Autowired
    public FindUserApiShell(FindUserService findUserService) {
        this.findUserService = findUserService;
    }

    @GetMapping("/{ssn}")
    public ResponseEntity<FindUserUseCase.Result> findUser(@PathVariable String ssn) {
        // 1. 将 RESTful 的路径参数组装为业务 Use Case 的 Context
        FindUserUseCase.Context context = new FindUserUseCase.Context(ssn);
        
        // 2. 委托给服务层执行
        FindUserUseCase.Result result = findUserService.findUser(context);
        
        // 3. 返回符合 RESTful 标准的 HTTP 状态码（如 200 OK）
        return ResponseEntity.ok(result);
    }
}

```

---

### 4. 协同工作机制（桥接外部流量与 Use Case）

在 Shell 层中，Shell API 通常不直接包含核心业务编排，而是与 **Service 层**（负责处理事务等框架横切关注点）协同工作，最终触发 Use Case 的执行：

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

* **严格遵循标准与契约分离**：通过 RESTful API 标准对外提供清晰统一的资源访问契约，而底层 HTTP 动词和状态码的变化完全被隔离在 Shell API 中，绝不污染核心业务。
* **多通道交付与无感扩展**：如果将来需要将相同的核心业务通过不同的通道暴露（例如同时支持 Shell API 和 Shell Consumer），**业务核心层的 Use Case 完全不用修改**，只需在 Shell 层编写一个新的入口适配器即可。
* **技术栈灵活演进**：如果底层 Web 框架需要从 Spring MVC 升级为 Spring WebFlux 或迁移到 Quarkus，只需修改 Shell API 内部的实现，外层的业务逻辑和契约不会受到任何波及。