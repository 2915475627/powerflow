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
