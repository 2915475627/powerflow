package com.powerflow.engine.spi;

import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import com.powerflow.engine.engine.NodeDispatcher;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Loads node executors via Java SPI (ServiceLoader).
 * External jars can register executors by providing
 * META-INF/services/com.powerflow.engine.domain.port.outbound.NodeExecutorPort
 */
public class NodeExecutorLoader {

    /**
     * Load all NodeExecutorPort implementations from the classpath
     * and register them with the dispatcher.
     */
    public static void loadNodesInto(NodeDispatcher dispatcher) {
        ServiceLoader<NodeExecutorPort> loader = ServiceLoader.load(NodeExecutorPort.class);
        Iterator<NodeExecutorPort> iterator = loader.iterator();

        while (iterator.hasNext()) {
            try {
                NodeExecutorPort executor = iterator.next();
                dispatcher.register(executor.supportedType(), executor);
                System.out.println("[PowerFlow] Registered node executor: " + executor.supportedType());
            } catch (Exception e) {
                System.err.println("[PowerFlow] Failed to load node executor: " + e.getMessage());
            }
        }
    }
}
