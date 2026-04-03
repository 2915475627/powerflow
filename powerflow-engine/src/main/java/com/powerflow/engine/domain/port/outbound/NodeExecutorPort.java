package com.powerflow.engine.domain.port.outbound;

import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;

/**
 * Port for executing individual nodes.
 * Each node type has a corresponding implementation.
 * Implementations are discovered via SPI (ServiceLoader).
 */
public interface NodeExecutorPort {
    /**
     * The node type this executor handles.
     */
    NodeType supportedType();

    /**
     * Execute a node.
     * @param node the node to execute
     * @param context the current execution context
     * @return the result of execution
     */
    NodeResult execute(Node node, Context context);
}
