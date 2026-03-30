# Workflow Core Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建工作流系统核心域——节点执行引擎、DAG 执行、AOP 日志记录。完全不依赖外部基础设施（无 DB、无 Redis），纯内存实现。

**Architecture:** 六边形架构，Domain 层为核心，Ports 为接口，Adapters 在后续子项目实现。Spring Boot 管理 Bean，SpEL 做表达式求值。

**Tech Stack:** Java 17, Maven, Spring Boot 3.x, JUnit 5, AssertJ, Mockito, SpEL

---

## 文件结构

```
powerflow/
├── pom.xml
└── src/
    ├── main/java/com/powerflow/workflow/
    │   ├── PowerflowApplication.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Workflow.java
    │   │   │   ├── Node.java
    │   │   │   ├── Edge.java
    │   │   │   ├── Context.java
    │   │   │   ├── NodeExecution.java
    │   │   │   ├── WorkflowExecutionResult.java
    │   │   │   ├── NodeResult.java
    │   │   │   └── enums/
    │   │   │       ├── NodeType.java
    │   │   │       └── ExecutionStatus.java
    │   │   ├── service/
    │   │   │   ├── WorkflowExecutor.java
    │   │   │   ├── NodeExecutor.java
    │   │   │   ├── ContextManager.java
    │   │   │   └── RuleEvaluator.java
    │   │   └── port/
    │   │       ├── inbound/
    │   │       │   └── WorkflowUseCase.java
    │   │       └── outbound/
    │   │           ├── WorkflowRepository.java
    │   │           ├── ExecutionLogRepository.java
    │   │           └── NodeExecutorPort.java
    │   ├── adapter/
    │   │   ├── inbound/
    │   │   │   └── rest/
    │   │   │       └── WorkflowController.java
    │   │   └── outbound/
    │   │       ├── persistence/
    │   │       │   └── InMemoryWorkflowRepository.java
    │   │       └── logging/
    │   │           └── InMemoryExecutionLogRepository.java
    │   ├── exception/
    │   │   ├── WorkflowExecutionException.java
    │   │   ├── NodeExecutionException.java
    │   │   └── WorkflowNotFoundException.java
    │   └── aop/
    │       └── NodeExecutionAspect.java
    └── test/java/com/powerflow/workflow/
        ├── domain/
        │   ├── service/
        │   │   ├── WorkflowExecutorTest.java
        │   │   ├── NodeExecutorTest.java
        │   │   ├── ContextManagerTest.java
        │   │   └── RuleEvaluatorTest.java
        │   └── model/
        │       └── WorkflowTest.java
        └── integration/
            └── WorkflowExecutionIntegrationTest.java
```

---

## Task 1: Maven 项目初始化

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/powerflow/workflow/PowerflowApplication.java`
- Create: `src/main/resources/application.properties`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>

    <groupId>com.powerflow</groupId>
    <artifactId>workflow</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 Spring Boot 启动类**

```java
package com.powerflow.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PowerflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PowerflowApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.properties**

```properties
spring.application.name=powerflow
```

- [ ] **Step 4: 验证项目编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add pom.xml src/main/java/com/powerflow/workflow/PowerflowApplication.java src/main/resources/application.properties
git commit -m "feat: init Maven project with Spring Boot 3.2"
```

---

## Task 2: 域模型 - 枚举和简单对象

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java`
- Create: `src/main/java/com/powerflow/workflow/domain/model/enums/ExecutionStatus.java`
- Create: `src/main/java/com/powerflow/workflow/domain/model/Context.java`
- Create: `src/test/java/com/powerflow/workflow/domain/model/ContextTest.java`

- [ ] **Step 1: 创建 NodeType 枚举**

```java
package com.powerflow.workflow.domain.model.enums;

public enum NodeType {
    DATA_PROCESSING,
    CONDITION
}
```

- [ ] **Step 2: 创建 ExecutionStatus 枚举**

```java
package com.powerflow.workflow.domain.model.enums;

public enum ExecutionStatus {
    SUCCESS,
    FAILED
}
```

- [ ] **Step 3: 创建 Context 模型（写测试先行）**

```java
package com.powerflow.workflow.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Context {
    private final Map<String, Object> data;

    public Context() {
        this.data = new HashMap<>();
    }

    public Context(Map<String, Object> initialData) {
        this.data = new HashMap<>(initialData);
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }
}
```

- [ ] **Step 4: 写 Context 测试（验证 KV 读写）**

```java
package com.powerflow.workflow.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContextTest {

    @Test
    void should_store_and_retrieve_values() {
        Context ctx = new Context();
        ctx.set("amount", 1000);
        ctx.set("userId", "user-123");

        assertThat(ctx.get("amount")).containsValue(1000);
        assertThat(ctx.get("userId")).containsValue("user-123");
    }

    @Test
    void should_return_empty_for_missing_key() {
        Context ctx = new Context();
        assertThat(ctx.get("missing")).isEmpty();
    }

    @Test
    void should_copy_on_construct() {
        Map<String, Object> initial = Map.of("key", "value");
        Context ctx = new Context(initial);
        assertThat(ctx.get("key")).containsValue("value");
    }
}
```

- [ ] **Step 5: 运行测试验证**

Run: `mvn test -Dtest=ContextTest`
Expected: 3 tests PASS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java
git add src/main/java/com/powerflow/workflow/domain/model/enums/ExecutionStatus.java
git add src/main/java/com/powerflow/workflow/domain/model/Context.java
git add src/test/java/com/powerflow/workflow/domain/model/ContextTest.java
git commit -m "feat: add Context model and enums"
```

---

## Task 3: 域模型 - Node 和 Edge

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/model/Node.java`
- Create: `src/main/java/com/powerflow/workflow/domain/model/Edge.java`
- Create: `src/test/java/com/powerflow/workflow/domain/model/NodeTest.java`
- Create: `src/test/java/com/powerflow/workflow/domain/model/EdgeTest.java`

- [ ] **Step 1: 创建 Node 模型**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.NodeType;
import java.util.HashMap;
import java.util.Map;

public class Node {
    private final String id;
    private final String name;
    private final NodeType type;
    private final Map<String, Object> config;
    private final Map<String, String> inputMapping;
    private final Map<String, String> outputMapping;

    public Node(String id, String name, NodeType type,
                Map<String, Object> config,
                Map<String, String> inputMapping,
                Map<String, String> outputMapping) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.config = new HashMap<>(config);
        this.inputMapping = new HashMap<>(inputMapping);
        this.outputMapping = new HashMap<>(outputMapping);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public NodeType getType() { return type; }
    public Map<String, Object> getConfig() { return new HashMap<>(config); }
    public Map<String, String> getInputMapping() { return new HashMap<>(inputMapping); }
    public Map<String, String> getOutputMapping() { return new HashMap<>(outputMapping); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private NodeType type;
        private Map<String, Object> config = new HashMap<>();
        private Map<String, String> inputMapping = new HashMap<>();
        private Map<String, String> outputMapping = new HashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(NodeType type) { this.type = type; return this; }
        public Builder config(Map<String, Object> config) { this.config = config; return this; }
        public Builder inputMapping(Map<String, String> inputMapping) { this.inputMapping = inputMapping; return this; }
        public Builder outputMapping(Map<String, String> outputMapping) { this.outputMapping = outputMapping; return this; }
        public Node build() { return new Node(id, name, type, config, inputMapping, outputMapping); }
    }
}
```

- [ ] **Step 2: 创建 Edge 模型**

```java
package com.powerflow.workflow.domain.model;

public class Edge {
    private final String id;
    private final String fromNodeId;
    private final String toNodeId;
    private final String condition;

    public Edge(String id, String fromNodeId, String toNodeId, String condition) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.condition = condition;
    }

    public String getId() { return id; }
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public boolean hasCondition() { return condition != null && !condition.isBlank(); }
    public String getCondition() { return condition; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String fromNodeId;
        private String toNodeId;
        private String condition;

        public Builder id(String id) { this.id = id; return this; }
        public Builder fromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; return this; }
        public Builder toNodeId(String toNodeId) { this.toNodeId = toNodeId; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }
        public Edge build() { return new Edge(id, fromNodeId, toNodeId, condition); }
    }
}
```

- [ ] **Step 3: 写 Node 测试**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class NodeTest {

    @Test
    void should_build_node_with_all_fields() {
        Node node = Node.builder()
            .id("node-1")
            .name("Calculate Discount")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("transform", "input.amount * 0.9", "outputKey", "discountedAmount"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("discountedAmount", "output.discountedAmount"))
            .build();

        assertThat(node.getId()).isEqualTo("node-1");
        assertThat(node.getName()).isEqualTo("Calculate Discount");
        assertThat(node.getType()).isEqualTo(NodeType.DATA_PROCESSING);
        assertThat(node.getConfig()).containsEntry("transform", "input.amount * 0.9");
        assertThat(node.getInputMapping()).containsEntry("amount", "input.amount");
        assertThat(node.getOutputMapping()).containsEntry("discountedAmount", "output.discountedAmount");
    }

    @Test
    void should_return_defensive_copies() {
        Node node = Node.builder()
            .id("node-1")
            .type(NodeType.DATA_PROCESSING)
            .build();

        node.getConfig().put("key", "value");
        node.getInputMapping().put("key", "value");
        node.getOutputMapping().put("key", "value");

        assertThat(node.getConfig()).isEmpty();
        assertThat(node.getInputMapping()).isEmpty();
        assertThat(node.getOutputMapping()).isEmpty();
    }
}
```

- [ ] **Step 4: 写 Edge 测试**

```java
package com.powerflow.workflow.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EdgeTest {

    @Test
    void should_build_edge_without_condition() {
        Edge edge = Edge.builder()
            .id("edge-1")
            .fromNodeId("node-1")
            .toNodeId("node-2")
            .build();

        assertThat(edge.getId()).isEqualTo("edge-1");
        assertThat(edge.hasCondition()).isFalse();
    }

    @Test
    void should_build_edge_with_condition() {
        Edge edge = Edge.builder()
            .id("edge-2")
            .fromNodeId("node-1")
            .toNodeId("node-3")
            .condition("input.amount > 1000")
            .build();

        assertThat(edge.hasCondition()).isTrue();
        assertThat(edge.getCondition()).isEqualTo("input.amount > 1000");
    }
}
```

- [ ] **Step 5: 运行测试验证**

Run: `mvn test -Dtest=NodeTest,EdgeTest`
Expected: 4 tests PASS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/model/Node.java
git add src/main/java/com/powerflow/workflow/domain/model/Edge.java
git add src/test/java/com/powerflow/workflow/domain/model/NodeTest.java
git add src/test/java/com/powerflow/workflow/domain/model/EdgeTest.java
git commit -m "feat: add Node and Edge models"
```

---

## Task 4: 域模型 - NodeExecution 和 Workflow

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/model/NodeExecution.java`
- Create: `src/main/java/com/powerflow/workflow/domain/model/Workflow.java`
- Create: `src/test/java/com/powerflow/workflow/domain/model/WorkflowTest.java`
- Create: `src/test/java/com/powerflow/workflow/domain/model/NodeExecutionTest.java`

- [ ] **Step 1: 创建 NodeExecution 模型**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NodeExecution {
    private final String id;
    private final String workflowExecutionId;
    private final String nodeId;
    private final ExecutionStatus status;
    private final Map<String, Object> input;
    private final Map<String, Object> output;
    private final String error;
    private final long durationMs;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private NodeExecution(String id, String workflowExecutionId, String nodeId,
                          ExecutionStatus status, Map<String, Object> input,
                          Map<String, Object> output, String error,
                          long durationMs, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.workflowExecutionId = workflowExecutionId;
        this.nodeId = nodeId;
        this.status = status;
        this.input = input;
        this.output = output;
        this.error = error;
        this.durationMs = durationMs;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public String getWorkflowExecutionId() { return workflowExecutionId; }
    public String getNodeId() { return nodeId; }
    public ExecutionStatus getStatus() { return status; }
    public Map<String, Object> getInput() { return input; }
    public Map<String, Object> getOutput() { return output; }
    public Optional<String> getError() { return Optional.ofNullable(error); }
    public long getDurationMs() { return durationMs; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String workflowExecutionId;
        private String nodeId;
        private ExecutionStatus status;
        private Map<String, Object> input;
        private Map<String, Object> output;
        private String error;
        private long durationMs;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public Builder id(String id) { this.id = id; return this; }
        public Builder workflowExecutionId(String workflowExecutionId) { this.workflowExecutionId = workflowExecutionId; return this; }
        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder input(Map<String, Object> input) { this.input = input; return this; }
        public Builder output(Map<String, Object> output) { this.output = output; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public NodeExecution build() { return new NodeExecution(id, workflowExecutionId, nodeId, status, input, output, error, durationMs, startTime, endTime); }
    }
}
```

- [ ] **Step 2: 创建 Workflow 模型**

```java
package com.powerflow.workflow.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Workflow {
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Node> nodes;
    private final List<Edge> edges;
    private final String startNodeId;

    public Workflow(String id, String name, String description,
                    List<Node> nodes, List<Edge> edges, String startNodeId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = new HashMap<>();
        for (Node node : nodes) {
            this.nodes.put(node.getId(), node);
        }
        this.edges = new ArrayList<>(edges);
        this.startNodeId = startNodeId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, Node> getNodes() { return new HashMap<>(nodes); }
    public List<Edge> getEdges() { return new ArrayList<>(edges); }
    public String getStartNodeId() { return startNodeId; }

    public Optional<Node> findNodeById(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public List<Edge> findEdgesByFromNodeId(String fromNodeId) {
        return edges.stream()
            .filter(e -> e.getFromNodeId().equals(fromNodeId))
            .toList();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<Node> nodes = new ArrayList<>();
        private List<Edge> edges = new ArrayList<>();
        private String startNodeId;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder nodes(List<Node> nodes) { this.nodes = nodes; return this; }
        public Builder edges(List<Edge> edges) { this.edges = edges; return this; }
        public Builder startNodeId(String startNodeId) { this.startNodeId = startNodeId; return this; }
        public Workflow build() { return new Workflow(id, name, description, nodes, edges, startNodeId); }
    }
}
```

- [ ] **Step 3: 写 Workflow 测试**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTest {

    @Test
    void should_build_workflow_and_retrieve_nodes() {
        Node node1 = Node.builder().id("node-1").name("Start").type(NodeType.DATA_PROCESSING).build();
        Node node2 = Node.builder().id("node-2").name("End").type(NodeType.DATA_PROCESSING).build();
        Edge edge = Edge.builder().id("edge-1").fromNodeId("node-1").toNodeId("node-2").build();

        Workflow workflow = Workflow.builder()
            .id("wf-1")
            .name("Simple Workflow")
            .startNodeId("node-1")
            .nodes(List.of(node1, node2))
            .edges(List.of(edge))
            .build();

        assertThat(workflow.getId()).isEqualTo("wf-1");
        assertThat(workflow.findNodeById("node-1")).isPresent();
        assertThat(workflow.findNodeById("node-2")).isPresent();
        assertThat(workflow.findEdgesByFromNodeId("node-1")).hasSize(1);
    }

    @Test
    void should_return_empty_when_node_not_found() {
        Workflow workflow = Workflow.builder()
            .id("wf-1")
            .startNodeId("node-1")
            .nodes(List.of())
            .edges(List.of())
            .build();

        assertThat(workflow.findNodeById("non-existent")).isEmpty();
    }
}
```

- [ ] **Step 4: 写 NodeExecution 测试**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutionTest {

    @Test
    void should_build_node_execution() {
        LocalDateTime start = LocalDateTime.now();

        NodeExecution execution = NodeExecution.builder()
            .workflowExecutionId("wf-exec-1")
            .nodeId("node-1")
            .status(ExecutionStatus.SUCCESS)
            .input(Map.of("amount", 1000))
            .output(Map.of("discountedAmount", 900))
            .durationMs(50)
            .startTime(start)
            .endTime(start.plusNanos(50_000_000))
            .build();

        assertThat(execution.getWorkflowExecutionId()).isEqualTo("wf-exec-1");
        assertThat(execution.getNodeId()).isEqualTo("node-1");
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(execution.getError()).isEmpty();
        assertThat(execution.getDurationMs()).isEqualTo(50);
    }

    @Test
    void should_capture_error() {
        NodeExecution execution = NodeExecution.builder()
            .nodeId("node-1")
            .status(ExecutionStatus.FAILED)
            .error("Division by zero")
            .build();

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getError()).containsValue("Division by zero");
    }
}
```

- [ ] **Step 5: 运行测试验证**

Run: `mvn test -Dtest=WorkflowTest,NodeExecutionTest`
Expected: 4 tests PASS

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/model/NodeExecution.java
git add src/main/java/com/powerflow/workflow/domain/model/Workflow.java
git add src/test/java/com/powerflow/workflow/domain/model/WorkflowTest.java
git add src/test/java/com/powerflow/workflow/domain/model/NodeExecutionTest.java
git commit -m "feat: add Workflow and NodeExecution models"
```

---

## Task 5: 域模型 - NodeResult 和 WorkflowExecutionResult

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/model/NodeResult.java`
- Create: `src/main/java/com/powerflow/workflow/domain/model/WorkflowExecutionResult.java`

- [ ] **Step 1: 创建 NodeResult 模型**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import java.util.Map;
import java.util.Optional;

public class NodeResult {
    private final String nodeId;
    private final ExecutionStatus status;
    private final Map<String, Object> output;
    private final Optional<String> error;
    private final Optional<String> nextNodeId;

    private NodeResult(String nodeId, ExecutionStatus status, Map<String, Object> output,
                       Optional<String> error, Optional<String> nextNodeId) {
        this.nodeId = nodeId;
        this.status = status;
        this.output = output;
        this.error = error;
        this.nextNodeId = nextNodeId;
    }

    public static Builder builder() { return new Builder(); }

    public String getNodeId() { return nodeId; }
    public ExecutionStatus getStatus() { return status; }
    public Map<String, Object> getOutput() { return output; }
    public Optional<String> getError() { return error; }
    public Optional<String> getNextNodeId() { return nextNodeId; }

    public boolean isSuccess() { return status == ExecutionStatus.SUCCESS; }

    public static class Builder {
        private String nodeId;
        private ExecutionStatus status;
        private Map<String, Object> output;
        private Optional<String> error = Optional.empty();
        private Optional<String> nextNodeId = Optional.empty();

        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder output(Map<String, Object> output) { this.output = output; return this; }
        public Builder error(String error) { this.error = Optional.of(error); return this; }
        public Builder nextNodeId(String nextNodeId) { this.nextNodeId = Optional.of(nextNodeId); return this; }
        public NodeResult build() { return new NodeResult(nodeId, status, output, error, nextNodeId); }
    }
}
```

- [ ] **Step 2: 创建 WorkflowExecutionResult 模型**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import java.util.List;
import java.util.Optional;

public class WorkflowExecutionResult {
    private final String workflowId;
    private final String executionId;
    private final ExecutionStatus status;
    private final Context finalContext;
    private final List<NodeExecution> nodeExecutions;
    private final Optional<String> error;

    private WorkflowExecutionResult(String workflowId, String executionId, ExecutionStatus status,
                                     Context finalContext, List<NodeExecution> nodeExecutions,
                                     Optional<String> error) {
        this.workflowId = workflowId;
        this.executionId = executionId;
        this.status = status;
        this.finalContext = finalContext;
        this.nodeExecutions = nodeExecutions;
        this.error = error;
    }

    public static Builder builder() { return new Builder(); }

    public String getWorkflowId() { return workflowId; }
    public String getExecutionId() { return executionId; }
    public ExecutionStatus getStatus() { return status; }
    public Context getFinalContext() { return finalContext; }
    public List<NodeExecution> getNodeExecutions() { return nodeExecutions; }
    public Optional<String> getError() { return error; }
    public boolean isSuccess() { return status == ExecutionStatus.SUCCESS; }

    public static class Builder {
        private String workflowId;
        private String executionId;
        private ExecutionStatus status;
        private Context finalContext;
        private List<NodeExecutions> nodeExecutions;
        private Optional<String> error = Optional.empty();

        public Builder workflowId(String workflowId) { this.workflowId = workflowId; return this; }
        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder finalContext(Context finalContext) { this.finalContext = finalContext; return this; }
        public Builder nodeExecutions(List<NodeExecution> nodeExecutions) { this.nodeExecutions = nodeExecutions; return this; }
        public Builder error(String error) { this.error = Optional.of(error); return this; }
        public WorkflowExecutionResult build() { return new WorkflowExecutionResult(workflowId, executionId, status, finalContext, nodeExecutions, error); }
    }
}
```

- [ ] **Step 3: 运行编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/model/NodeResult.java
git add src/main/java/com/powerflow/workflow/domain/model/WorkflowExecutionResult.java
git commit -m "feat: add NodeResult and WorkflowExecutionResult models"
```

---

## Task 6: 异常定义

**Files:**
- Create: `src/main/java/com/powerflow/workflow/exception/WorkflowExecutionException.java`
- Create: `src/main/java/com/powerflow/workflow/exception/NodeExecutionException.java`
- Create: `src/main/java/com/powerflow/workflow/exception/WorkflowNotFoundException.java`

- [ ] **Step 1: 创建异常类**

```java
package com.powerflow.workflow.exception;

public class WorkflowExecutionException extends RuntimeException {
    private final String workflowId;
    private final String nodeId;

    public WorkflowExecutionException(String message, String workflowId, String nodeId) {
        super(message);
        this.workflowId = workflowId;
        this.nodeId = nodeId;
    }

    public String getWorkflowId() { return workflowId; }
    public String getNodeId() { return nodeId; }
}
```

```java
package com.powerflow.workflow.exception;

public class NodeExecutionException extends RuntimeException {
    private final String nodeId;

    public NodeExecutionException(String message, String nodeId) {
        super(message);
        this.nodeId = nodeId;
    }

    public String getNodeId() { return nodeId; }
}
```

```java
package com.powerflow.workflow.exception;

public class WorkflowNotFoundException extends RuntimeException {
    private final String workflowId;

    public WorkflowNotFoundException(String workflowId) {
        super("Workflow not found: " + workflowId);
        this.workflowId = workflowId;
    }

    public String getWorkflowId() { return workflowId; }
}
```

- [ ] **Step 2: 运行编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/powerflow/workflow/exception/
git commit -m "feat: add domain exceptions"
```

---

## Task 7: 端口接口定义

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/port/inbound/WorkflowUseCase.java`
- Create: `src/main/java/com/powerflow/workflow/domain/port/outbound/WorkflowRepository.java`
- Create: `src/main/java/com/powerflow/workflow/domain/port/outbound/ExecutionLogRepository.java`
- Create: `src/main/java/com/powerflow/workflow/domain/port/outbound/NodeExecutorPort.java`

- [ ] **Step 1: 创建入站端口 WorkflowUseCase**

```java
package com.powerflow.workflow.domain.port.inbound;

import com.powerflow.workflow.domain.model.*;

public interface WorkflowUseCase {
    WorkflowExecutionResult execute(String workflowId, Context inputContext);
    NodeResult testNode(String workflowId, String nodeId, Context inputContext);
}
```

- [ ] **Step 2: 创建出站端口**

```java
package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.Workflow;
import java.util.Optional;

public interface WorkflowRepository {
    Optional<Workflow> findById(String id);
    Workflow save(Workflow workflow);
    void delete(String id);
}
```

```java
package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.NodeExecution;
import java.util.List;

public interface ExecutionLogRepository {
    void save(NodeExecution execution);
    List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId);
}
```

```java
package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.*;

public interface NodeExecutorPort {
    NodeResult execute(Node node, Context context);
}
```

- [ ] **Step 3: 运行编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/port/
git commit -m "feat: add port interfaces"
```

---

## Task 8: ContextManager 领域服务

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/service/ContextManager.java`
- Create: `src/test/java/com/powerflow/workflow/domain/service/ContextManagerTest.java`

- [ ] **Step 1: 写 ContextManager 测试（TDD RED）**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Context;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ContextManagerTest {

    @Test
    void should_extract_node_input_from_context_by_mapping() {
        Context ctx = new Context(Map.of("amount", 1000, "userId", "user-123"));
        ContextManager manager = new ContextManager();

        Map<String, Object> input = manager.extractNodeInput(
            Map.of("amount", "input.amount", "userId", "input.userId"),
            ctx
        );

        assertThat(input).containsEntry("amount", 1000).containsEntry("userId", "user-123");
    }

    @Test
    void should_write_node_output_to_context_by_mapping() {
        Context ctx = new Context();
        ContextManager manager = new ContextManager();

        manager.writeNodeOutput(
            Map.of("result", "output.discountedAmount"),
            Map.of("result", 900),
            ctx
        );

        assertThat(ctx.get("output.discountedAmount")).containsValue(900);
    }

    @Test
    void should_handle_missing_keys_gracefully() {
        Context ctx = new Context(Map.of("amount", 1000));
        ContextManager manager = new ContextManager();

        Map<String, Object> input = manager.extractNodeInput(
            Map.of("missing", "input.missing"),
            ctx
        );

        assertThat(input).doesNotContainKey("missing");
    }
}
```

- [ ] **Step 2: 运行测试验证 RED**

Run: `mvn test -Dtest=ContextManagerTest`
Expected: FAIL (ContextManager not exists yet)

- [ ] **Step 3: 创建 ContextManager**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Context;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContextManager {

    public Map<String, Object> extractNodeInput(Map<String, String> inputMapping, Context context) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : inputMapping.entrySet()) {
            String contextKey = entry.getValue().replace("input.", "");
            context.get(contextKey).ifPresent(value -> result.put(entry.getKey(), value));
        }
        return result;
    }

    public void writeNodeOutput(Map<String, String> outputMapping,
                                 Map<String, Object> nodeOutput,
                                 Context context) {
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            String outputKey = entry.getKey();
            String contextKey = entry.getValue();
            if (nodeOutput.containsKey(outputKey)) {
                context.set(contextKey, nodeOutput.get(outputKey));
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试验证 GREEN**

Run: `mvn test -Dtest=ContextManagerTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/service/ContextManager.java
git add src/test/java/com/powerflow/workflow/domain/service/ContextManagerTest.java
git commit -m "feat: add ContextManager domain service"
```

---

## Task 9: RuleEvaluator 领域服务

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/service/RuleEvaluator.java`
- Create: `src/test/java/com/powerflow/workflow/domain/service/RuleEvaluatorTest.java`

- [ ] **Step 1: 写 RuleEvaluator 测试（TDD RED）**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Context;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {

    @Test
    void should_evaluate_boolean_expression() {
        RuleEvaluator evaluator = new RuleEvaluator();
        Context ctx = new Context(Map.of("amount", 1500));

        boolean result = evaluator.evaluate("input.amount > 1000", ctx);

        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_when_condition_not_met() {
        RuleEvaluator evaluator = new RuleEvaluator();
        Context ctx = new Context(Map.of("amount", 500));

        boolean result = evaluator.evaluate("input.amount > 1000", ctx);

        assertThat(result).isFalse();
    }

    @Test
    void should_handle_numeric_equals() {
        RuleEvaluator evaluator = new RuleEvaluator();
        Context ctx = new Context(Map.of("status", "active"));

        boolean result = evaluator.evaluate("input.status == 'active'", ctx);

        assertThat(result).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试验证 RED**

Run: `mvn test -Dtest=RuleEvaluatorTest`
Expected: FAIL

- [ ] **Step 3: 创建 RuleEvaluator**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Context;
import org.springframework.stereotype.Service;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Service
public class RuleEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    public boolean evaluate(String expression, Context context) {
        EvaluationContext evalContext = new StandardEvaluationContext();
        Map<String, Object> data = context.toMap();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            ((StandardEvaluationContext) evalContext).setVariable("" + entry.getKey(), entry.getValue());
        }
        String spelExpression = expression.replace("input.", "#");
        return Boolean.TRUE.equals(parser.parseExpression(spelExpression).getValue(evalContext));
    }
}
```

- [ ] **Step 4: 运行测试验证 GREEN**

Run: `mvn test -Dtest=RuleEvaluatorTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/service/RuleEvaluator.java
git add src/test/java/com/powerflow/workflow/domain/service/RuleEvaluatorTest.java
git commit -m "feat: add RuleEvaluator domain service"
```

---

## Task 10: NodeExecutor 领域服务

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/service/NodeExecutorService.java`
- Create: `src/test/java/com/powerflow/workflow/domain/service/NodeExecutorServiceTest.java`

- [ ] **Step 1: 写 NodeExecutor 测试（TDD RED）**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutorServiceTest {

    @Test
    void should_execute_data_processing_node() {
        NodeExecutorService executor = new NodeExecutorService(null);
        Context ctx = new Context(Map.of("amount", 1000));

        Node node = Node.builder()
            .id("node-1")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.amount * 0.9"))
            .build();

        NodeResult result = executor.execute(node, ctx);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.getOutput()).containsEntry("result", 900.0);
    }

    @Test
    void should_execute_condition_node_and_return_next_node() {
        ContextManager contextManager = new ContextManager();
        RuleEvaluator ruleEvaluator = new RuleEvaluator();
        NodeExecutorService executor = new NodeExecutorService(null);

        Context ctx = new Context(Map.of("amount", 1500));

        Node conditionNode = Node.builder()
            .id("condition-1")
            .type(NodeType.CONDITION)
            .config(Map.of(
                "conditions", List.of(
                    Map.of("expression", "#input.amount > 1000", "nextNodeId", "node-high"),
                    Map.of("expression", "#input.amount > 500", "nextNodeId", "node-medium")
                ),
                "defaultNextNodeId", "node-low"
            ))
            .build();

        NodeResult result = executor.execute(conditionNode, ctx);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.getNextNodeId()).containsValue("node-high");
    }
}
```

- [ ] **Step 2: 运行测试验证 RED**

Run: `mvn test -Dtest=NodeExecutorServiceTest`
Expected: FAIL

- [ ] **Step 3: 创建 NodeExecutorService**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorPort;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NodeExecutorService {

    private final ExpressionParser parser = new SpelExpressionParser();

    public NodeResult execute(Node node, Context context) {
        return switch (node.getType()) {
            case DATA_PROCESSING -> executeDataProcessing(node, context);
            case CONDITION -> executeCondition(node, context);
        };
    }

    private NodeResult executeDataProcessing(Node node, Context context) {
        try {
            String outputKey = (String) node.getConfig().get("outputKey");
            String expression = (String) node.getConfig().get("expression");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            Object result = parser.parseExpression(expression).getValue(evalContext);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, result);

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(output)
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    private NodeResult executeCondition(Node node, Context context) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> conditions = (List<Map<String, String>>) node.getConfig().get("conditions");
            String defaultNextNodeId = (String) node.getConfig().get("defaultNextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            for (Map<String, String> condition : conditions) {
                String expression = condition.get("expression");
                String nextNodeId = condition.get("nextNodeId");

                Boolean result = parser.parseExpression(expression).getValue(evalContext, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return NodeResult.builder()
                        .nodeId(node.getId())
                        .status(ExecutionStatus.SUCCESS)
                        .nextNodeId(nextNodeId)
                        .output(Map.of())
                        .build();
                }
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .nextNodeId(defaultNextNodeId)
                .output(Map.of())
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
}
```

- [ ] **Step 4: 运行测试验证 GREEN**

Run: `mvn test -Dtest=NodeExecutorServiceTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/service/NodeExecutorService.java
git add src/test/java/com/powerflow/workflow/domain/service/NodeExecutorServiceTest.java
git commit -m "feat: add NodeExecutorService domain service"
```

---

## Task 11: WorkflowExecutor 领域服务

**Files:**
- Create: `src/main/java/com/powerflow/workflow/domain/service/WorkflowExecutor.java`
- Create: `src/test/java/com/powerflow/workflow/domain/service/WorkflowExecutorTest.java`

- [ ] **Step 1: 写 WorkflowExecutor 测试（TDD RED）**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkflowExecutorTest {

    @Test
    void should_execute_simple_linear_workflow() {
        WorkflowRepository repo = mock(WorkflowRepository.class);
        ExecutionLogRepository logRepo = mock(ExecutionLogRepository.class);

        Node node1 = Node.builder()
            .id("node-1")
            .name("Start")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.amount * 2"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("result", "output.double"))
            .build();

        Workflow workflow = Workflow.builder()
            .id("wf-1")
            .name("Simple Workflow")
            .startNodeId("node-1")
            .nodes(List.of(node1))
            .edges(List.of())
            .build();

        when(repo.findById("wf-1")).thenReturn(java.util.Optional.of(workflow));

        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(null);
        WorkflowExecutor executor = new WorkflowExecutor(repo, logRepo, contextManager, nodeExecutor);

        Context inputContext = new Context(Map.of("amount", 500));
        WorkflowExecutionResult result = executor.execute("wf-1", inputContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalContext().get("output.double")).containsValue(1000);
        verify(logRepo, atLeastOnce()).save(any(NodeExecution.class));
    }

    @Test
    void should_fail_when_workflow_not_found() {
        WorkflowRepository repo = mock(WorkflowRepository.class);
        ExecutionLogRepository logRepo = mock(ExecutionLogRepository.class);
        when(repo.findById("non-existent")).thenReturn(java.util.Optional.empty());

        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(null);
        WorkflowExecutor executor = new WorkflowExecutor(repo, logRepo, contextManager, nodeExecutor);

        Context ctx = new Context();
        org.junit.jupiter.api.Assertions.assertThrows(
            com.powerflow.workflow.exception.WorkflowNotFoundException.class,
            () -> executor.execute("non-existent", ctx)
        );
    }
}
```

- [ ] **Step 2: 运行测试验证 RED**

Run: `mvn test -Dtest=WorkflowExecutorTest`
Expected: FAIL

- [ ] **Step 3: 创建 WorkflowExecutor**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.exception.NodeExecutionException;
import com.powerflow.workflow.exception.WorkflowNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkflowExecutor {

    private final WorkflowRepository workflowRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final ContextManager contextManager;
    private final NodeExecutorService nodeExecutor;

    public WorkflowExecutor(WorkflowRepository workflowRepository,
                            ExecutionLogRepository executionLogRepository,
                            ContextManager contextManager,
                            NodeExecutorService nodeExecutor) {
        this.workflowRepository = workflowRepository;
        this.executionLogRepository = executionLogRepository;
        this.contextManager = contextManager;
        this.nodeExecutor = nodeExecutor;
    }

    public WorkflowExecutionResult execute(String workflowId, Context inputContext) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        return executeWorkflow(workflow, inputContext);
    }

    private WorkflowExecutionResult executeWorkflow(Workflow workflow, Context inputContext) {
        String executionId = UUID.randomUUID().toString();
        Context currentContext = new Context(inputContext.toMap());
        List<NodeExecution> executions = new ArrayList<>();
        String currentNodeId = workflow.getStartNodeId();

        while (currentNodeId != null) {
            Optional<Node> nodeOpt = workflow.findNodeById(currentNodeId);
            if (nodeOpt.isEmpty()) {
                break;
            }
            Node node = nodeOpt.get();

            LocalDateTime startTime = LocalDateTime.now();

            Map<String, Object> nodeInput = contextManager.extractNodeInput(node.getInputMapping(), currentContext);
            NodeResult result = nodeExecutor.execute(node, currentContext);

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            NodeExecution execution = NodeExecution.builder()
                .workflowExecutionId(executionId)
                .nodeId(node.getId())
                .status(result.getStatus())
                .input(nodeInput)
                .output(result.getOutput())
                .error(result.getError().orElse(null))
                .durationMs(durationMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .build();

            executionLogRepository.save(execution);
            executions.add(execution);

            if (!result.isSuccess()) {
                return WorkflowExecutionResult.builder()
                    .workflowId(workflow.getId())
                    .executionId(executionId)
                    .status(ExecutionStatus.FAILED)
                    .finalContext(currentContext)
                    .nodeExecutions(executions)
                    .error("Node " + node.getId() + " failed: " + result.getError().orElse("Unknown error"))
                    .build();
            }

            contextManager.writeNodeOutput(node.getOutputMapping(), result.getOutput(), currentContext);

            currentNodeId = result.getNextNodeId().orElse(null);
        }

        return WorkflowExecutionResult.builder()
            .workflowId(workflow.getId())
            .executionId(executionId)
            .status(ExecutionStatus.SUCCESS)
            .finalContext(currentContext)
            .nodeExecutions(executions)
            .build();
    }
}
```

- [ ] **Step 4: 运行测试验证 GREEN**

Run: `mvn test -Dtest=WorkflowExecutorTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/powerflow/workflow/domain/service/WorkflowExecutor.java
git add src/test/java/com/powerflow/workflow/domain/service/WorkflowExecutorTest.java
git commit -m "feat: add WorkflowExecutor domain service"
```

---

## Task 12: 内存适配器实现

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java`
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/logging/InMemoryExecutionLogRepository.java`

- [ ] **Step 1: 创建 InMemoryWorkflowRepository**

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Workflow save(Workflow workflow) {
        store.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}
```

- [ ] **Step 2: 创建 InMemoryExecutionLogRepository**

```java
package com.powerflow.workflow.adapter.outbound.logging;

import com.powerflow.workflow.domain.model.NodeExecution;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExecutionLogRepository implements ExecutionLogRepository {

    private final Map<String, List<NodeExecution>> store = new ConcurrentHashMap<>();

    @Override
    public void save(NodeExecution execution) {
        store.computeIfAbsent(execution.getWorkflowExecutionId(), k -> new ArrayList<>())
             .add(execution);
    }

    @Override
    public List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId) {
        return new ArrayList<>(store.getOrDefault(workflowExecutionId, List.of()));
    }
}
```

- [ ] **Step 3: 运行编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/
git commit -m "feat: add in-memory adapters"
```

---

## Task 13: AOP Aspect 实现

**Files:**
- Create: `src/main/java/com/powerflow/workflow/aop/NodeExecutionAspect.java`

- [ ] **Step 1: 创建 NodeExecutionAspect**

```java
package com.powerflow.workflow.aop;

import com.powerflow.workflow.domain.model.NodeExecution;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class NodeExecutionAspect {

    private final ExecutionLogRepository executionLogRepository;

    public NodeExecutionAspect(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    @Around("execution(* com.powerflow.workflow.domain.service.NodeExecutorService.execute(..))")
    public Object aroundNodeExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String nodeId = args.length > 0 ? ((com.powerflow.workflow.domain.model.Node) args[0]).getId() : "unknown";
        String executionId = "aspect-" + System.currentTimeMillis();

        LocalDateTime startTime = LocalDateTime.now();
        Object result = null;
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            status = ExecutionStatus.FAILED;
            error = t.getMessage();
            throw t;
        } finally {
            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            NodeExecution execution = NodeExecution.builder()
                .workflowExecutionId(executionId)
                .nodeId(nodeId)
                .status(status)
                .output(result != null ? java.util.Map.of("result", result) : java.util.Map.of())
                .error(error)
                .durationMs(durationMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .build();

            executionLogRepository.save(execution);
        }
    }
}
```

- [ ] **Step 2: 运行编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/powerflow/workflow/aop/NodeExecutionAspect.java
git commit -m "feat: add NodeExecutionAspect AOP"
```

---

## Task 14: WorkflowUseCase 实现

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/inbound/rest/WorkflowController.java`

- [ ] **Step 1: 创建 WorkflowUseCase 实现**

```java
package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.port.inbound.WorkflowUseCase;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController implements WorkflowUseCase {

    private final WorkflowExecutor workflowExecutor;
    private final WorkflowRepository workflowRepository;
    private final ContextManager contextManager;
    private final NodeExecutorService nodeExecutorService;

    public WorkflowController(WorkflowExecutor workflowExecutor,
                               WorkflowRepository workflowRepository,
                               ContextManager contextManager,
                               NodeExecutorService nodeExecutorService) {
        this.workflowExecutor = workflowExecutor;
        this.workflowRepository = workflowRepository;
        this.contextManager = contextManager;
        this.nodeExecutorService = nodeExecutorService;
    }

    @Override
    @PostMapping("/{workflowId}/execute")
    public WorkflowExecutionResult execute(@PathVariable String workflowId, @RequestBody Context inputContext) {
        return workflowExecutor.execute(workflowId, inputContext);
    }

    @Override
    @PostMapping("/{workflowId}/nodes/{nodeId}/test")
    public NodeResult testNode(@PathVariable String workflowId,
                               @PathVariable String nodeId,
                               @RequestBody Context inputContext) {
        return workflowRepository.findById(workflowId)
            .flatMap(wf -> wf.findNodeById(nodeId))
            .map(node -> nodeExecutorService.execute(node, inputContext))
            .orElseThrow(() -> new RuntimeException("Node not found: " + nodeId));
    }

    @PostMapping
    public Workflow createWorkflow(@RequestBody Workflow workflow) {
        return workflowRepository.save(workflow);
    }

    @GetMapping("/{workflowId}")
    public Workflow getWorkflow(@PathVariable String workflowId) {
        return workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
    }
}
```

- [ ] **Step 2: 运行编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/inbound/rest/WorkflowController.java
git commit -m "feat: add WorkflowController REST adapter"
```

---

## Task 15: 完整集成测试

**Files:**
- Create: `src/test/java/com/powerflow/workflow/integration/WorkflowExecutionIntegrationTest.java`

- [ ] **Step 1: 写完整工作流集成测试**

```java
package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.logging.InMemoryExecutionLogRepository;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutionIntegrationTest {

    private InMemoryWorkflowRepository workflowRepository;
    private InMemoryExecutionLogRepository logRepository;
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        logRepository = new InMemoryExecutionLogRepository();
        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(null);
        workflowExecutor = new WorkflowExecutor(workflowRepository, logRepository, contextManager, nodeExecutor);
    }

    @Test
    void should_execute_full_workflow_with_multiple_nodes() {
        Node calculateDiscount = Node.builder()
            .id("discount")
            .name("Calculate Discount")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "discountedAmount", "expression", "#input.amount * 0.9"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("discountedAmount", "output.discounted"))
            .build();

        Node checkDiscount = Node.builder()
            .id("check")
            .name("Check Discount")
            .type(NodeType.CONDITION)
            .config(Map.of(
                "conditions", List.of(
                    Map.of("expression", "#input.discountedAmount > 500", "nextNodeId", "high-discount"),
                    Map.of("expression", "#input.discountedAmount > 100", "nextNodeId", "medium-discount")
                ),
                "defaultNextNodeId", "low-discount"
            ))
            .inputMapping(Map.of("discountedAmount", "input.discounted"))
            .outputMapping(Map.of())
            .build();

        Workflow workflow = Workflow.builder()
            .id("test-wf")
            .name("Discount Workflow")
            .startNodeId("discount")
            .nodes(List.of(calculateDiscount, checkDiscount))
            .edges(List.of())
            .build();

        workflowRepository.save(workflow);

        Context inputContext = new Context(Map.of("amount", 1000));
        WorkflowExecutionResult result = workflowExecutor.execute("test-wf", inputContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalContext().get("output.discounted")).containsValue(900.0);
        assertThat(result.getNodeExecutions()).hasSize(2);
    }
}
```

- [ ] **Step 2: 运行集成测试验证**

Run: `mvn test -Dtest=WorkflowExecutionIntegrationTest`
Expected: PASS

- [ ] **Step 3: 运行全量测试**

Run: `mvn test`
Expected: ALL TESTS PASS

- [ ] **Step 4: 提交**

```bash
git add src/test/java/com/powerflow/workflow/integration/
git commit -m "test: add full workflow integration test"
```

---

## Task 16: 最终验证

- [ ] **Step 1: 运行完整构建**

Run: `mvn clean compile test`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: 检查覆盖率（可选）**

Run: `mvn test jacoco:report` (if jacoco configured)
Expected: Coverage report generated

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat: complete workflow core domain with DDD hexagonal architecture"
```

---

## 自检清单

- [ ] 所有域模型已创建并测试
- [ ] 所有端口接口已定义
- [ ] WorkflowExecutor 实现 DAG 拓扑执行
- [ ] NodeExecutor 支持 DATA_PROCESSING 和 CONDITION
- [ ] ContextManager 处理输入输出映射
- [ ] RuleEvaluator 使用 SpEL 求值
- [ ] AOP Aspect 记录执行日志
- [ ] 内存适配器支撑集成测试
- [ ] REST API 可执行工作流和测试节点
- [ ] 全量测试通过

---

**计划完成。执行方式选择：**

**1. Subagent-Driven（推荐）** — 每个 Task 由独立 subagent 执行，Task 间有检查点

**2. Inline Execution** — 当前 session 内批量执行

选择哪种方式？