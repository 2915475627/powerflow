package com.powerflow.engine.domain.enums;

public enum NodeType {
    DATA_PROCESSING,
    CONDITION,
    HTTP_REQUEST,
    LLM_CALL,
    PROMPT_TEMPLATE,
    PARALLEL,
    FOREACH,
    BRANCH,
    SUBWORKFLOW,
    TRY_CATCH,
    RETRY,
    START,
    END,
    JOIN
}
