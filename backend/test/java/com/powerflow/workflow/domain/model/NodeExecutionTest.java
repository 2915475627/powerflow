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
        assertThat(execution.getError()).contains("Division by zero");
    }
}
