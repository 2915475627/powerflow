package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.logging.InMemoryExecutionLogRepository;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import com.powerflow.workflow.domain.service.handler.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Social Content Business Scenarios Integration Tests
 *
 * Tests based on social content platform business scenarios:
 * - Content moderation with three-level condition classification
 * - User profile building with parallel data collection
 * - Recommendation distribution with parallel push
 * - Sentiment analysis with parallel LLM analysis
 */
class SocialContentScenariosTest {

    private InMemoryWorkflowRepository workflowRepository;
    private InMemoryExecutionLogRepository logRepository;
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        logRepository = new InMemoryExecutionLogRepository();
        ContextManager contextManager = new ContextManager();

        HttpRequestHandler httpHandler = new HttpRequestHandler(
            new com.powerflow.workflow.adapter.outbound.http.RestTemplateHttpClientAdapter()
        );
        LlmCallHandler llmHandler = new LlmCallHandler();
        ParallelHandler parallelHandler = new ParallelHandler(workflowRepository);
        ForeachHandler foreachHandler = new ForeachHandler();
        SubworkflowHandler subworkflowHandler = new SubworkflowHandler(workflowRepository);
        TryCatchHandler tryCatchHandler = new TryCatchHandler();
        RetryHandler retryHandler = new RetryHandler();

        NodeExecutorService nodeExecutor = new NodeExecutorService(
            httpHandler, llmHandler, parallelHandler, foreachHandler,
            subworkflowHandler, tryCatchHandler, retryHandler
        );
        workflowExecutor = new WorkflowExecutor(workflowRepository, logRepository, contextManager, nodeExecutor);
    }

    @Nested
    @DisplayName("Scenario 1: Content Moderation (内容审核)")
    class ContentModerationScenario {

        @Test
        @DisplayName("Should moderate content with three-level condition")
        void should_moderate_content_with_condition() {
            // 1. Get content
            Node getContent = Node.builder()
                .id("get_content")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "content", "expression", "'用户发布内容'", "nextNodeId", "audit"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("content", "content"))
                .build();

            // 2. LLM audit (simulated)
            Node audit = Node.builder()
                .id("audit")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "auditResult", "expression", "'pass'", "nextNodeId", "decision"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("auditResult", "auditResult"))
                .build();

            // 3. CONDITION three-level classification
            List<Map<String, String>> conditions = new ArrayList<>();
            conditions.add(Map.of("expression", "#input.auditResult == 'pass'", "nextNodeId", "publish"));
            conditions.add(Map.of("expression", "#input.auditResult == 'review'", "nextNodeId", "manual_review"));
            conditions.add(Map.of("expression", "#input.auditResult == 'block'", "nextNodeId", "block"));

            Node decision = Node.builder()
                .id("decision")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", conditions,
                    "defaultNextNodeId", "manual_review"
                ))
                .inputMapping(Map.of("auditResult", "auditResult"))
                .outputMapping(Map.of())
                .build();

            // 4. Publish
            Node publish = Node.builder()
                .id("publish")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'内容已发布'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 5. Manual review
            Node manualReview = Node.builder()
                .id("manual_review")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'需人工复审'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 6. Block
            Node block = Node.builder()
                .id("block")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'内容已拦截'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("content-moderation-wf")
                .name("Content Moderation")
                .startNodeId("get_content")
                .nodes(List.of(getContent, audit, decision, publish, manualReview, block))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("get_content").toNodeId("audit").build(),
                    Edge.builder().id("e2").fromNodeId("audit").toNodeId("decision").build(),
                    Edge.builder().id("e3").fromNodeId("decision").toNodeId("publish").build(),
                    Edge.builder().id("e4").fromNodeId("decision").toNodeId("manual_review").build(),
                    Edge.builder().id("e5").fromNodeId("decision").toNodeId("block").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("content-moderation-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalContext().get("output.result").map(Object::toString))
                .contains("内容已发布");
        }
    }

    @Nested
    @DisplayName("Scenario 2: User Profile (用户画像)")
    class UserProfileScenario {

        @Test
        @DisplayName("Should build user profile in parallel")
        void should_build_profile_in_parallel() {
            // 1. PARALLEL parallel collection
            Node parallelFetch = Node.builder()
                .id("parallel_fetch")
                .type(NodeType.PARALLEL)
                .config(Map.of(
                    "branches", List.of(
                        Map.of("name", "basic", "nodeIds", List.of("basic_node")),
                        Map.of("name", "behavior", "nodeIds", List.of("behavior_node")),
                        Map.of("name", "interest", "nodeIds", List.of("interest_node")),
                        Map.of("name", "social", "nodeIds", List.of("social_node"))
                    ),
                    "strategy", "AND",
                    "nextNodeId", "merge"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 2. Four collection nodes
            Node basicNode = Node.builder()
                .id("basic_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "basic", "expression", "'基本信息'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("basic", "basic"))
                .build();

            Node behaviorNode = Node.builder()
                .id("behavior_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "behavior", "expression", "'行为数据'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("behavior", "behavior"))
                .build();

            Node interestNode = Node.builder()
                .id("interest_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "interest", "expression", "'兴趣数据'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("interest", "interest"))
                .build();

            Node socialNode = Node.builder()
                .id("social_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "social", "expression", "'社交数据'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("social", "social"))
                .build();

            // 3. Merge
            Node merge = Node.builder()
                .id("merge")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "profile", "expression", "'画像已生成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("profile", "output.profile"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("user-profile-wf")
                .name("User Profile")
                .startNodeId("parallel_fetch")
                .nodes(List.of(parallelFetch, basicNode, behaviorNode, interestNode, socialNode, merge))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parallel_fetch").toNodeId("basic_node").build(),
                    Edge.builder().id("e2").fromNodeId("parallel_fetch").toNodeId("behavior_node").build(),
                    Edge.builder().id("e3").fromNodeId("parallel_fetch").toNodeId("interest_node").build(),
                    Edge.builder().id("e4").fromNodeId("parallel_fetch").toNodeId("social_node").build(),
                    Edge.builder().id("e5").fromNodeId("basic_node").toNodeId("merge").build(),
                    Edge.builder().id("e6").fromNodeId("behavior_node").toNodeId("merge").build(),
                    Edge.builder().id("e7").fromNodeId("interest_node").toNodeId("merge").build(),
                    Edge.builder().id("e8").fromNodeId("social_node").toNodeId("merge").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("user-profile-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Recommendation Distribution (推荐分发)")
    class RecommendationDistributionScenario {

        @Test
        @DisplayName("Should distribute recommendations in parallel")
        void should_distribute_in_parallel() {
            // PARALLEL distribution - PARALLEL must be start node
            Node parallelDist = Node.builder()
                .id("parallel_dist")
                .type(NodeType.PARALLEL)
                .config(Map.of(
                    "branches", List.of(
                        Map.of("name", "generate", "nodeIds", List.of("generate_node")),
                        Map.of("name", "feed", "nodeIds", List.of("push_feed")),
                        Map.of("name", "email", "nodeIds", List.of("push_email")),
                        Map.of("name", "sms", "nodeIds", List.of("push_sms"))
                    ),
                    "strategy", "AND",
                    "nextNodeId", "complete"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 1. LLM generate recommendations (runs in parallel with push)
            Node generateNode = Node.builder()
                .id("generate_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "recommendations", "expression", "['推荐1','推荐2','推荐3']"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("recommendations", "recommendations"))
                .build();

            // 2. Three push nodes
            Node pushFeed = Node.builder()
                .id("push_feed")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "feedResult", "expression", "'信息流推送完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("feedResult", "feedResult"))
                .build();

            Node pushEmail = Node.builder()
                .id("push_email")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "emailResult", "expression", "'邮件推送完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("emailResult", "emailResult"))
                .build();

            Node pushSms = Node.builder()
                .id("push_sms")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "smsResult", "expression", "'短信推送完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("smsResult", "smsResult"))
                .build();

            // 3. Complete
            Node complete = Node.builder()
                .id("complete")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'分发完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("recommendation-dist-wf")
                .name("Recommendation Distribution")
                .startNodeId("parallel_dist")
                .nodes(List.of(parallelDist, generateNode, pushFeed, pushEmail, pushSms, complete))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parallel_dist").toNodeId("generate_node").build(),
                    Edge.builder().id("e2").fromNodeId("parallel_dist").toNodeId("push_feed").build(),
                    Edge.builder().id("e3").fromNodeId("parallel_dist").toNodeId("push_email").build(),
                    Edge.builder().id("e4").fromNodeId("parallel_dist").toNodeId("push_sms").build(),
                    Edge.builder().id("e5").fromNodeId("generate_node").toNodeId("complete").build(),
                    Edge.builder().id("e6").fromNodeId("push_feed").toNodeId("complete").build(),
                    Edge.builder().id("e7").fromNodeId("push_email").toNodeId("complete").build(),
                    Edge.builder().id("e8").fromNodeId("push_sms").toNodeId("complete").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("recommendation-dist-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Sentiment Analysis (舆情分析)")
    class SentimentAnalysisScenario {

        @Test
        @DisplayName("Should analyze sentiment in parallel")
        void should_analyze_sentiment_in_parallel() {
            // 1. Crawl content
            Node crawl = Node.builder()
                .id("crawl")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "content", "expression", "'社交媒体内容'", "nextNodeId", "parallel"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("content", "content"))
                .build();

            // 2. PARALLEL parallel analysis
            Node parallelAnalyze = Node.builder()
                .id("parallel")
                .type(NodeType.PARALLEL)
                .config(Map.of(
                    "branches", List.of(
                        Map.of("name", "sentiment", "nodeIds", List.of("sentiment_node")),
                        Map.of("name", "topic", "nodeIds", List.of("topic_node")),
                        Map.of("name", "heat", "nodeIds", List.of("heat_node"))
                    ),
                    "strategy", "AND",
                    "nextNodeId", "aggregate"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 3. Three analysis nodes
            Node sentimentNode = Node.builder()
                .id("sentiment_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "sentiment", "expression", "'正面'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("sentiment", "sentiment"))
                .build();

            Node topicNode = Node.builder()
                .id("topic_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "topic", "expression", "'产品反馈'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("topic", "topic"))
                .build();

            Node heatNode = Node.builder()
                .id("heat_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "heat", "expression", 85))
                .inputMapping(Map.of())
                .outputMapping(Map.of("heat", "heat"))
                .build();

            // 4. Aggregate
            Node aggregate = Node.builder()
                .id("aggregate")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "report", "expression", "'舆情分析完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("report", "output.report"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("sentiment-analysis-wf")
                .name("Sentiment Analysis")
                .startNodeId("crawl")
                .nodes(List.of(crawl, parallelAnalyze, sentimentNode, topicNode, heatNode, aggregate))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("crawl").toNodeId("parallel").build(),
                    Edge.builder().id("e2").fromNodeId("parallel").toNodeId("sentiment_node").build(),
                    Edge.builder().id("e3").fromNodeId("parallel").toNodeId("topic_node").build(),
                    Edge.builder().id("e4").fromNodeId("parallel").toNodeId("heat_node").build(),
                    Edge.builder().id("e5").fromNodeId("sentiment_node").toNodeId("aggregate").build(),
                    Edge.builder().id("e6").fromNodeId("topic_node").toNodeId("aggregate").build(),
                    Edge.builder().id("e7").fromNodeId("heat_node").toNodeId("aggregate").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("sentiment-analysis-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }
}
