package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.*;

public interface NodeExecutorPort {
    NodeResult execute(Node node, Context context);
}
