# 第2章 相关技术介绍

## 2.1 Java Spring Boot 与 AOP

Spring Boot 是由 Pivotal 团队提供的 Spring 框架脚手架，通过自动配置（Auto-Configuration）机制大幅简化了 Spring 应用的初始搭建和开发过程。Spring Boot 遵循"约定优于配置"的理念，内置了 Tomcat、Jetty 等 Servlet 容器，开发人员无需额外部署 WAR 文件即可直接运行 JAR 包形式的 Web 应用。Spring Boot 的 starter 依赖机制允许开发者通过添加少量的依赖声明，自动获得框架各层所需的完整技术栈，这种模块化的依赖管理方式既保证了版本的兼容性，又避免了依赖配置的复杂性。

Spring AOP（Aspect-Oriented Programming）是 Spring 框架的核心功能之一，它通过切面（Aspect）的概念将分散在多个业务模块中的横切关注点（Cross-cutting Concerns）进行统一管理。在传统的面向对象编程中，日志记录、安全检查、事务管理等横切关注点需要侵入到每一个业务方法中，导致业务逻辑与基础设施逻辑高度耦合。AOP 通过将横切关注点抽取为独立的切面类，在编译期或运行期将切面逻辑织入（Weave）到目标方法的前后左右，实现了业务逻辑与非业务逻辑的有效分离。

在 PowerFlow 系统中，AOP 技术被应用于节点执行过程的日志记录。通过定义 NodeExecutionAspect 切面类，使用 @Around 通知环绕 NodeExecutor.execute() 方法，在方法执行前后自动记录开始时间、结束时间、执行耗时、输入输出数据等执行上下文信息，无需在每个节点类型的执行逻辑中显式编写日志代码，实现了执行日志的零侵入记录。

## 2.2 领域驱动设计与六边形架构

领域驱动设计（Domain-Driven Design，DDD）是由 Eric Evans 在其同名著作中提出的一种软件开发方法论，它强调软件系统的设计应当以业务领域为核心，通过与领域专家的密切协作，建立一套反映业务语义的统一语言（Ubiquitous Language），并在此基础上构建包含聚合根（Aggregate Root）、实体（Entity）、值对象（Value Object）、领域服务（Domain Service）等元素的领域模型。DDD 的核心价值在于将业务复杂度与技术复杂度分离，使软件系统能够更灵活地应对业务需求的变化。

六边形架构（Hexagonal Architecture），又称端口与适配器模式（Ports and Adapters），是由 Alistair Cockburn 提出的一种软件架构风格。六边形架构的核心思想是将应用程序划分为两个区域：核心业务逻辑区（内部六边形）和外部适配器区（外部六边形）。核心业务逻辑完全独立于外部世界，不依赖任何基础设施组件；外部世界的所有交互——无论是来自用户界面、REST API、消息队列还是数据库——都必须通过一个明确定义的端口（Port）接口与核心业务逻辑进行通信。适配器（Adapter）负责将外部请求转换为核心业务逻辑可以理解的格式，并将核心业务逻辑的输出转换为外部世界可以接受的格式。

六边形架构天然支持依赖倒置原则（Dependency Inversion Principle），即高层模块不依赖低层模块，两者都依赖抽象。在六边形架构中，核心业务逻辑定义了它需要外部世界提供什么能力（入站端口和出站端口），而具体的实现细节——比如数据是存储在 PostgreSQL 还是 MySQL、是使用 HTTP 还是消息队列进行通信——完全由外部适配器决定。这种架构使得系统的核心业务逻辑可以在没有数据库、没有 Web 服务器的情况下进行单元测试，极大地提升了系统的可测试性和可维护性。

PowerFlow 系统采用 DDD 六边形架构进行设计，核心业务逻辑完全内聚在领域层，包括工作流执行引擎（WorkflowExecutor）、节点执行器（NodeExecutor）、上下文管理器（ContextManager）和规则求值器（RuleEvaluator）。外部世界的所有交互——包括来自前端管理后台的 HTTP 请求、对 PostgreSQL 的持久化操作、对 Redis 的缓存操作——均通过对应的适配器与领域层进行通信。

## 2.3 React 与 TypeScript

React 是由 Facebook 开发的用于构建用户界面的 JavaScript 库，它采用组件化（Component-based）的开发范式，将界面拆分为一系列独立、可复用的组件，每个组件维护自己的状态（State）和属性（Props），通过声明式（Declarative）的编程风格描述界面的最终形态。React 的核心理念之一是虚拟 DOM（Virtual Document Object Model），它将真实 DOM 的操作先映射到内存中的虚拟 DOM 树上，通过对比（Diffing）算法找出最小的更新补丁（Patch），批量应用到真实 DOM 上，从而避免了频繁操作真实 DOM 带来的性能开销。React 16.8 引入的 Hooks 机制进一步简化了状态逻辑的复用，开发者无需编写类组件即可在函数式组件中使用状态和生命周期功能。

TypeScript 是微软开发的 JavaScript 的超集，它在 JavaScript 之上添加了静态类型检查系统。TypeScript 的类型系统能够在编译阶段发现类型相关的错误，减少运行时类型错误的发生；同时，类型注解（Type Annotation）作为一种文档形式，使代码的接口契约更加明确，提升了代码的可读性和可维护性。TypeScript 支持泛型（Generics）、接口（Interface）、类型别名（Type Alias）、交叉类型（Intersection Types）等高级类型特性，能够精确地描述复杂的数据结构。React 与 TypeScript 的组合——通常称为 React + TS——是当前前端开发的主流技术栈之一，它兼顾了开发效率和运行时安全性。

## 2.4 React Flow

React Flow 是一个基于 React 的图形编辑器库，专门用于构建节点图编辑器（Node-based Graph Editor）。与通用的 SVG 绑定库不同，React Flow 提供了开箱即用的节点（Node）、边（Edge）、连杆（Handle）等图形元素，以及缩放、平移、拖拽、选择、分组等交互功能。React Flow 使用函数式的方式管理图形状态，通过 `useNodesState` 和 `useEdgesState` 两个 Hook 管理节点和边的状态数据，通过 `addEdge` 和 `onNodesChange`、`onEdgesChange` 等回调函数处理交互事件。开发者可以通过自定义节点组件（Custom Node）来实现任意视觉形态和工作逻辑的节点，通过自定义边组件（Custom Edge）来展示不同的连线样式，如条件边上显示表达式标签。React Flow 的设计哲学强调灵活性和可扩展性，它不预设任何特定的节点类型或布局算法，而是将这些决策留给开发者根据具体业务场景来决定。

在 PowerFlow 系统中，React Flow 被用于实现 DAG 可视化编辑器。用户可以在画布上拖拽不同类型的节点，通过连线将节点串联成完整的工作流图形，系统自动管理节点的布局和边的路由，节点配置面板与图形编辑器联动，实时展示和修改节点属性。

## 2.5 DAG 拓扑排序算法

有向无环图（Directed Acyclic Graph，简称 DAG）是一种特殊的图结构，其中包含若干顶点和有向边，每条边连接两个顶点，且图中不存在任何环（即从某个顶点出发沿着有向边无法回到该顶点本身）。DAG 广泛用于表示任务调度、依赖管理、版本控制等场景。在工作流执行引擎中，工作流的节点和连线天然构成一个 DAG——每个节点代表一个执行步骤，每条边代表节点之间的执行顺序或数据传递关系，且工作流中不允许出现环（循环执行），否则将导致执行无法终止。

拓扑排序（Topological Sorting）是处理 DAG 的核心算法之一，它将 DAG 中的所有顶点排列为一个线性序列，使得对于每一条有向边 (u, v)，顶点 u 都出现在顶点 v 之前。常用的拓扑排序算法有两种：Kahn算法（Kahn's Algorithm）和深度优先搜索算法（DFS-based Algorithm）。Kahn算法的基本思想是：维护一个入度（Indegree）为零的顶点队列，每次从队列中取出一个顶点，将其加入拓扑序列，并删除该顶点的所有出边（即目标顶点的入度减一），如果目标顶点的入度变为零则加入队列，重复直到队列为空。DFS算法则是对图进行深度优先搜索，在 DFS 的退出顺序（post-order）的逆序即为拓扑序列。

在实际的工作流执行引擎中，拓扑排序用于确定节点的执行顺序：只有当一个节点的所有前驱节点都执行完毕，才能够开始执行该节点。此外，在执行之前必须对工作流进行环检测（Cycle Detection），如果图中存在环，则工作流无法正常执行，应当在执行前抛出异常并拒绝执行。环检测可以通过在 DFS 过程中记录节点的访问状态来实现——若在 DFS 递归栈中发现已访问的节点，则说明存在环。

## 2.6 SpEL 规则引擎

SpEL（Spring Expression Language）是 Spring 框架子项目 Spring Expression Language 的简称，它提供了一种强大的表达式求值语言，用于在运行时解析和求值字符串形式的表达式。SpEL 的语法脱胎于 Unified EL（Enterprise JavaBeans 3.0 规范中引入的表达式语言），支持属性访问、方法调用、类型转换、三元运算符、集合投影、集合选择等丰富的表达式特性。与其他规则引擎（如 Drools）相比，SpEL 足够轻量，内嵌于 Spring 框架中无需额外引入依赖，学习曲线平缓，适合在代码中嵌入业务规则表达式。

在 PowerFlow 系统中，SpEL 被应用于两个关键场景：节点输入输出映射和数据转换。在 DATA_PROCESSING 节点中，开发者可以通过 SpEL 表达式描述数据转换逻辑，例如 `input.amount * input.taxRate` 表示将输入数据中的 amount 和 taxRate 相乘作为输出；在 CONDITION 节点中，每个条件分支关联一个 SpEL 布尔表达式，系统运行时根据上下文数据求值各表达式的结果来确定执行路径。SpEL 求值器将上下文数据绑定为变量（如 `input` 指向节点的输入 Map），通过 SpelExpressionParser 解析表达式字符串，使用 StandardEvaluationContext 设置变量值，最终由 Expression.getValue() 返回求值结果。

## 2.7 PostgreSQL 与 Redis

PostgreSQL 是一款功能强大的开源对象关系型数据库管理系统，它支持 SQL 标准的大部分特性，并在此基础上进行了大量扩展。PostgreSQL 的 JSONB（Binary JSON）数据类型允许在数据库中直接存储和索引 JSON 文档，相比传统的 JSON 文本类型，JSONB 以二进制形式存储并预先解析，支持 GIN 索引的全文检索和成员包含查询。在 PowerFlow 系统中，工作流的节点图结构（nodes 和 edges）以 JSONB 格式存储在 workflows 表中，这种设计使得前端传递的复杂嵌套结构可以无缝地持久化到数据库中，同时保留了关系型数据库的事务特性、外键约束和查询优化能力。

Redis 是一个基于内存的键值存储系统，它支持字符串、哈希（Hash）、列表（List）、集合（Set）、有序集合（Sorted Set）等多种数据结构，同时提供了过期时间、发布订阅、事务、Lua 脚本等高级特性。Redis 的读写性能远高于传统磁盘数据库，单实例 QPS（Queries Per Second）可达十万级别，因此常被用作缓存层或消息队列。在 PowerFlow 系统中，Redis 被用于缓存工作流执行过程中的 Context 数据，将高频访问的上下文数据保留在内存中，减少对 PostgreSQL 的访问压力，提升执行引擎的数据读写效率。
