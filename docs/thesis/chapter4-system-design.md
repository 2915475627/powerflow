# 第4章 系统设计

## 4.1 总体架构设计

PowerFlow 系统采用 DDD 六边形架构（端口-适配器模式）进行整体架构设计。系统的核心领域逻辑完全独立于外部基础设施，外部世界的所有交互——包括前端浏览器的 HTTP 请求、对数据库的持久化操作、对缓存系统的读写——均通过适配器与领域层进行通信。这种架构确保了核心业务逻辑的高度内聚和可测试性，同时允许外部依赖的自由替换而不影响领域层代码。

系统的分层架构自顶向下依次为：

前端层（Frontend Layer）基于 React + TypeScript + Vite 构建，使用 React Flow 实现 DAG 可视化编辑器，通过 React Query 管理服务端状态，通过 Tailwind CSS 实现页面样式。前端层通过 RESTful HTTP API 与后端进行通信，不包含任何业务逻辑，仅负责用户交互和数据展示。

入站适配器层（Inbound Adapter Layer）包含 REST API 控制器（Controller），接收来自前端的 HTTP 请求，将请求参数解析后委托给入站端口接口。控制器层负责 HTTP 协议相关的数据转换（如路径参数绑定、请求体反序列化、异常到 HTTP 状态码的映射），不包含任何业务逻辑。

入站端口层（Inbound Port Layer）定义了领域层向外暴露的能力接口。WorkflowUseCase 接口是系统唯一的入站端口，它声明了工作流执行、节点测试等核心业务操作的契约。入站端口的存在使得前端和领域层之间的接口契约固定，无论前端使用 React 还是 Vue，无论调用方式是 HTTP 还是消息队列，都不会影响领域层的实现。

领域层（Domain Layer）是整个系统的核心，包含所有业务逻辑和业务规则。领域层完全独立，不依赖任何外部组件，可以通过单元测试在不启动数据库、不启动 Web 服务器的情况下完成完整的核心逻辑验证。领域层包含的核心组件有：WorkflowExecutor 负责工作流的 DAG 构建、环检测、拓扑排序和执行流程控制；NodeExecutor 负责根据节点类型分发执行请求到对应的处理逻辑；ContextManager 负责节点输入输出与上下文数据之间的读写映射；RuleEvaluator 负责 SpEL 表达式的解析和求值；Domain Model 包含 Workflow、Node、Edge、Context、NodeExecution 等核心实体和值对象。

出站端口层（Outbound Port Layer）定义了领域层需要外部能力时的抽象接口。领域层定义了它需要什么，但不关心这些能力由谁提供。出站端口包括 WorkflowRepository（工作流数据的持久化和查询）、ExecutionLogRepository（节点执行记录的持久化）、NodeExecutorPort（节点处理能力的扩展点）。

出站适配器层（Outbound Adapter Layer）实现出站端口定义的接口，将领域层的抽象需求转化为具体的技术实现。JPA 适配器实现 WorkflowRepository 和 ExecutionLogRepository，将数据持久化到 PostgreSQL；Redis 适配器实现 Context 缓存功能；日志适配器负责将执行日志输出到标准日志框架。

## 4.2 核心域模型设计

### 4.2.1 Workflow（工作流聚合根）

Workflow 是系统的核心聚合根实体，它封装了一个完整工作流的所有属性和业务不变性。Workflow 的设计遵循 DDD 的聚合根模式：它是访问工作流内部节点的唯一入口，外界只能通过 Workflow 对象本身来操作节点和连线，而不能直接修改内部节点状态。

Workflow 的核心字段包括：id 为全局唯一标识符，使用 UUID 保证分布式环境下的唯一性；name 和 description 分别为工作流的名称和描述，供用户识别和管理；nodes 为节点集合，以 Map<String, Node> 的形式存储，键为节点 ID，值为节点对象，这种结构使得根据节点 ID 查找节点的复杂度为 O(1)；edges 为连线集合，以 List<Edge> 的形式存储，记录了节点之间的有向连接关系；startNodeId 标识了工作流的起始节点 ID，执行引擎从该节点开始遍历；enabled 布尔字段标识工作流是否处于启用状态，只有启用的工作流才能被触发执行。

Workflow 提供了 findNodeById() 和 findEdgesByFromNodeId() 两个查询方法，分别用于根据 ID 查找节点和根据源节点 ID 查找所有出向连线。这些方法封装了内部存储结构的访问细节，使得执行引擎可以通过流畅的接口与 Workflow 对象交互。

### 4.2.2 Node（节点实体）

Node 是工作流中的执行单元实体。每个 Node 包含以下属性：id 为节点唯一标识；name 为节点的可读名称，供前端展示和日志记录使用；type 为节点类型，取值为 NodeType 枚举的六个值之一；config 为节点配置数据，以 Map<String, Object> 的形式存储，不同类型的节点使用不同的配置结构，如 DATA_PROCESSING 节点在 config 中存储 transform 表达式和 outputKey；inputMapping 和 outputMapping 均为 Map<String, String> 类型，分别定义了从上下文到节点输入、从节点输出到上下文的键值映射关系。

Node 的设计将节点的"是什么"（类型和标识）与节点的"如何执行"（配置和映射）分离。同一种类型的节点可以有不同的配置和映射关系，从而实现不同的业务逻辑。例如，两个 DATA_PROCESSING 节点可以分别配置不同的 transform 表达式，实现不同的数据转换。

### 4.2.3 NodeType 枚举

NodeType 枚举定义了系统支持的全部六种节点类型。START 类型节点是工作流的入口，每个工作流有且仅有一个 START 节点，它不执行任何实际操作，仅作为拓扑排序的起点。END 类型节点是工作流的终点，每个工作流可以有多个 END 节点（表示不同的结束分支），它同样不执行任何实际操作，仅作为拓扑排序的终点。

DATA_PROCESSING 类型节点是系统的核心执行单元，它从 inputMapping 指定的上下文键取值作为输入数据，通过 config 中存储的 SpEL transform 表达式对数据进行变换处理，将结果写入 outputMapping 指定的上下文键中。CONDITION 类型节点根据 config 中存储的条件规则列表（每条规则包含 SpEL 布尔表达式和目标节点 ID）对上下文数据进行条件判断，动态确定下一跳节点。PARALLEL 类型节点没有实际业务逻辑，它的作用是标记一个并行执行区域的开始，从该节点出发的所有不以 JOIN 节点为目标的路径构成一个并行组。JOIN 类型节点是并行执行区域的汇合点，它等待所有从属于配对 PARALLEL 节点的分支执行完毕，然后汇总各分支的输出继续后续执行。

### 4.2.4 Edge（连线值对象）

Edge 是表示节点之间有向连接关系的值对象。每个 Edge 包含 fromNodeId（源节点 ID）、toNodeId（目标节点 ID）和可选的 condition（SpEL 条件表达式）三个字段。对于普通连线，condition 为空，执行引擎按照固定路径将控制权传递给下一节点；对于 CONDITION 节点发出的连线，condition 字段存储了该分支对应的 SpEL 布尔表达式，执行引擎通过求值这些表达式来确定哪条连线生效。

Edge 作为值对象，其相等性由其字段值决定而非对象引用决定。这一设计使得工作流的边集合可以用 List<Edge> 存储，在序列化/反序列化和数据传输场景下不会因为对象引用问题导致数据丢失。

### 4.2.5 Context（执行上下文）

Context 是工作流执行过程中各节点之间共享的数据载体，以 Map<String, Object> 的形式存储键值对数据。为了支持嵌套结构（如 user.name 访问用户对象的 name 属性），Context 在内部实现了嵌套值的解析逻辑：当遇到包含"."的键时，按照"."分隔的路径逐层访问嵌套 Map 中的值。

Context 的不可变性（Immutable）是设计中的一个重要选择。每次节点输出写入上下文时，ContextManager 不是在原 Context 的内部 Map 上进行修改，而是创建一个包含新数据的新 Context 对象。这种不可变设计使得执行历史的回溯变得极为简单——每个节点的输入输出对应一个独立的 Context 快照，不存在共享可变状态导致的数据竞争问题。

### 4.2.6 NodeExecution（节点执行记录）

NodeExecution 是记录每个节点执行详情的实体，它贯穿整个执行过程的生命周期。NodeExecution 的主要字段包括：id 为执行记录的全局唯一标识；workflowExecutionId 为所属工作流执行实例的 ID，用于将同一工作流执行的多个节点执行记录归为一组；nodeId 为被执行的节点 ID；status 为执行状态，取值为 SUCCESS 或 FAILED；inputData 和 outputData 分别记录节点执行时的输入和输出，均以 JSONB 格式存储；error 字段在执行失败时存储错误消息；durationMs 记录执行耗时毫秒数；startTime 和 endTime 记录执行的开始和结束时间戳。

NodeExecution 实体完全由 AOP 切面自动创建和填充，节点执行器的业务逻辑无需关心日志记录的细节。这种设计确保了日志记录的一致性和完整性——无论节点执行成功还是失败，AOP 切面都会执行，不会因为节点逻辑中的异常路径而遗漏日志。

## 4.3 数据库设计

PowerFlow 系统的数据持久化层基于 PostgreSQL 数据库实现，使用 Spring Data JPA 作为 ORM 框架。数据库中共设计了两张核心表。

workflows 表用于存储工作流的基本信息和结构数据。其主键 id 使用 VARCHAR(36) 类型存储 UUID 字符串；name 和 description 分别存储工作流名称和描述；nodes 和 edges 两个字段使用 JSONB 类型存储，分别序列化存储节点 Map 和边 List 的完整 JSON 结构，JSONB 类型使得复杂嵌套的节点配置数据可以直接存储和检索而无需额外的编码解码逻辑；start_node_id 存储工作流的起始节点 ID；enabled 布尔字段标识启用状态；created_at 和 updated_at 使用 TIMESTAMP 类型记录创建和更新时间戳。

node_executions 表用于存储每个节点执行记录的详细信息。其主键 id 同样使用 VARCHAR(36) 存储 UUID；workflow_execution_id 标识所属的工作流执行实例 ID（注意这与 workflows 表的主键无关，是一个逻辑执行实例 ID）；node_id 存储被执行的节点 ID；status 存储执行状态字符串；input_data 和 output_data 使用 JSONB 类型存储节点执行前后的完整数据快照；error 字段在执行失败时存储错误堆栈信息；duration_ms 使用 BIGINT 类型存储执行耗时毫秒数；start_time 和 end_time 使用 TIMESTAMP 类型存储执行时间戳。

## 4.4 节点配置设计

不同类型的节点使用不同的 config 配置结构，下面分别说明。

DATA_PROCESSING 节点的 config 包含两个字段：transform 为 SpEL 表达式字符串，描述输入数据的转换逻辑，例如 `input.amount * input.taxRate` 表示将输入中的 amount 字段与 taxRate 字段相乘；outputKey 为字符串，指定将计算结果写入上下文的目标键名。节点执行时，RuleEvaluator 将 inputMapping 从上下文中解析出的数据绑定为 `input` 变量，执行 transform 表达式的求值，最后将结果以 outputKey 为键写入上下文。

CONDITION 节点的 config 包含两个字段：conditions 为一个数组，每项包含 expression（SpEL 布尔表达式字符串）和 nextNodeId（满足该条件时跳转的目标节点 ID）；defaultNextNodeId 为字符串，指定当所有条件表达式均不满足时跳转的默认目标节点 ID。执行时，RuleEvaluator 按 conditions 数组顺序对各 expression 求值，第一个返回 true 的表达式所指向的 nextNodeId 即为下一跳；若所有表达式均为 false，则跳转到 defaultNextNodeId。

PARALLEL 节点和 START/END 节点不执行实际操作，其 config 字段为空 Map 或仅包含描述性字段。JOIN 节点的 config 可选包含 waitAll 布尔字段（默认为 true），标识是否等待所有并行分支完成。

## 4.5 执行引擎核心算法设计

执行引擎是 PowerFlow 系统的核心，其算法设计需要解决三个核心问题：如何在执行前验证工作流的合法性、如何确定节点的执行顺序、如何正确地在节点之间传递数据并支持并行分支。

执行引擎的工作流程分为五个阶段。在接收阶段，引擎接收工作流实例和初始上下文作为输入参数。在验证阶段，引擎首先构建工作流的 DAG 邻接表表示，然后使用 DFS 着色算法检测图中是否存在环——如果检测到环，则立即抛出 WorkflowValidationException 异常并拒绝执行，确保工作流不会陷入无限循环。在排序阶段，引擎使用 Kahn 算法对 DAG 进行拓扑排序，得到一个节点 ID 的线性序列，该序列保证了对于任意一条有向边 (u, v)，节点 u 在节点 v 之前出现。在执行阶段，引擎按照拓扑序遍历所有节点，对每个节点执行以下操作：首先由 ContextManager 根据节点的 inputMapping 从当前上下文中提取数据，构建节点的输入；然后 AOP 切面记录节点执行的开始时间；接着由 NodeExecutor 根据节点类型执行相应的业务逻辑；之后 AOP 切面记录结束时间并保存 NodeExecution 记录；最后由 ContextManager 根据节点的 outputMapping 将输出数据写入上下文，更新当前上下文状态。在返回阶段，引擎返回包含最终上下文和工作流执行 ID 的执行结果对象。

对于并行分支的处理，执行引擎采用 PARALLEL+JOIN 配对模型。当拓扑序遍历遇到 PARALLEL 节点时，引擎识别出所有以该 PARALLEL 节点为起点的非 JOIN 路径，构成并行组。引擎使用 Java 的 CompletableFuture 并发提交各并行分支的执行任务，各分支独立地按照各自的拓扑子序执行，直到遇到 JOIN 节点。JOIN 节点检查所有并行分支的执行状态，若全部成功则继续后续执行；若任意分支失败，则立即停止整个工作流的执行。
