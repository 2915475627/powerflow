# 节点扩展实现计划

> **创建时间**: 2026-03-31
> **目标**: 扩展节点类型，支持 HTTP、LLM、并行、循环等高级功能

---

## 概览

### 新增节点类型

| 节点类型 | 优先级 | 工作量 | 说明 |
|---------|--------|--------|------|
| `HTTP_REQUEST` | P0 | M | HTTP 请求节点 |
| `LLM_CALL` | P0 | M | AI 模型调用节点 |
| `PARALLEL` | P1 | H | 并行执行节点 |
| `FOREACH` | P1 | M | 循环迭代节点 |
| `BRANCH` | P2 | L | 优化版条件分支 |
| `SUBWORKFLOW` | P2 | H | 子工作流调用 |
| `TRY_CATCH` | P3 | M | 异常捕获节点 |
| `RETRY` | P3 | L | 重试机制节点 |

---

## Task 1: HTTP_REQUEST 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/service/handler/HttpRequestHandler.java` (新增)
- `src/main/java/com/powerflow/workflow/domain/port/outbound/HttpClientPort.java` (新增)
- `src/main/java/com/powerflow/workflow/adapter/outbound/http/RestTemplateHttpClientAdapter.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/HttpRequestHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 HTTP_REQUEST 到 NodeType 枚举**

```java
public enum NodeType {
    DATA_PROCESSING,
    CONDITION,
    HTTP_REQUEST  // 新增
}
```

**Step 2: 创建 HttpClientPort 接口**

```java
public interface HttpClientPort {
    HttpResponse request(HttpRequest request);
}

public class HttpRequest {
    private String url;
    private String method;  // GET, POST, PUT, DELETE
    private Map<String, String> headers;
    private Object body;
    private int timeout;
}

public class HttpResponse {
    private int statusCode;
    private Map<String, String> headers;
    private String body;
    private long durationMs;
}
```

**Step 3: 创建 HttpRequestHandler**

```java
@Component
public class HttpRequestHandler implements NodeHandler {

    private final HttpClientPort httpClient;

    @Override
    public NodeResult execute(Node node, Context context) {
        HttpRequestConfig config = parseConfig(node.getConfig());

        HttpRequest request = HttpRequest.builder()
            .url(resolveExpression(config.getUrl(), context))
            .method(config.getMethod())
            .headers(resolveHeaders(config.getHeaders(), context))
            .body(resolveBody(config.getBody(), context))
            .timeout(config.getTimeout())
            .build();

        long startTime = System.currentTimeMillis();
        HttpResponse response = httpClient.request(request);
        long duration = System.currentTimeMillis() - startTime;

        return buildResult(node, response, duration);
    }
}
```

**Step 4: 创建 RestTemplate 适配器**

```java
@Component
public class RestTemplateHttpClientAdapter implements HttpClientPort {

    private final RestTemplate restTemplate;

    @Override
    public HttpResponse request(HttpRequest request) {
        HttpHeaders headers = new HttpHeaders();
        request.getHeaders().forEach(headers::add);

        HttpEntity<Object> entity = new HttpEntity<>(request.getBody(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
            request.getUrl(),
            HttpMethod.valueOf(request.getMethod().toUpperCase()),
            entity,
            String.class
        );

        return HttpResponse.builder()
            .statusCode(response.getStatusCode().value())
            .headers(response.getHeaders().toSingleValueMap())
            .body(response.getBody())
            .build();
    }
}
```

**Step 5: 更新 NodeExecutorService**

```java
@Service
public class NodeExecutorService implements NodeExecutorPort {

    private final Map<NodeType, NodeHandler> handlers;

    public NodeExecutorService(HttpRequestHandler httpHandler) {
        this.handlers = Map.of(
            NodeType.DATA_PROCESSING, this::executeDataProcessing,
            NodeType.CONDITION, this::executeCondition,
            NodeType.HTTP_REQUEST, httpHandler::execute  // 新增
        );
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        NodeHandler handler = handlers.get(node.getType());
        return handler.execute(node, context);
    }
}
```

**Step 6: 创建前端组件**

```
frontend/src/components/nodes/HttpRequestNode.tsx
```

**Step 7: 测试验证**

```bash
# 创建带 HTTP 请求的工作流
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "http-wf",
    "name": "HTTP Workflow",
    "startNodeId": "http1",
    "nodes": {
      "http1": {
        "id": "http1",
        "name": "Call API",
        "type": "HTTP_REQUEST",
        "config": {
          "url": "https://api.github.com/users",
          "method": "GET",
          "outputKey": "users"
        }
      }
    },
    "edges": []
  }'
```

---

## Task 2: LLM_CALL 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/service/handler/LlmCallHandler.java` (新增)
- `src/main/java/com/powerflow/workflow/domain/port/outbound/LlmProviderPort.java` (新增)
- `src/main/java/com/powerflow/workflow/adapter/outbound/llm/OpenAiAdapter.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/LlmCallHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 LLM_CALL 到 NodeType**

```java
public enum NodeType {
    DATA_PROCESSING,
    CONDITION,
    HTTP_REQUEST,
    LLM_CALL  // 新增
}
```

**Step 2: 创建 LlmProviderPort 接口**

```java
public interface LlmProviderPort {
    LlmResponse generate(LlmRequest request);
}

public class LlmRequest {
    private String provider;  // openai, anthropic, azure
    private String model;
    private String prompt;
    private Map<String, Object> parameters;  // temperature, maxTokens, etc.
    private List<Message> messages;  // for chat models
}

public class LlmResponse {
    private String content;
    private String model;
    private int tokenUsage;
    private Map<String, String> metadata;
}

public class Message {
    private String role;  // system, user, assistant
    private String content;
}
```

**Step 3: 创建 LlmCallHandler**

```java
@Component
public class LlmCallHandler implements NodeHandler {

    private final Map<String, LlmProviderPort> providers;

    @Override
    public NodeResult execute(Node node, Context context) {
        LlmCallConfig config = parseConfig(node.getConfig());

        LlmRequest request = LlmRequest.builder()
            .provider(config.getProvider())
            .model(config.getModel())
            .prompt(resolveTemplate(config.getPrompt(), context))
            .parameters(config.getParameters())
            .build();

        LlmResponse response = providers.get(config.getProvider()).generate(request);

        Map<String, Object> output = new HashMap<>();
        output.put("content", response.getContent());
        output.put("tokenUsage", response.getTokenUsage());

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(config.getNextNodeId())
            .build();
    }
}
```

**Step 4: 创建 OpenAI 适配器**

```java
@Component
public class OpenAiAdapter implements LlmProviderPort {

    @Override
    public LlmResponse generate(LlmRequest request) {
        // 调用 OpenAI API
        // 返回 LlmResponse
    }
}
```

**Step 5: 前端组件**

```
frontend/src/components/nodes/LlmCallNode.tsx
```

---

## Task 3: PARALLEL 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/model/ParallelBranch.java` (新增)
- `src/main/java/com/powerflow/workflow/domain/service/handler/ParallelHandler.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/ParallelHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 PARALLEL 到 NodeType**

```java
public enum NodeType {
    DATA_PROCESSING,
    CONDITION,
    HTTP_REQUEST,
    LLM_CALL,
    PARALLEL  // 新增
}
```

**Step 2: 创建 ParallelBranch 模型**

```java
public class ParallelBranch {
    private final String name;
    private final List<String> nodeIds;

    // getters, builder
}
```

**Step 3: 创建 ParallelHandler**

```java
@Component
public class ParallelHandler implements NodeHandler {

    private final NodeExecutorService nodeExecutor;

    @Override
    public NodeResult execute(Node node, Context context) {
        ParallelConfig config = parseConfig(node.getConfig());
        String strategy = config.getStrategy();  // AND or OR

        List<CompletableFuture<NodeResult>> futures = config.getBranches().stream()
            .map(branch -> CompletableFuture.supplyAsync(
                () -> executeBranch(branch, context)
            ))
            .toList();

        List<NodeResult> results;
        if ("OR".equals(strategy)) {
            // Wait for first completion
            results = List.of(futures.get(0).get());
        } else {
            // Wait for all
            results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        }

        return aggregateResults(node, results);
    }

    private NodeResult executeBranch(ParallelBranch branch, Context context) {
        // 按顺序执行分支内的节点
        Context branchCtx = new Context(context.toMap());
        NodeResult lastResult = null;
        for (String nodeId : branch.getNodeIds()) {
            Node subNode = findNode(nodeId);
            lastResult = nodeExecutor.execute(subNode, branchCtx);
            if (!lastResult.isSuccess()) break;
        }
        return lastResult;
    }
}
```

**Step 4: 前端组件**

```
frontend/src/components/nodes/ParallelNode.tsx
```

---

## Task 4: FOREACH 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/service/handler/ForeachHandler.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/ForeachHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 FOREACH 到 NodeType**

```java
public enum NodeType {
    // ...
    FOREACH  // 新增
}
```

**Step 2: 创建 ForeachHandler**

```java
@Component
public class ForeachHandler implements NodeHandler {

    private final NodeExecutorService nodeExecutor;

    @Override
    public NodeResult execute(Node node, Context context) {
        ForeachConfig config = parseConfig(node.getConfig());

        Object collectionObj = evaluate(config.getCollection(), context);
        if (!(collectionObj instanceof Collection)) {
            throw new IllegalArgumentException("Collection must be a Collection");
        }

        Collection<?> items = (Collection<?>) collectionObj;
        List<Object> results = new ArrayList<>();
        int index = 0;

        for (Object item : items) {
            if (index >= config.getMaxIterations()) break;

            Context iterationCtx = new Context(context.toMap());
            iterationCtx.set(config.getVariableName(), item);
            iterationCtx.set("loopIndex", index);

            NodeResult result = executeSubgraph(config.getSubgraph(), iterationCtx);
            results.add(result.getOutput());

            if (!result.isSuccess() && config.isFailOnError()) break;
            index++;
        }

        Map<String, Object> output = new HashMap<>();
        output.put("results", results);
        output.put("totalIterations", index);

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(config.getNextNodeId())
            .build();
    }
}
```

**Step 3: 前端组件**

```
frontend/src/components/nodes/ForeachNode.tsx
```

---

## Task 5: BRANCH 节点 (优化版条件)

### Files to Modify
- `src/main/java/com/powerflow/workflow/domain/service/NodeExecutorService.java`

### Implementation

**Step 1: 添加 BRANCH 到 NodeType**

```java
public enum NodeType {
    // ...
    BRANCH  // 替代 CONDITION
}
```

**Step 2: 创建 BranchConfig**

```java
public class BranchConfig {
    private String name;
    private String expression;
    private String nextNodeId;
}

public class BranchResult {
    private String matchedBranch;
    private String nextNodeId;
}
```

**Step 3: 更新 executeCondition 为 executeBranch**

```java
private NodeResult executeBranch(Node node, Context context) {
    @SuppressWarnings("unchecked")
    List<BranchConfig> branches = (List<BranchConfig>) node.getConfig().get("branches");
    String defaultBranch = (String) node.getConfig().get("defaultBranch");

    EvaluationContext evalContext = new StandardEvaluationContext();
    evalContext.setVariable("input", context.toMap());

    for (BranchConfig branch : branches) {
        String expression = transformMapAccess(branch.getExpression());
        Boolean matches = parser.parseExpression(expression)
            .getValue(evalContext, Boolean.class);

        if (Boolean.TRUE.equals(matches)) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(Map.of("matchedBranch", branch.getName()))
                .nextNodeId(branch.getNextNodeId())
                .build();
        }
    }

    return NodeResult.builder()
        .nodeId(node.getId())
        .status(ExecutionStatus.SUCCESS)
        .output(Map.of("matchedBranch", "default"))
        .nextNodeId(defaultBranch)
        .build();
}
```

---

## Task 6: SUBWORKFLOW 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/service/handler/SubworkflowHandler.java` (新增)
- `src/main/java/com/powerflow/workflow/domain/port/outbound/SubworkflowExecutorPort.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/SubworkflowHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 SUBWORKFLOW 到 NodeType**

```java
public enum NodeType {
    // ...
    SUBWORKFLOW  // 新增
}
```

**Step 2: 创建 SubworkflowExecutorPort**

```java
public interface SubworkflowExecutorPort {
    WorkflowExecutionResult execute(String workflowId, Context inputContext);
}
```

**Step 3: 创建 SubworkflowHandler**

```java
@Component
public class SubworkflowHandler implements NodeHandler {

    private final SubworkflowExecutorPort executor;

    @Override
    public NodeResult execute(Node node, Context context) {
        SubworkflowConfig config = parseConfig(node.getConfig());

        WorkflowExecutionResult result = executor.execute(
            config.getWorkflowId(),
            context
        );

        Map<String, Object> output = new HashMap<>();
        output.put("status", result.getStatus());
        output.put("result", result.getFinalContext().toMap());

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(result.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
            .output(output)
            .nextNodeId(config.getNextNodeId())
            .error(result.getError().orElse(null))
            .build();
    }
}
```

**Step 4: 前端组件**

```
frontend/src/components/nodes/SubworkflowNode.tsx
```

---

## Task 7: TRY_CATCH 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/service/handler/TryCatchHandler.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/TryCatchHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 TRY_CATCH 到 NodeType**

```java
public enum NodeType {
    // ...
    TRY_CATCH  // 新增
}
```

**Step 2: 创建 TryCatchHandler**

```java
@Component
public class TryCatchHandler implements NodeHandler {

    private final NodeExecutorService nodeExecutor;

    @Override
    public NodeResult execute(Node node, Context context) {
        TryCatchConfig config = parseConfig(node.getConfig());

        // Execute try nodes
        NodeResult tryResult = executeNodes(config.getTryNodes(), context);
        if (tryResult.isSuccess()) {
            return tryResult;
        }

        // Execute catch nodes
        Context errorCtx = new Context(context.toMap());
        errorCtx.set("error", tryResult.getError());

        return executeNodes(config.getCatchNodes(), errorCtx);
    }

    private NodeResult executeNodes(List<String> nodeIds, Context ctx) {
        Context currentCtx = ctx;
        NodeResult lastResult = null;
        for (String nodeId : nodeIds) {
            Node n = findNode(nodeId);
            lastResult = nodeExecutor.execute(n, currentCtx);
            if (!lastResult.isSuccess()) return lastResult;
            currentCtx = mergeContext(currentCtx, lastResult.getOutput());
        }
        return lastResult;
    }
}
```

---

## Task 8: RETRY 节点

### Files to Create
- `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` (修改)
- `src/main/java/com/powerflow/workflow/domain/service/handler/RetryHandler.java` (新增)
- `src/test/java/com/powerflow/workflow/domain/service/handler/RetryHandlerTest.java` (新增)

### Implementation

**Step 1: 添加 RETRY 到 NodeType**

```java
public enum NodeType {
    // ...
    RETRY  // 新增
}
```

**Step 2: 创建 RetryConfig**

```java
public class RetryConfig {
    private int maxAttempts;
    private long initialDelayMs;
    private long maxDelayMs;
    private String backoffStrategy;  // FIXED, EXPONENTIAL, FIBONACCI
    private List<String> targetNodeIds;
}
```

**Step 3: 创建 RetryHandler**

```java
@Component
public class RetryHandler implements NodeHandler {

    private final NodeExecutorService nodeExecutor;

    @Override
    public NodeResult execute(Node node, Context context) {
        RetryConfig config = parseConfig(node.getConfig());

        int attempt = 0;
        long delay = config.getInitialDelayMs();

        while (attempt < config.getMaxAttempts()) {
            attempt++;
            NodeResult result = executeTargetNodes(config.getTargetNodeIds(), context);

            if (result.isSuccess()) {
                return result;
            }

            if (attempt < config.getMaxAttempts()) {
                sleep(delay);
                delay = calculateNextDelay(delay, config);
            }
        }

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.FAILED)
            .error("Max retry attempts exceeded: " + config.getMaxAttempts())
            .build();
    }

    private long calculateNextDelay(long currentDelay, RetryConfig config) {
        return switch (config.getBackoffStrategy()) {
            case "EXPONENTIAL" -> Math.min(currentDelay * 2, config.getMaxDelayMs());
            case "FIBONACCI" -> currentDelay + previousDelay;
            default -> currentDelay;
        };
    }
}
```

---

## 前端任务清单

### 新增节点组件

| 组件 | 依赖 | 说明 |
|------|------|------|
| `HttpRequestNode.tsx` | Task 1 | HTTP 请求配置面板 |
| `LlmCallNode.tsx` | Task 2 | LLM 调用配置面板 |
| `ParallelNode.tsx` | Task 3 | 并行分支视图 |
| `ForeachNode.tsx` | Task 4 | 循环配置面板 |
| `BranchNode.tsx` | Task 5 | 优化分支配置 |
| `SubworkflowNode.tsx` | Task 6 | 子工作流选择 |
| `TryCatchNode.tsx` | Task 7 | 异常处理配置 |
| `RetryNode.tsx` | Task 8 | 重试策略配置 |

### 修改现有文件

- `frontend/src/types/workflow.ts` - 添加新节点类型
- `frontend/src/pages/WorkflowEditorPage.tsx` - 注册新节点类型
- `frontend/src/api/workflow.ts` - 无需修改（API 通用）

---

## 执行顺序

```
1. Task 1: HTTP_REQUEST 节点 (P0)
2. Task 2: LLM_CALL 节点 (P0)
3. Task 5: BRANCH 节点优化 (P2) - 可提前，与条件节点相似
4. Task 3: PARALLEL 节点 (P1)
5. Task 4: FOREACH 节点 (P1)
6. Task 6: SUBWORKFLOW 节点 (P2)
7. Task 7: TRY_CATCH 节点 (P3)
8. Task 8: RETRY 节点 (P3)
```

---

## 自检清单

### Task 1 (HTTP_REQUEST)
- [ ] NodeType 枚举添加 HTTP_REQUEST
- [ ] HttpClientPort 接口
- [ ] HttpRequestHandler
- [ ] RestTemplateHttpClientAdapter
- [ ] 单元测试
- [ ] 前端 HttpRequestNode 组件

### Task 2 (LLM_CALL)
- [ ] NodeType 枚举添加 LLM_CALL
- [ ] LlmProviderPort 接口
- [ ] LlmCallHandler
- [ ] OpenAiAdapter (或其他 provider)
- [ ] 单元测试
- [ ] 前端 LlmCallNode 组件

### Task 3 (PARALLEL)
- [ ] NodeType 枚举添加 PARALLEL
- [ ] ParallelBranch 模型
- [ ] ParallelHandler (ForkJoinPool)
- [ ] 单元测试
- [ ] 前端 ParallelNode 组件

### Task 4 (FOREACH)
- [ ] NodeType 枚举添加 FOREACH
- [ ] ForeachHandler
- [ ] 单元测试
- [ ] 前端 ForeachNode 组件

### Task 5 (BRANCH)
- [ ] NodeType 枚举添加/替换 BRANCH
- [ ] BranchConfig
- [ ] executeBranch 方法
- [ ] 单元测试

### Task 6 (SUBWORKFLOW)
- [ ] NodeType 枚举添加 SUBWORKFLOW
- [ ] SubworkflowExecutorPort
- [ ] SubworkflowHandler
- [ ] 单元测试
- [ ] 前端 SubworkflowNode 组件

### Task 7 (TRY_CATCH)
- [ ] NodeType 枚举添加 TRY_CATCH
- [ ] TryCatchHandler
- [ ] 单元测试
- [ ] 前端 TryCatchNode 组件

### Task 8 (RETRY)
- [ ] NodeType 枚举添加 RETRY
- [ ] RetryConfig
- [ ] RetryHandler
- [ ] 单元测试
- [ ] 前端 RetryNode 组件
