package com.powerflow.workflow.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

class ContextTest {

    @Test
    void should_store_and_retrieve_values() {
        Context ctx = new Context();
        ctx.set("amount", 1000);
        ctx.set("userId", "user-123");

        assertThat(ctx.get("amount")).contains(1000);
        assertThat(ctx.get("userId")).contains("user-123");
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
        assertThat(ctx.get("key")).contains("value");
    }
}
