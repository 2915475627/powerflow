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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E-commerce Business Scenarios Integration Tests
 *
 * Tests based on the e-commerce MVP business scenarios design document.
 * These tests verify the workflow engine can handle:
 * - Intent recognition and routing (BRANCH node)
 * - Sequential workflow execution
 * - Condition-based routing (CONDITION node)
 * - Data processing and transformation
 */
class EcommerceBusinessScenariosTest {

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
    @DisplayName("Scenario 1: Product Search Workflow (商品搜索)")
    class ProductSearchScenario {

        @Test
        @DisplayName("Should execute product search workflow")
        void should_execute_product_search() {
            // Step 1: Build search query
            Node buildSearch = Node.builder()
                .id("build_search")
                .name("Build Search Request")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "query", "expression", "'Nike 运动鞋'", "nextNodeId", "format"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("query", "query"))
                .build();

            // Step 2: Format results
            Node format = Node.builder()
                .id("format")
                .name("Format Results")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'找到商品: ' + #input.query"))
                .inputMapping(Map.of("q", "query"))
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("product-search-wf")
                .name("Product Search Workflow")
                .startNodeId("build_search")
                .nodes(List.of(buildSearch, format))
                .edges(List.of(Edge.builder().id("e1").fromNodeId("build_search").toNodeId("format").build()))
                .build();

            workflowRepository.save(workflow);

            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("product-search-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getNodeExecutions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Scenario 2: Order Creation with Condition (下单创建)")
    class OrderCreationScenario {

        @Test
        @DisplayName("Should create order when stock is sufficient")
        void should_create_order_when_stock_sufficient() {
            // Step 1: Check stock - returns sufficient=true
            Node checkStock = Node.builder()
                .id("check_stock")
                .name("Check Stock")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "sufficient", "expression", "true", "nextNodeId", "condition"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("sufficient", "stockSufficient"))
                .build();

            // Step 2: Condition branch
            List<Map<String, String>> conditions = new ArrayList<>();
            conditions.add(Map.of("expression", "#input.stockSufficient == true", "nextNodeId", "create_order"));
            conditions.add(Map.of("expression", "#input.stockSufficient == false", "nextNodeId", "insufficient"));

            Node conditionNode = Node.builder()
                .id("condition")
                .name("Stock Condition")
                .type(NodeType.CONDITION)
                .config(Map.of("conditions", conditions, "defaultNextNodeId", "insufficient"))
                .inputMapping(Map.of("stock", "stockSufficient"))
                .outputMapping(Map.of())
                .build();

            // Step 3: Create order (when condition is true)
            Node createOrder = Node.builder()
                .id("create_order")
                .name("Create Order")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "orderResult", "expression", "'ORD202604010001'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("orderResult", "output.orderResult"))
                .build();

            // Step 4: Insufficient stock (when condition is false)
            Node insufficient = Node.builder()
                .id("insufficient")
                .name("Insufficient Stock")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "errorResult", "expression", "'库存不足'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("errorResult", "output.errorResult"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("order-creation-wf")
                .name("Order Creation Workflow")
                .startNodeId("check_stock")
                .nodes(List.of(checkStock, conditionNode, createOrder, insufficient))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("check_stock").toNodeId("condition").build(),
                    Edge.builder().id("e2").fromNodeId("condition").toNodeId("create_order").build(),
                    Edge.builder().id("e3").fromNodeId("condition").toNodeId("insufficient").build()
                ))
                .build();

            workflowRepository.save(workflow);

            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("order-creation-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalContext().get("output.orderResult").map(Object::toString))
                .contains("ORD202604010001");
        }

        @Test
        @DisplayName("Should return error when stock is insufficient")
        void should_return_error_when_stock_insufficient() {
            // Step 1: Check stock - returns sufficient=false
            Node checkStock = Node.builder()
                .id("check_stock")
                .name("Check Stock")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "sufficient", "expression", "false", "nextNodeId", "condition"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("sufficient", "stockSufficient"))
                .build();

            // Step 2: Condition branch
            List<Map<String, String>> conditions = new ArrayList<>();
            conditions.add(Map.of("expression", "#input.stockSufficient == true", "nextNodeId", "create_order"));
            conditions.add(Map.of("expression", "#input.stockSufficient == false", "nextNodeId", "insufficient"));

            Node conditionNode = Node.builder()
                .id("condition")
                .name("Stock Condition")
                .type(NodeType.CONDITION)
                .config(Map.of("conditions", conditions, "defaultNextNodeId", "insufficient"))
                .inputMapping(Map.of("stock", "stockSufficient"))
                .outputMapping(Map.of())
                .build();

            // Step 3: Create order (not reached)
            Node createOrder = Node.builder()
                .id("create_order")
                .name("Create Order")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "orderResult", "expression", "'ORD202604010001'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("orderResult", "output.orderResult"))
                .build();

            // Step 4: Insufficient stock
            Node insufficient = Node.builder()
                .id("insufficient")
                .name("Insufficient Stock")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "errorResult", "expression", "'库存不足'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("errorResult", "output.errorResult"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("order-creation-wf-2")
                .name("Order Creation Workflow 2")
                .startNodeId("check_stock")
                .nodes(List.of(checkStock, conditionNode, createOrder, insufficient))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("check_stock").toNodeId("condition").build(),
                    Edge.builder().id("e2").fromNodeId("condition").toNodeId("create_order").build(),
                    Edge.builder().id("e3").fromNodeId("condition").toNodeId("insufficient").build()
                ))
                .build();

            workflowRepository.save(workflow);

            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("order-creation-wf-2", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalContext().get("output.errorResult").map(Object::toString))
                .contains("库存不足");
        }
    }

    @Nested
    @DisplayName("Scenario 3: Complete Purchase Flow (完整购物流程)")
    class CompletePurchaseFlowScenario {

        @Test
        @DisplayName("Should execute complete purchase flow")
        void should_execute_complete_purchase_flow() {
            // Step 1: Intent recognition
            Node step1 = Node.builder()
                .id("step1_intent")
                .name("Intent Recognition")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "intent", "expression", "'search'", "nextNodeId", "step2_search"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("intent", "intent"))
                .build();

            // Step 2: Search products
            Node step2 = Node.builder()
                .id("step2_search")
                .name("Search Products")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "products", "expression", "10", "nextNodeId", "step3_inventory"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("products", "productCount"))
                .build();

            // Step 3: Check inventory
            Node step3 = Node.builder()
                .id("step3_inventory")
                .name("Check Inventory")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "inventory", "expression", "true", "nextNodeId", "step4_price"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("inventory", "inStock"))
                .build();

            // Step 4: Calculate price
            Node step4 = Node.builder()
                .id("step4_price")
                .name("Calculate Price")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "price", "expression", "699", "nextNodeId", "step5_confirm"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("price", "totalPrice"))
                .build();

            // Step 5: Confirm order
            Node step5 = Node.builder()
                .id("step5_confirm")
                .name("User Confirmation")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "confirmed", "expression", "true", "nextNodeId", "step6_create"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("confirmed", "confirmed"))
                .build();

            // Step 6: Create order
            Node step6 = Node.builder()
                .id("step6_create")
                .name("Create Order")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "order", "expression", "'ORD202604010001'", "nextNodeId", "step7_notify"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("order", "orderId"))
                .build();

            // Step 7: Send notification
            Node step7 = Node.builder()
                .id("step7_notify")
                .name("Send Notification")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "notification", "expression", "'短信已发送'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("notification", "output.notification"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("complete-purchase-wf")
                .name("Complete Purchase Workflow")
                .startNodeId("step1_intent")
                .nodes(List.of(step1, step2, step3, step4, step5, step6, step7))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("step1_intent").toNodeId("step2_search").build(),
                    Edge.builder().id("e2").fromNodeId("step2_search").toNodeId("step3_inventory").build(),
                    Edge.builder().id("e3").fromNodeId("step3_inventory").toNodeId("step4_price").build(),
                    Edge.builder().id("e4").fromNodeId("step4_price").toNodeId("step5_confirm").build(),
                    Edge.builder().id("e5").fromNodeId("step5_confirm").toNodeId("step6_create").build(),
                    Edge.builder().id("e6").fromNodeId("step6_create").toNodeId("step7_notify").build()
                ))
                .build();

            workflowRepository.save(workflow);

            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("complete-purchase-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getNodeExecutions()).hasSize(7);
        }
    }

    @Nested
    @DisplayName("Scenario 4: Smart Customer Service Routing (智能客服路由)")
    class SmartCustomerServiceRoutingScenario {

        @Test
        @DisplayName("Should route to correct department based on LLM intent")
        void should_route_based_on_intent() {
            // DATA_PROCESSING node simulates LLM returning intent=search
            Node intentNode = Node.builder()
                .id("intent")
                .name("Intent Recognition")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of(
                    "outputKey", "intent",
                    "expression", "'search'",
                    "nextNodeId", "route"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("intent", "intent"))
                .build();

            // BRANCH node routes based on LLM output
            Node branchNode = Node.builder()
                .id("route")
                .name("Route")
                .type(NodeType.BRANCH)
                .config(Map.of("branches", List.of(
                    Map.of("name", "search", "expression", "#input.intent == 'search'", "nextNodeId", "search_node"),
                    Map.of("name", "recommend", "expression", "#input.intent == 'recommend'", "nextNodeId", "recommend_node"),
                    Map.of("name", "inquiry", "expression", "#input.intent == 'inquiry'", "nextNodeId", "inquiry_node"),
                    Map.of("name", "order", "expression", "#input.intent == 'order'", "nextNodeId", "order_node")
                )))
                .inputMapping(Map.of("intent", "intent"))
                .outputMapping(Map.of())
                .build();

            // Route target nodes
            Node searchNode = Node.builder()
                .id("search_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'搜索结果'", "nextNodeId", "format"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "routeResult"))
                .build();

            Node recommendNode = Node.builder()
                .id("recommend_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'推荐结果'", "nextNodeId", "format"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "routeResult"))
                .build();

            Node inquiryNode = Node.builder()
                .id("inquiry_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'咨询结果'", "nextNodeId", "format"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "routeResult"))
                .build();

            Node orderNode = Node.builder()
                .id("order_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'订单结果'", "nextNodeId", "format"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "routeResult"))
                .build();

            Node formatNode = Node.builder()
                .id("format")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "final", "expression", "'路由到: ' + #input.intent"))
                .inputMapping(Map.of("intent", "intent"))
                .outputMapping(Map.of("final", "output.final"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("customer-service-wf")
                .name("Customer Service Routing")
                .startNodeId("intent")
                .nodes(List.of(intentNode, branchNode, searchNode, recommendNode, inquiryNode, orderNode, formatNode))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("intent").toNodeId("route").build(),
                    Edge.builder().id("e2").fromNodeId("route").toNodeId("search_node").build(),
                    Edge.builder().id("e3").fromNodeId("route").toNodeId("recommend_node").build(),
                    Edge.builder().id("e4").fromNodeId("route").toNodeId("inquiry_node").build(),
                    Edge.builder().id("e5").fromNodeId("route").toNodeId("order_node").build(),
                    Edge.builder().id("e6").fromNodeId("search_node").toNodeId("format").build(),
                    Edge.builder().id("e7").fromNodeId("recommend_node").toNodeId("format").build(),
                    Edge.builder().id("e8").fromNodeId("inquiry_node").toNodeId("format").build(),
                    Edge.builder().id("e9").fromNodeId("order_node").toNodeId("format").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of("query", "想买Nike鞋"));
            WorkflowExecutionResult result = workflowExecutor.execute("customer-service-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getNodeExecutions()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 5: Parallel Recommendation (并行推荐)")
    class ParallelRecommendationScenario {

        @Test
        @DisplayName("Should fetch user profile and behaviors in parallel")
        void should_fetch_parallel() {
            Map<String, Object> parallelConfig = new HashMap<>();
            parallelConfig.put("branches", List.of(
                Map.of("name", "profile", "nodeIds", List.of("profile_node")),
                Map.of("name", "behaviors", "nodeIds", List.of("behaviors_node"))
            ));
            parallelConfig.put("strategy", "AND");
            parallelConfig.put("nextNodeId", "merge");

            Node parallelNode = Node.builder()
                .id("parallel")
                .name("Parallel Fetch")
                .type(NodeType.PARALLEL)
                .config(parallelConfig)
                .inputMapping(Map.of())
                .outputMapping(Map.of("branchResults", "parallelResults"))
                .build();

            Map<String, Object> profileConfig = new HashMap<>();
            profileConfig.put("outputKey", "profile");
            profileConfig.put("expression", "'用户画像数据'");
            profileConfig.put("nextNodeId", null);

            Node profileNode = Node.builder()
                .id("profile_node")
                .type(NodeType.DATA_PROCESSING)
                .config(profileConfig)
                .inputMapping(Map.of())
                .outputMapping(Map.of("profile", "profile"))
                .build();

            Map<String, Object> behaviorsConfig = new HashMap<>();
            behaviorsConfig.put("outputKey", "behaviors");
            behaviorsConfig.put("expression", "'行为历史数据'");
            behaviorsConfig.put("nextNodeId", null);

            Node behaviorsNode = Node.builder()
                .id("behaviors_node")
                .type(NodeType.DATA_PROCESSING)
                .config(behaviorsConfig)
                .inputMapping(Map.of())
                .outputMapping(Map.of("behaviors", "behaviors"))
                .build();

            Node mergeNode = Node.builder()
                .id("merge")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "merged", "expression", "'已合并: ' + #input.profile + ' + ' + #input.behaviors"))
                .inputMapping(Map.of("profile", "profile", "behaviors", "behaviors"))
                .outputMapping(Map.of("merged", "output.merged"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("parallel-recommend-wf")
                .name("Parallel Recommend")
                .startNodeId("parallel")
                .nodes(List.of(parallelNode, profileNode, behaviorsNode, mergeNode))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parallel").toNodeId("profile_node").build(),
                    Edge.builder().id("e2").fromNodeId("parallel").toNodeId("behaviors_node").build(),
                    Edge.builder().id("e3").fromNodeId("profile_node").toNodeId("merge").build(),
                    Edge.builder().id("e4").fromNodeId("behaviors_node").toNodeId("merge").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("parallel-recommend-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 6: Foreach Batch Processing (FOREACH批量处理)")
    class ForeachBatchProcessingScenario {

        @Test
        @DisplayName("Should iterate over product list with foreach")
        void should_iterate_products() {
            Node foreachNode = Node.builder()
                .id("foreach")
                .name("Process Products")
                .type(NodeType.FOREACH)
                .config(Map.of(
                    "collection", "#input.products",
                    "variableName", "product",
                    "maxIterations", 100,
                    "nextNodeId", "aggregate"
                ))
                .inputMapping(Map.of("products", "input.products"))
                .outputMapping(Map.of("results", "processed"))
                .build();

            Node aggregateNode = Node.builder()
                .id("aggregate")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "count", "expression", "#input.totalIterations"))
                .inputMapping(Map.of("totalIterations", "processed.totalIterations"))
                .outputMapping(Map.of("count", "output.count"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("foreach-products-wf")
                .name("Foreach Products")
                .startNodeId("foreach")
                .nodes(List.of(foreachNode, aggregateNode))
                .edges(List.of(Edge.builder().id("e1").fromNodeId("foreach").toNodeId("aggregate").build()))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of("products", List.of("产品A", "产品B", "产品C")));
            WorkflowExecutionResult result = workflowExecutor.execute("foreach-products-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getNodeExecutions()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 7: Try-Catch Exception Handling (TRY_CATCH异常处理)")
    class TryCatchExceptionHandlingScenario {

        @Test
        @DisplayName("Should catch exception in try-catch")
        void should_catch_exception() {
            Node tryCatchNode = Node.builder()
                .id("try_catch")
                .name("Try Create Order")
                .type(NodeType.TRY_CATCH)
                .config(Map.of(
                    "tryNodeId", "risky_operation",
                    "catchNodeId", "handle_error",
                    "nextNodeId", "notify"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("caught", "error.caught"))
                .build();

            Node riskyOp = Node.builder()
                .id("risky_operation")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "1/0"))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            Node handleError = Node.builder()
                .id("handle_error")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "errorMsg", "expression", "'已捕获异常'", "nextNodeId", "notify"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("errorMsg", "errorMsg"))
                .build();

            Node notify = Node.builder()
                .id("notify")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("try-catch-wf")
                .name("Try Catch Workflow")
                .startNodeId("try_catch")
                .nodes(List.of(tryCatchNode, riskyOp, handleError, notify))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("try_catch").toNodeId("risky_operation").build(),
                    Edge.builder().id("e2").fromNodeId("risky_operation").toNodeId("handle_error").build(),
                    Edge.builder().id("e3").fromNodeId("handle_error").toNodeId("notify").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("try-catch-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 8: Retry Mechanism (RETRY重试机制)")
    class RetryMechanismScenario {

        @Test
        @DisplayName("Should retry failed operation")
        void should_retry_operation() {
            Node retryNode = Node.builder()
                .id("retry")
                .name("Retry Operation")
                .type(NodeType.RETRY)
                .config(Map.of(
                    "maxAttempts", 3,
                    "retryNodeId", "unstable_operation",
                    "nextNodeId", "success"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("attempts", "retry.attempts"))
                .build();

            Map<String, Object> unstableConfig = new HashMap<>();
            unstableConfig.put("outputKey", "result");
            unstableConfig.put("expression", "'操作成功'");
            unstableConfig.put("nextNodeId", null);

            Node unstableOp = Node.builder()
                .id("unstable_operation")
                .type(NodeType.DATA_PROCESSING)
                .config(unstableConfig)
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            Node success = Node.builder()
                .id("success")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("retry-wf")
                .name("Retry Workflow")
                .startNodeId("retry")
                .nodes(List.of(retryNode, unstableOp, success))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("retry").toNodeId("unstable_operation").build(),
                    Edge.builder().id("e2").fromNodeId("unstable_operation").toNodeId("success").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("retry-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }
}
