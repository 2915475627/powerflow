package com.powerflow.engine.engine;

import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorCallback;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import com.powerflow.engine.exception.WorkflowExecutionException;

import java.util.EnumMap;
import java.util.Map;

/**
 * Node dispatcher routes node execution to the appropriate NodeExecutorPort.
 * Executors are registered via SPI or manual registration.
 */
public class NodeDispatcher {

    private final Map<NodeType, NodeExecutorPort> executors = new EnumMap<>(NodeType.class);
    private NodeExecutorCallback callback;

    /**
     * Register a node executor for a specific node type.
     */
    public void register(NodeType type, NodeExecutorPort executor) {
        executors.put(type, executor);
    }

    /**
     * Set the callback for nodes that need sub-execution.
     */
    public void setCallback(NodeExecutorCallback callback) {
        this.callback = callback;
        // Propagate callback to executors that need it
        for (NodeExecutorPort executor : executors.values()) {
            if (executor instanceof NodeExecutorCallbackSetter setter) {
                setter.setCallback(callback);
            }
        }
    }

    /**
     * Dispatch execution to the appropriate executor.
     */
    public NodeResult dispatch(Node node, Context context) {
        NodeExecutorPort executor = executors.get(node.getType());
        if (executor == null) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(com.powerflow.engine.domain.enums.ExecutionStatus.FAILED)
                .error("No executor registered for node type: " + node.getType())
                .build();
        }

        try {
            return executor.execute(node, context);
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(com.powerflow.engine.domain.enums.ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    public boolean hasExecutor(NodeType type) {
        return executors.containsKey(type);
    }

    /**
     * Marker interface for executors that accept a callback.
     */
    public interface NodeExecutorCallbackSetter {
        void setCallback(NodeExecutorCallback callback);
    }
}
