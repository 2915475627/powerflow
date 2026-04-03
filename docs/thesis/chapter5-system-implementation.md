# 第5章 系统实现

## 5.1 后端实现

### 5.1.1 项目结构

PowerFlow 后端项目基于 Spring Boot + Maven 构建，采用 DDD 六边形架构组织代码。项目的包结构按照功能职责划分，最顶层为 com.powerflow.workflow，其下划分为 domain、adapter、aop 三个顶级包。

domain 包是系统的核心业务逻辑层，其下按照 DDD 的战术设计进一步划分为 model、service 和 port 三个子包。model 子包包含所有领域模型类，其中 enums 子包定义了 NodeType 和 ExecutionStatus 两个枚举类型，Workflow.java、Node.java、Edge.java、Context.java 和 NodeExecution.java 五个文件分别定义了系统的五个核心领域对象。service 子包包含四个领域服务类：WorkflowExecutor 是工作流执行引擎的核心实现类；NodeExecutor 负责根据节点类型分发执行请求；ContextManager 负责上下文的读写和映射；RuleEvaluator 负责 SpEL 表达式的解析和求值。port 子包定义了系统所有的端口接口，其中 inbound 子包包含入站端口 WorkflowUseCase，outbound 子包包含出站端口 WorkflowRepository、ExecutionLogRepository 和 NodeExecutorPort。

adapter 包是基础设施适配器层，其下按照Inbound/Outbound 和技术实现进一步划分。adapter/inbound/rest 子包包含 REST API 的控制器类，如 WorkflowController 处理工作流的 CRUD 请求和执行触发，NodeController 处理节点测试请求。adapter/outbound/persistence 子包包含 JPA 实体类和 Spring Data JPA Repository 接口实现，负责数据到 PostgreSQL 的持久化。adapter/outbound/cache 子包包含 Redis 缓存适配器，负责 Context 数据的缓存读写。adapter/outbound/logging 子包包含日志适配器，将执行日志输出到标准日志框架。

aop 包包含 NodeExecutionAspect 切面类，使用 @Aspect 和 @Component 注解注册为 Spring Bean，通过 @Around 通知环绕 NodeExecutor.execute() 方法实现节点执行过程的自动日志记录。

### 5.1.2 关键实现

**WorkflowExecutor 的实现**。WorkflowExecutor 是工作流执行引擎的核心服务类，使用 @Service 注解注册为 Spring Bean。它的 execute() 方法是执行引擎的入口签名，接收 Workflow 对象和 Context 对象作为参数，返回 WorkflowExecutionResult 对象。在 execute() 方法内部，首先调用 buildDAG() 方法根据 workflow.getEdges() 构建邻接表表示的 DAG 结构，使用 HashMap<String, List<String>> 存储每个节点的出向邻居列表。然后调用 hasCycle() 方法使用 DFS 着色算法检测环——在 DFS 递归过程中维护节点状态数组（白色表示未访问、灰色表示访问中、黑色表示已完成），若遇到灰色节点则说明存在环。接着调用 topologicalSort() 方法使用 Kahn 算法得到拓扑序列，该方法维护一个入度为 O(1) 查询的节点入度映射，每次选取入度为零的节点加入结果序列并更新其邻居的入度，循环直到队列为空。最后按拓扑序遍历执行节点，调用 NodeExecutor.execute() 执行每个节点，由 ContextManager.applyOutputMapping() 更新上下文状态，若任何节点执行失败则立即抛出 WorkflowExecutionException 终止执行。

**NodeExecutor 的实现**。NodeExecutor 使用策略模式处理不同类型的节点。它的 execute() 方法接收 Node 对象和 Context 对象作为参数，通过 Java 14 的 switch 表达式（Pattern Matching for Switch）根据 node.getType() 分发到不同的处理方法。START 和 END 类型节点执行最简单的空操作直接返回成功结果。DATA_PROCESSING 类型节点首先调用 ContextManager.resolveInput() 获取节点输入数据，然后调用 RuleEvaluator.evaluate() 对 transform 表达式求值，将结果写入 outputKey 指定的输出字段，最后调用 ContextManager.applyOutputMapping() 将结果写入上下文。CONDITION 类型节点遍历 config 中的条件规则列表，对每个 expression 调用 RuleEvaluator.evaluate() 进行 SpEL 布尔求值，找到第一个返回 true 的条件后返回对应 nextNodeId 作为执行结果。PARALLEL 类型节点使用 CompletableFuture.allOf() 并发执行所有并行分支，然后等待所有分支完成。JOIN 类型节点等待 CountDownLatch 倒计数为零后汇总所有分支的输出。

**RuleEvaluator 的实现**。RuleEvaluator 使用 Spring 的 SpEL API 完成表达式求值。类内部维护一个 SpelExpressionParser 实例（线程安全，可复用），每次求值时通过 parser.parseExpression() 将表达式字符串解析为 Expression 对象，创建一个 StandardEvaluationContext 并通过 setVariable() 方法将上下文数据绑定为命名变量，最后调用 exp.getValue(evalContext) 返回求值结果。对于布尔表达式（如 CONDITION 节点中的条件），返回值为 Boolean 类型；对于数值或字符串表达式（如 DATA_PROCESSING 节点中的 transform），返回值为实际的计算结果类型。

**ContextManager 的实现**。ContextManager 处理节点输入输出与执行上下文之间的映射逻辑。resolveInput() 方法遍历节点的 inputMapping（Map<String, String>），其中 key 为上下文中的源数据键名，value 为节点输入的参数名，从 context.getData() 中提取对应的值构建节点的输入 Map，若遇到嵌套键（如 user.name）则递归解析嵌套 Map。applyOutputMapping() 方法遍历节点的 outputMapping（Map<String, String>），其中 key 为节点输出字段名，value 为上下文中的目标键名，从 NodeResult 的输出数据中提取对应字段值，写入新的上下文数据 Map 并返回新的 Context 实例。

**NodeExecutionAspect 的实现**。NodeExecutionAspect 使用 Spring AOP 的 @Aspect 和 @Around 注解，指定切点表达式为 execution(* NodeExecutor.execute(..))，确保对 NodeExecutor 的所有 execute() 调用都会被该切面拦截。切面的 around() 方法在方法执行前记录 startTime，在方法执行后（无论成功还是异常）记录 endTime，计算 durationMs，从 JoinPoint 中获取方法参数（node 和 context）以及方法返回值（NodeResult），最后调用 ExecutionLogRepository 将执行记录持久化。若方法执行抛出异常，则在 error 字段中存储异常消息而非结果数据。

### 5.1.3 REST API 设计

系统后端提供了完整的 RESTful API 供前端调用。API 的设计遵循资源导向原则，将工作流和执行记录作为核心资源进行建模。

工作流相关的 API 包括：POST /api/workflows 接收工作流的名称、描述、节点集合、边集合和起始节点 ID，创建新工作流并返回其 ID；GET /api/workflows 返回所有工作流的列表（不含完整的节点和边数据，仅返回基本信息）；GET /api/workflows/{id} 返回指定工作流的完整信息（包括节点 Map 和边 List）；PUT /api/workflows/{id} 接收更新后的工作流数据，更新数据库中对应记录；DELETE /api/workflows/{id} 删除指定工作流及其所有关联数据。

执行相关的 API 包括：POST /api/workflows/{id}/execute 接收初始上下文 JSON 数据，触发指定工作流的执行，返回执行结果（包含执行 ID、最终上下文、节点执行记录列表）；POST /api/workflows/{id}/nodes/{nodeId}/test 接收测试上下文 JSON，对指定工作流中的指定节点进行独立求值测试，返回节点的输出结果和执行耗时，用于开发调试。

历史查询相关的 API 包括：GET /api/workflows/{id}/executions 返回指定工作流的所有历史执行记录列表（分页）；GET /api/executions/{executionId} 返回指定执行实例的完整详细信息。

## 5.2 前端实现

### 5.2.1 技术栈与项目结构

前端项目基于 React 18 + TypeScript + Vite 构建，使用 React Flow 实现 DAG 可视化编辑器，使用 React Query 实现服务端状态的管理和缓存，使用 Tailwind CSS 实现页面样式。项目的页面级组件位于 src/pages/ 目录下，包括 WorkflowsPage（工作流列表管理页面）、WorkflowEditorPage（DAG 可视化编辑页面）、NodeConfigPage（节点配置管理页面）、NodeTestPage（节点独立测试页面）和 TriggerLogsPage（执行日志与历史查询页面）。组件级代码位于 src/components/ 目录下，其中 Layout.tsx 提供全局页面布局框架，NodeConfigPanel.tsx 提供节点配置面板组件，WorkflowExecutionTimeline.tsx 提供执行历史的可视化时间轴组件，nodes/ 子目录下存放各类型节点的自定义可视化组件。

### 5.2.2 DAG 可视化编辑器实现

WorkflowEditorPage 是前端最核心的页面，它使用 React Flow 的 Hook 和组件库构建交互式 DAG 编辑画布。页面初始化时通过 useQuery 获取指定工作流的完整数据（包括节点 Map 和边 List），将数据转换为 React Flow 所需的节点和边的格式（{id, type, position, data} 格式的节点对象和 {id, source, target, label} 格式的边对象），分别通过 useNodesState 和 useEdgesState 管理状态。

工具栏区域为每种节点类型（START、END、DATA_PROCESSING、CONDITION、PARALLEL、JOIN）提供添加按钮，点击后使用 React Flow 提供的 addNodes 方法向画布添加新节点，节点的初始位置为画布中心。节点之间的连线通过 React Flow 的拖拽连接交互实现——用户从源节点的输出连杆（handle）拖拽到目标节点的输入连杆，系统自动调用 onConnect 回调创建新的边对象。对于 CONDITION 类型的节点，边的 label（标签）字段用于存储条件表达式，前端在边上通过自定义边组件渲染表达式的文字描述。

节点选中后，右侧的 NodeConfigPanel 面板自动同步展示该节点的配置信息。面板根据节点类型动态渲染不同的表单元素：DATA_PROCESSING 节点显示 transform 表达式输入框和 outputKey 输入框；CONDITION 节点显示条件规则列表编辑器（可动态添加/删除条件行）；START 和 END 节点仅展示节点名称和类型信息，不可编辑。用户修改配置后，数据实时同步回 React Flow 的节点状态中，并通过 React Query 的 mutate 同步到后端数据库保存。

画布支持缩放（通过 React Flow 内置的 Controls 组件）和平移（通过鼠标拖拽画布背景）操作。React Flow 的 Background 组件提供网格背景增强视觉定位感，MiniMap 组件提供小地图导航辅助。

### 5.2.3 节点测试功能实现

NodeTestPage 提供了一个隔离的节点测试环境，允许开发者在不触发完整工作流执行的情况下验证节点逻辑的正确性。页面分为三个区域：左侧为 Context 输入区，提供一个 JSON 格式的文本编辑框，用户在此输入测试用的上下文数据（键值对 JSON 对象）；中间为操作区，提供一个"执行"按钮，点击后调用 POST /api/workflows/{workflowId}/nodes/{nodeId}/test 接口，将工作流 ID、节点 ID 和上下文 JSON 作为请求体发送测试请求；右侧为结果展示区，以 JSON 格式展示节点的输出结果，同时以文字形式展示执行状态（SUCCESS/FAILED）和耗时毫秒数。

NodeTestPage 的实现使用了 React Query 的 useMutation 来处理测试请求，通过 onSuccess 回调更新结果区域的状态。页面加载时通过 useQuery 获取指定节点当前配置，用于在测试前向用户展示即将被执行节点的当前配置信息。
