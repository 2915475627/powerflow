package com.powerflow.engine.domain.port.outbound;

/**
 * Interface for node executors that need to receive the callback.
 * Implement this interface to get a NodeExecutorCallback injected.
 */
public interface NodeExecutorCallbackSetter {

    /**
     * Set the callback to be used for sub-execution.
     */
    void setCallback(NodeExecutorCallback callback);
}
