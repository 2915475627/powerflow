# PowerFlow AI工作流平台 - 设计文档

## 概述

将PowerFlow从通用工作流引擎改造为AI工作流平台，底层保持类LangChain4j的可嵌入Java库，上层提供聊天界面 + 工作流执行链路展示 + REST API。

## 模块结构

| 模块 | 说明 |
|------|------|
| `powerflow-engine` | 纯POJO工作流引擎（原powerflow-core），零Spring依赖 |
| `powerflow-nodes` | 节点执行器：基础节点 + AI节点 |
| `powerflow-web` | Spring Boot REST API + 聊天界面（原powerflow-adapter-springboot重命名）|
| `powerflow-example` | 示例应用 |

## 第一期：LLM + 对话能力

### 1. LLM Provider抽象

定义`LlmProvider`接口，内置多 provider 实现：

```java
public interface LlmProvider {
    String generate(String prompt, LlmConfig config);
    Stream<String> generateStream(String prompt, LlmConfig config);
}
```

实现：
- `OpenAiLlmProvider` - OpenAI GPT系列
- `DeepSeekLlmProvider` - DeepSeek系列
- `ZhipuLlmProvider` - 智谱GLM系列

配置项（`LlmConfig`）：
- `model` - 模型名称
- `temperature` - 随机性 (0-1)
- `maxTokens` - 最大token数
- `systemPrompt` - 系统提示词
- `apiKey` / `baseUrl` - 连接信息

### 2. 扩展LLM_CALL节点

现有`LlmCallNodeExecutor`改造为使用`LlmProvider`：

```java
public class LlmCallNodeExecutor implements NodeExecutorPort {
    private LlmProvider llmProvider; // 通过SPI或配置注入

    @Override
    public NodeResult execute(Node node, Context context) {
        String prompt = resolveTemplate(node.getConfig().get("prompt"), context);
        String model = node.getConfig().getOrDefault("model", "gpt-4");
        double temperature = (double) node.getConfig().getOrDefault("temperature", 0.7);

        LlmConfig config = LlmConfig.builder()
            .model(model)
            .temperature(temperature)
            .systemPrompt(node.getConfig().get("systemPrompt"))
            .build();

        String response = llmProvider.generate(prompt, config);
        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(Map.of("response", response))
            .build();
    }
}
```

### 3. Prompt模板节点（新增）

```java
public class PromptTemplateNodeExecutor implements NodeExecutorPort {
    @Override
    public NodeResult execute(Node node, Context context) {
        String template = (String) node.getConfig().get("template");
        Map<String, Object> variables = (Map) node.getConfig().get("variables");

        String rendered = renderTemplate(template, variables, context);
        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(Map.of("prompt", rendered))
            .build();
    }

    private String renderTemplate(String template, Map<String, Object> vars, Context ctx) {
        // 支持 {{variable}} 语法，从vars和ctx上下文取值
    }
}
```

### 4. 对话记忆（Context扩展）

现有`Context`类增加session概念：

```java
public class Context {
    Map<String, Object> data;        // 当前节点数据
    List<ChatMessage> history;       // 多轮对话历史 [{role, content}]
    String sessionId;                // 会话ID

    public void addMessage(String role, String content) { ... }
    public List<ChatMessage> getHistory() { ... }
}

public class ChatMessage {
    String role;    // "user" | "assistant" | "system"
    String content;
}
```

### 5. 应用层：聊天界面

**页面功能**：
- 对话输入框
- 消息列表（user/assistant）
- 工作流选择和执行
- 执行链路可视化（状态树/日志流）

**API端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 发送对话消息 |
| GET | `/api/chat/{sessionId}/history` | 获取对话历史 |
| POST | `/api/chat/{sessionId}/execute` | 基于对话执行工作流 |
| GET | `/api/workflows/{id}/executions/{execId}` | 获取执行链路详情 |

**响应格式**：

```json
{
  "sessionId": "xxx",
  "message": "LLM回复内容",
  "workflowExecutionId": "yyy",
  "chain": [
    {"nodeId": "start", "status": "SUCCESS", "output": {}},
    {"nodeId": "llm_1", "status": "SUCCESS", "output": {"response": "..."}},
    {"nodeId": "end", "status": "SUCCESS", "output": {}}
  ]
}
```

## NodeType枚举扩展

新增：
- `PROMPT_TEMPLATE` - Prompt模板节点
- `TOOL_CALL` - 工具调用节点（预留）

## 后续扩展（不包含在第一期）

- 知识库RAG（文档上传→分片→Embedding→向量存储→检索）
- 插件系统（类Coze工具生态）
- 多渠道发布

## 技术要点

1. **SPI加载**：通过`ServiceLoader`发现并加载`LlmProvider`实现
2. **配置外置**：provider信息通过环境变量或配置文件注入，不硬编码
3. **流式输出**：LLM stream模式用于长文本实时返回
4. **前端解耦**：REST API保持通用，不绑定特定前端
