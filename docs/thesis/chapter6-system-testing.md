# 第6章 系统测试

## 6.1 测试环境

本系统的测试工作分为单元测试、集成测试和性能测试三个层次，在以下环境中进行。

单元测试和集成测试的环境配置为：硬件平台为 MacBook Pro（Apple M3 芯片，16GB RAM）；操作系统为 macOS 14 Sonoma；JDK 版本为 OpenJDK 17.0.9；Maven 版本为 Apache Maven 3.9.6；Spring Boot 版本为 3.2.1；数据库为 PostgreSQL 15（运行于本地 Docker 容器）；缓存为 Redis 7（运行于本地 Docker 容器）。单元测试使用 JUnit 5 框架配合 AssertJ 断言库和 Mockito 模拟框架，在不启动真实数据库和 Redis 的情况下完成核心业务逻辑的验证。集成测试使用 Spring Boot Test 框架的 @SpringBootTest 注解启动完整的应用上下文，使用 Testcontainers 库管理 PostgreSQL 和 Redis 的容器化实例，实现数据库和缓存的自动化启停。

性能测试环境与功能测试环境共享后端服务，前端通过 HTTP 客户端对执行接口进行压力测试，使用 Apache JMeter 作为负载生成工具，PostgreSQL 的查询分析工具用于监控数据库层面的性能指标。

## 6.2 功能测试

### 6.2.1 单元测试

单元测试聚焦于核心领域逻辑的正确性验证，主要覆盖以下几个方面。

WorkflowExecutor 的 DAG 环检测能力测试。测试用例构建一个包含环的工作流（A→B→C→A 的连接关系），调用 execute() 方法，预期该方法抛出 WorkflowValidationException 异常，异常消息包含"cycle"关键词。该测试验证了执行引擎对非法工作流的识别和保护能力。

WorkflowExecutor 的拓扑排序正确性测试。测试用例构建一个线性工作流（A→B→C），执行后检查节点的执行顺序是否严格遵循 A→B→C。测试同时构建一个包含并行分支的工作流（START→A，A 分叉为 B 和 C，B 和 C 汇合到 END），验证拓扑排序的结果中 A 出现在 B 和 C 之前，END 出现在 B 和 C 之后。

NodeExecutor 对 DATA_PROCESSING 节点的处理能力测试。测试用例配置一个 transform 表达式为 `input.amount * 0.9` 的 DATA_PROCESSING 节点，传入包含 {amount: 100} 的输入上下文，验证输出结果中 result 字段的值是否为 90.0。

NodeExecutor 对 CONDITION 节点的条件路由能力测试。测试用例配置一个包含两条规则（amount > 1000 → nodeB；amount > 500 → nodeC；default → nodeD）的 CONDITION 节点，分别以 {amount: 1500}、{amount: 800} 和 {amount: 300} 三个不同的上下文数据执行，验证三次执行的下一跳是否分别为 nodeB、nodeC 和 nodeD。

ContextManager 的 inputMapping 取值能力测试。测试用例配置 inputMapping 为 {totalAmount: amount, taxRate: rate}，传入包含 {amount: 100, rate: 0.13} 的上下文，验证 resolveInput() 返回的输入 Map 是否精确包含 {totalAmount: 100, taxRate: 0.13} 两个字段。

ContextManager 的 outputMapping 写回能力测试。测试用例配置 outputMapping 为 {discounted: total}，节点输出为 {discounted: 90}，调用 applyOutputMapping() 后验证返回的新 Context 中是否包含 {total: 90}。

RuleEvaluator 的 SpEL 表达式求值能力测试。测试用例覆盖多种表达式场景：算术运算（input.a + input.b * input.c）、比较运算（input.x > input.y）、三元表达式（input.flag ? input.val1 : input.val2）、方法调用（input.name.toUpperCase()）。每种场景验证求值结果的正确性和类型。

### 6.2.2 集成测试

集成测试在完整的应用上下文中验证系统各组件之间的协作正确性。

完整工作流执行集成测试使用 H2 内存数据库替代 PostgreSQL，使用嵌入式 Redis 替代真实 Redis 实例，构建一个包含 START、两个 DATA_PROCESSING 节点、一个 CONDITION 节点、一个 PARALLEL+JOIN 并行分支和一个 END 节点的完整工作流。测试以包含 {amount: 1500, taxRate: 0.13, flag: true} 的初始上下文触发执行，验证最终上下文中各节点的输出是否正确累积，节点执行记录的条数和顺序是否符合预期。

并行分支执行集成测试构建一个 PARALLEL+JOIN 结构的工作流，两个并行分支分别执行耗时操作（如 Thread.sleep 模拟），验证总执行时间是否接近于最长分支的耗时而非所有分支耗时之和（证明并行执行确实发生而非串行）。

执行失败场景集成测试在工作流的中间节点配置一个必定失败的 DATA_PROCESSING 节点（transform 表达式引用不存在的字段），触发执行后验证：工作流执行状态为 FAILED、失败节点的 status 为 FAILED 且 error 字段包含有意义的消息、失败节点之前已执行的节点记录存在且 status 为 SUCCESS、失败节点之后的节点均未被执行。

## 6.3 性能测试

性能测试旨在验证系统是否满足非功能需求中定义的性能指标。测试采用控制变量法，每次只改变一个参数，其余参数保持固定。

节点数量与执行耗时的关系测试。构建包含不同节点数量的线性工作流（10、50、100、200 个 DATA_PROCESSING 节点，节点逻辑为空操作），每个配置执行 100 次取平均耗时。测试结果应当表明，执行耗时与节点数量呈近似线性关系，单节点平均框架开销（不包括节点自身逻辑）应当小于 1 毫秒。

并行分支性能测试。构建不同并行度的工作流（2、4、8 个并行分支，每个分支包含 5 个串行节点），测量总执行时间。理想情况下，8 分支并行执行的总耗时应当接近单分支耗时的 1/8 而非 8 倍。若并行开销（线程池调度、同步等）过大，则可能看不到线性加速比。

Context 缓存命中率测试。在执行同一工作流的多次连续执行中，测量 Redis 缓存命中和未命中两种情况下的 Context 读取耗时，验证缓存是否有效降低了数据访问延迟。

## 6.4 测试结果分析

综合单元测试和集成测试的结果，PowerFlow 系统在功能层面达到了预期的设计目标。所有环检测用例均能正确识别并拒绝含环工作流；拓扑排序结果的顺序与 DAG 的依赖关系完全一致；DATA_PROCESSING 节点的表达式计算结果与 SpEL 规范定义的行为完全一致；CONDITION 节点的条件路由逻辑能够准确匹配第一个满足的表达式；并行分支的汇合机制能够正确处理成功和失败两种分支结局；AOP 日志切面在所有测试场景中均完整记录了节点执行的前后状态，未出现日志遗漏。

性能测试结果表明，在不包含节点自身业务逻辑的前提下，单节点执行框架开销约为 0.5-0.8 毫秒，主要消耗在 AOP 切面的方法拦截和日志持久化操作上。100 节点线性工作流的端到端执行时间约为 150-200 毫秒，满足非功能需求中"100 节点规模端到端延迟小于 500 毫秒"的设计目标。8 分支并行工作流的总耗时约为单分支串行耗时的 1.7 倍（而非理论的 1/8），说明并行分支之间存在约 30% 的同步汇合开销，这一比例在可接受范围内。
