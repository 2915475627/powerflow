package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.NodeResult;

public interface NodeExecutorCallback {
    NodeResult execute(Node node, Context context);
}
