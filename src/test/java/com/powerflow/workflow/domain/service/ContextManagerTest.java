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

        assertThat(ctx.get("output.discountedAmount")).contains(900);
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