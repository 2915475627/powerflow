package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.NodeTemplate;
import com.powerflow.workflow.domain.model.enums.NodeType;
import java.util.List;
import java.util.Optional;

public interface NodeTemplateRepository {
    Optional<NodeTemplate> findById(String id);
    List<NodeTemplate> findByNodeType(NodeType nodeType);
    List<NodeTemplate> findAll();
    List<NodeTemplate> findActiveByNodeType(NodeType nodeType);
    NodeTemplate save(NodeTemplate template);
    void delete(String id);
}
