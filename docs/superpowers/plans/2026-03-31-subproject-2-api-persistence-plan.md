# 子项目 2：后端 API 与持久化

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 将子项目1的内存实现替换为 PostgreSQL + Redis 持久化，完成完整的 REST API。

**Architecture:** 子项目1已实现核心域和内存适配器，本项目替换为 JPA+PostgreSQL 持久化 Workflow 和 NodeExecution，Redis 缓存 Context。

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Spring Data Redis, Docker

---

## 文件结构

```
src/main/java/com/powerflow/workflow/
├── adapter/
│   ├── inbound/
│   │   └── rest/
│   │       └── WorkflowController.java  # 已存在
│   └── outbound/
│       ├── persistence/
│       │   ├── JpaWorkflowRepository.java      # NEW
│       │   ├── JpaNodeExecutionRepository.java # NEW
│       │   └── entity/
│       │       ├── WorkflowEntity.java         # NEW
│       │       ├── NodeEntity.java             # NEW
│       │       ├── EdgeEntity.java             # NEW
│       │       └── NodeExecutionEntity.java     # NEW
│       └── cache/
│           └── RedisContextCache.java          # NEW
├── config/
│   └── RedisConfig.java                        # NEW
└── resources/
    ├── application.yml                         # MODIFY - 添加DB/Redis配置
    └── db/migration/
        └── V1__init_schema.sql                 # NEW - Flyway迁移脚本
```

---

## Task 1: Docker Compose 配置

**Files:**
- Create: `docker-compose.yml`
- Create: `src/main/resources/application.yml`（替换 application.properties）

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: powerflow-postgres
    environment:
      POSTGRES_DB: powerflow
      POSTGRES_USER: powerflow
      POSTGRES_PASSWORD: powerflow123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: powerflow-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  application:
    name: powerflow

  datasource:
    url: jdbc:postgresql://localhost:5432/powerflow
    username: powerflow
    password: powerflow123
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379

  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

logging:
  level:
    com.powerflow: DEBUG
    org.springframework.web: INFO
```

- [ ] **Step 3: 添加 Maven 依赖到 pom.xml**

在 dependencies 中添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

- [ ] **Step 4: 运行 docker-compose up -d 启动数据库**

Run: `docker-compose up -d`
Expected: postgres 和 redis 容器启动

- [ ] **Step 5: 提交**

```bash
git add docker-compose.yml src/main/resources/application.yml pom.xml
git commit -m "feat(subproject-2): add Docker Compose, PostgreSQL, Redis, Flyway config"
```

---

## Task 2: JPA Entity 定义

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/entity/WorkflowEntity.java`
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/entity/NodeEntity.java`
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/entity/EdgeEntity.java`
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/entity/NodeExecutionEntity.java`

- [ ] **Step 1: 创建 WorkflowEntity**

```java
package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflows")
public class WorkflowEntity {

    @Id
    private String id;

    private String name;
    private String description;
    private String startNodeId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_id")
    private List<NodeEntity> nodes = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_id")
    private List<EdgeEntity> edges = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
    public List<NodeEntity> getNodes() { return nodes; }
    public void setNodes(List<NodeEntity> nodes) { this.nodes = nodes; }
    public List<EdgeEntity> getEdges() { return edges; }
    public void setEdges(List<EdgeEntity> edges) { this.edges = edges; }
}
```

- [ ] **Step 2: 创建 NodeEntity**

```java
package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "nodes")
public class NodeEntity {

    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private String type;

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> config = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> inputMapping = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> outputMapping = new HashMap<>();

    private String workflowId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public Map<String, String> getInputMapping() { return inputMapping; }
    public void setInputMapping(Map<String, String> inputMapping) { this.inputMapping = inputMapping; }
    public Map<String, String> getOutputMapping() { return outputMapping; }
    public void setOutputMapping(Map<String, String> outputMapping) { this.outputMapping = outputMapping; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
}
```

- [ ] **Step 3: 创建 EdgeEntity**

```java
package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "edges")
public class EdgeEntity {

    @Id
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private String condition;
    private String workflowId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
}
```

- [ ] **Step 4: 创建 NodeExecutionEntity**

```java
package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "node_executions")
public class NodeExecutionEntity {

    @Id
    private String id;

    private String workflowExecutionId;
    private String nodeId;

    @Enumerated(EnumType.STRING)
    private String status;

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> input = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> output = new HashMap<>();

    private String error;
    private long durationMs;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkflowExecutionId() { return workflowExecutionId; }
    public void setWorkflowExecutionId(String workflowExecutionId) { this.workflowExecutionId = workflowExecutionId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
```

- [ ] **Step 5: 创建 MapToJsonConverter**

```java
package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) return "{}";
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null) return new HashMap<>();
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/entity/
git commit -m "feat(subproject-2): add JPA entities for Workflow, Node, Edge, NodeExecution"
```

---

## Task 3: Flyway 迁移脚本

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: 创建迁移脚本**

```sql
CREATE TABLE workflows (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_node_id VARCHAR(255) NOT NULL
);

CREATE TABLE nodes (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    config JSONB NOT NULL DEFAULT '{}',
    input_mapping JSONB NOT NULL DEFAULT '{}',
    output_mapping JSONB NOT NULL DEFAULT '{}',
    workflow_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE
);

CREATE TABLE edges (
    id VARCHAR(255) PRIMARY KEY,
    from_node_id VARCHAR(255) NOT NULL,
    to_node_id VARCHAR(255) NOT NULL,
    condition TEXT,
    workflow_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE
);

CREATE TABLE node_executions (
    id VARCHAR(255) PRIMARY KEY,
    workflow_execution_id VARCHAR(255) NOT NULL,
    node_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    input JSONB NOT NULL DEFAULT '{}',
    output JSONB NOT NULL DEFAULT '{}',
    error TEXT,
    duration_ms BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);

CREATE INDEX idx_node_executions_workflow_execution_id ON node_executions(workflow_execution_id);
CREATE INDEX idx_nodes_workflow_id ON nodes(workflow_id);
CREATE INDEX idx_edges_workflow_id ON edges(workflow_id);
```

- [ ] **Step 2: 添加 jackson-databind 依赖**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

- [ ] **Step 3: 提交**

```bash
git add src/main/resources/db/migration/V1__init_schema.sql pom.xml
git commit -m "feat(subproject-2): add Flyway migration script V1"
```

---

## Task 4: JPA Repository 接口

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/JpaWorkflowRepository.java`
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/JpaNodeExecutionRepository.java`

- [ ] **Step 1: 创建 JpaWorkflowRepository**

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWorkflowRepository extends JpaRepository<WorkflowEntity, String> {
}
```

- [ ] **Step 2: 创建 JpaNodeExecutionRepository**

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.NodeExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNodeExecutionRepository extends JpaRepository<NodeExecutionEntity, String> {
    Page<NodeExecutionEntity> findByWorkflowExecutionId(String workflowExecutionId, Pageable pageable);
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/JpaWorkflowRepository.java
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/JpaNodeExecutionRepository.java
git commit -m "feat(subproject-2): add JPA repository interfaces"
```

---

## Task 5: Entity Mapper

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/EntityMapper.java`

- [ ] **Step 1: 创建 EntityMapper**

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.*;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public Workflow toDomain(WorkflowEntity entity) {
        if (entity == null) return null;

        List<Node> nodes = entity.getNodes().stream()
            .map(this::toNodeDomain)
            .collect(Collectors.toList());

        List<Edge> edges = entity.getEdges().stream()
            .map(this::toEdgeDomain)
            .collect(Collectors.toList());

        return Workflow.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .startNodeId(entity.getStartNodeId())
            .nodes(nodes)
            .edges(edges)
            .build();
    }

    public WorkflowEntity toEntity(Workflow workflow) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.getId());
        entity.setName(workflow.getName());
        entity.setDescription(workflow.getDescription());
        entity.setStartNodeId(workflow.getStartNodeId());
        return entity;
    }

    public Node toNodeDomain(NodeEntity entity) {
        return Node.builder()
            .id(entity.getId())
            .name(entity.getName())
            .type(NodeType.valueOf(entity.getType()))
            .config(entity.getConfig())
            .inputMapping(entity.getInputMapping())
            .outputMapping(entity.getOutputMapping())
            .build();
    }

    public NodeEntity toNodeEntity(Node node, String workflowId) {
        NodeEntity entity = new NodeEntity();
        entity.setId(node.getId());
        entity.setName(node.getName());
        entity.setType(node.getType().name());
        entity.setConfig(node.getConfig());
        entity.setInputMapping(node.getInputMapping());
        entity.setOutputMapping(node.getOutputMapping());
        entity.setWorkflowId(workflowId);
        return entity;
    }

    public Edge toEdgeDomain(EdgeEntity entity) {
        return Edge.builder()
            .id(entity.getId())
            .fromNodeId(entity.getFromNodeId())
            .toNodeId(entity.getToNodeId())
            .condition(entity.getCondition())
            .build();
    }

    public EdgeEntity toEdgeEntity(Edge edge, String workflowId) {
        EdgeEntity entity = new EdgeEntity();
        entity.setId(edge.getId());
        entity.setFromNodeId(edge.getFromNodeId());
        entity.setToNodeId(edge.getToNodeId());
        entity.setCondition(edge.getCondition());
        entity.setWorkflowId(workflowId);
        return entity;
    }

    public NodeExecution toNodeExecutionDomain(NodeExecutionEntity entity) {
        return NodeExecution.builder()
            .id(entity.getId())
            .workflowExecutionId(entity.getWorkflowExecutionId())
            .nodeId(entity.getNodeId())
            .status(ExecutionStatus.valueOf(entity.getStatus()))
            .input(entity.getInput())
            .output(entity.getOutput())
            .error(entity.getError())
            .durationMs(entity.getDurationMs())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .build();
    }

    public NodeExecutionEntity toNodeExecutionEntity(NodeExecution execution) {
        NodeExecutionEntity entity = new NodeExecutionEntity();
        entity.setId(execution.getId());
        entity.setWorkflowExecutionId(execution.getWorkflowExecutionId());
        entity.setNodeId(execution.getNodeId());
        entity.setStatus(execution.getStatus().name());
        entity.setInput(execution.getInput());
        entity.setOutput(execution.getOutput());
        entity.setError(execution.getError().orElse(null));
        entity.setDurationMs(execution.getDurationMs());
        entity.setStartTime(execution.getStartTime());
        entity.setEndTime(execution.getEndTime());
        return entity;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/EntityMapper.java
git commit -m "feat(subproject-2): add entity mapper"
```

---

## Task 6: Postgres WorkflowRepository 实现

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/PostgresWorkflowRepository.java`

- [ ] **Step 1: 创建 PostgresWorkflowRepository**

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.*;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PostgresWorkflowRepository implements WorkflowRepository {

    private final JpaWorkflowRepository jpaRepository;
    private final EntityMapper mapper;

    public PostgresWorkflowRepository(JpaWorkflowRepository jpaRepository, EntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Optional<Workflow> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Workflow save(Workflow workflow) {
        WorkflowEntity entity = mapper.toEntity(workflow);

        List<NodeEntity> nodeEntities = workflow.getNodes().stream()
            .map(n -> mapper.toNodeEntity(n, workflow.getId()))
            .toList();
        entity.setNodes(nodeEntities);

        List<EdgeEntity> edgeEntities = workflow.getEdges().stream()
            .map(e -> mapper.toEdgeEntity(e, workflow.getId()))
            .toList();
        entity.setEdges(edgeEntities);

        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/PostgresWorkflowRepository.java
git commit -m "feat(subproject-2): add PostgresWorkflowRepository implementation"
```

---

## Task 7: Postgres ExecutionLogRepository 实现

**Files:**
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/PostgresExecutionLogRepository.java`

- [ ] **Step 1: 创建 PostgresExecutionLogRepository**

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.NodeExecutionEntity;
import com.powerflow.workflow.domain.model.NodeExecution;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PostgresExecutionLogRepository implements ExecutionLogRepository {

    private final JpaNodeExecutionRepository jpaRepository;
    private final EntityMapper mapper;

    public PostgresExecutionLogRepository(JpaNodeExecutionRepository jpaRepository, EntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void save(NodeExecution execution) {
        jpaRepository.save(mapper.toNodeExecutionEntity(execution));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId) {
        return jpaRepository.findByWorkflowExecutionId(workflowExecutionId, Pageable.unpaged())
            .getContent()
            .stream()
            .map(mapper::toNodeExecutionDomain)
            .toList();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/PostgresExecutionLogRepository.java
git commit -m "feat(subproject-2): add PostgresExecutionLogRepository implementation"
```

---

## Task 8: Redis Context 缓存

**Files:**
- Create: `src/main/java/com/powerflow/workflow/config/RedisConfig.java`
- Create: `src/main/java/com/powerflow/workflow/adapter/outbound/cache/RedisContextCache.java`

- [ ] **Step 1: 创建 RedisConfig**

```java
package com.powerflow.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 2: 创建 RedisContextCache**

```java
package com.powerflow.workflow.adapter.outbound.cache;

import com.powerflow.workflow.domain.model.Context;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisContextCache {

    private static final String KEY_PREFIX = "context:";
    private static final Duration TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisContextCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String executionId, Context context) {
        redisTemplate.opsForValue().set(KEY_PREFIX + executionId, context.toMap(), TTL);
    }

    public Optional<Context> get(String executionId) {
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + executionId);
        if (value instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
            return Optional.of(new Context(map));
        }
        return Optional.empty();
    }

    public void delete(String executionId) {
        redisTemplate.delete(KEY_PREFIX + executionId);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/powerflow/workflow/config/RedisConfig.java
git add src/main/java/com/powerflow/workflow/adapter/outbound/cache/RedisContextCache.java
git commit -m "feat(subproject-2): add Redis context cache"
```

---

## Task 9: 禁用内存适配器，启用 JPA 适配器

**Files:**
- Modify: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java`
- Modify: `src/main/java/com/powerflow/workflow/adapter/outbound/logging/InMemoryExecutionLogRepository.java`

- [ ] **Step 1: 给内存适配器添加 @Primary 注解（优先级低于 JPA 实现）**

在 InMemoryWorkflowRepository 类上添加 `@Primary` 注解。

在 InMemoryExecutionLogRepository 类上添加 `@Primary` 注解。

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java
git add src/main/java/com/powerflow/workflow/adapter/outbound/logging/InMemoryExecutionLogRepository.java
git commit -m "feat(subproject-2): add @Primary to in-memory adapters for test fallback"
```

---

## Task 10: 删除内存适配器，使用 JPA 实现

**Files:**
- Delete: `src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java`
- Delete: `src/main/java/com/powerflow/workflow/adapter/outbound/logging/InMemoryExecutionLogRepository.java`

- [ ] **Step 1: 删除内存适配器文件**

```bash
rm src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java
rm src/main/java/com/powerflow/workflow/adapter/outbound/logging/InMemoryExecutionLogRepository.java
```

- [ ] **Step 2: 修改 WorkflowController 使用 JPA Repository**

修改 WorkflowController 中的 @Autowired 注入，改为注入 JpaWorkflowRepository 和 JpaNodeExecutionRepository。

- [ ] **Step 3: 提交**

```bash
git rm src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java
git rm src/main/java/com/powerflow/workflow/adapter/outbound/logging/InMemoryExecutionLogRepository.java
git commit -m "refactor(subproject-2): remove in-memory adapters, use JPA implementations"
```

---

## Task 11: 集成测试

**Files:**
- Create: `src/test/java/com/powerflow/workflow/integration/WorkflowPersistenceIntegrationTest.java`

- [ ] **Step 1: 创建集成测试**

```java
package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.persistence.EntityMapper;
import com.powerflow.workflow.adapter.outbound.persistence.JpaNodeExecutionRepository;
import com.powerflow.workflow.adapter.outbound.persistence.JpaWorkflowRepository;
import com.powerflow.workflow.adapter.outbound.persistence.PostgresExecutionLogRepository;
import com.powerflow.workflow.adapter.outbound.persistence.PostgresWorkflowRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowPersistenceIntegrationTest {

    @Autowired
    private JpaWorkflowRepository workflowRepository;

    @Autowired
    private JpaNodeExecutionRepository executionRepository;

    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        EntityMapper mapper = new EntityMapper();
        PostgresWorkflowRepository pgWorkflowRepo = new PostgresWorkflowRepository(workflowRepository, mapper);
        PostgresExecutionLogRepository pgExecRepo = new PostgresExecutionLogRepository(executionRepository, mapper);
        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(null);
        workflowExecutor = new WorkflowExecutor(pgWorkflowRepo, pgExecRepo, contextManager, nodeExecutor);
    }

    @Test
    void should_persist_and_retrieve_workflow() {
        Node node = Node.builder()
            .id("test-node")
            .name("Test Node")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.value * 2"))
            .inputMapping(Map.of("value", "input.value"))
            .outputMapping(Map.of("result", "output.result"))
            .build();

        Workflow workflow = Workflow.builder()
            .id("test-wf")
            .name("Test Workflow")
            .startNodeId("test-node")
            .nodes(List.of(node))
            .edges(List.of())
            .build();

        workflowRepository.save(workflow);

        var saved = workflowRepository.findById("test-wf");
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("Test Workflow");
        assertThat(saved.get().getNodes()).hasSize(1);
    }
}
```

- [ ] **Step 2: 创建 test profile 配置**

Create: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/powerflow_test
    username: powerflow
    password: powerflow123
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

- [ ] **Step 3: 运行集成测试**

Run: `mvn test -Dtest=WorkflowPersistenceIntegrationTest`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add src/test/java/com/powerflow/workflow/integration/WorkflowPersistenceIntegrationTest.java
git add src/test/resources/application-test.yml
git commit -m "test(subproject-2): add workflow persistence integration test"
```

---

## Task 12: 完整验证

- [ ] **Step 1: 运行完整构建**

Run: `mvn clean compile test`
Expected: BUILD SUCCESS

- [ ] **Step 2: 推送代码**

```bash
git add -A
git commit -m "feat(subproject-2): complete API persistence layer with PostgreSQL and Redis"
git push origin subproject-2-api-persistence
```

---

## 自检清单

- [ ] Docker Compose 启动 PostgreSQL + Redis
- [ ] JPA Entity 映射 Workflow, Node, Edge, NodeExecution
- [ ] Flyway 迁移脚本创建数据库表
- [ ] JPA Repository 接口
- [ ] EntityMapper 实体与域模型转换
- [ ] PostgresWorkflowRepository 实现
- [ ] PostgresExecutionLogRepository 实现
- [ ] Redis Context 缓存
- [ ] 集成测试通过
- [ ] 代码推送到 subproject-2-api-persistence 分支
