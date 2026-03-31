package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Context;
import org.springframework.stereotype.Service;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

@Service
public class RuleEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    public boolean evaluate(String expression, Context context) {
        EvaluationContext evalContext = new StandardEvaluationContext();
        Map<String, Object> data = context.toMap();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            ((StandardEvaluationContext) evalContext).setVariable(entry.getKey(), entry.getValue());
        }
        String spelExpression = expression.replace("input.", "#");
        return Boolean.TRUE.equals(parser.parseExpression(spelExpression).getValue(evalContext));
    }
}
