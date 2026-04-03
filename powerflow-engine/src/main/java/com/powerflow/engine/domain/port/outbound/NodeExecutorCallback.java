package com.powerflow.engine.domain.port.outbound;

import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;

/**
 * Callback interface for nodes that need to delegate to the workflow engine.
 * Used by nodes like SUBWORKFLOW, TRY_CATCH, RETRY that need to execute
 * sub-graphs or handle complex control flow.
 */
public interface NodeExecutorCallback {
    /**
     * Execute a node via the callback.
     * This allows nodes to call back into the engine for sub-execution.
     */
    NodeResult execute(Node node, Context context);
}
