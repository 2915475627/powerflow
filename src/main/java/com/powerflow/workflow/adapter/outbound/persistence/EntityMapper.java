package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.*;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public Workflow toDomain(WorkflowEntity entity) {
        if (entity == null) return null;

        List<Node> nodes = entity.getNodes().stream()
            .map(this::toNodeDomain)
            .collect(Collectors.toList());

        List<Edge> edges = entity.getEdges().stream()
            .map(this::toEdgeDomain)
            .collect(Collectors.toList());

        return Workflow.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .startNodeId(entity.getStartNodeId())
            .nodes(nodes)
            .edges(edges)
            .build();
    }

    public WorkflowEntity toEntity(Workflow workflow) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.getId());
        entity.setName(workflow.getName());
        entity.setDescription(workflow.getDescription());
        entity.setStartNodeId(workflow.getStartNodeId());
        return entity;
    }

    public Node toNodeDomain(NodeEntity entity) {
        return Node.builder()
            .id(entity.getId())
            .name(entity.getName())
            .type(NodeType.valueOf(entity.getType()))
            .config(entity.getConfig())
            .inputMapping(entity.getInputMapping())
            .outputMapping(entity.getOutputMapping())
            .build();
    }

    public NodeEntity toNodeEntity(Node node, String workflowId) {
        NodeEntity entity = new NodeEntity();
        entity.setId(node.getId());
        entity.setName(node.getName());
        entity.setType(node.getType().name());
        entity.setConfig(node.getConfig());
        entity.setInputMapping(node.getInputMapping());
        entity.setOutputMapping(node.getOutputMapping());
        entity.setWorkflowId(workflowId);
        return entity;
    }

    public Edge toEdgeDomain(EdgeEntity entity) {
        return Edge.builder()
            .id(entity.getId())
            .fromNodeId(entity.getFromNodeId())
            .toNodeId(entity.getToNodeId())
            .condition(entity.getCondition())
            .build();
    }

    public EdgeEntity toEdgeEntity(Edge edge, String workflowId) {
        EdgeEntity entity = new EdgeEntity();
        entity.setId(edge.getId());
        entity.setFromNodeId(edge.getFromNodeId());
        entity.setToNodeId(edge.getToNodeId());
        entity.setCondition(edge.getCondition());
        entity.setWorkflowId(workflowId);
        return entity;
    }

    public NodeExecution toNodeExecutionDomain(NodeExecutionEntity entity) {
        return NodeExecution.builder()
            .id(entity.getId())
            .workflowExecutionId(entity.getWorkflowExecutionId())
            .nodeId(entity.getNodeId())
            .status(ExecutionStatus.valueOf(entity.getStatus()))
            .input(entity.getInput())
            .output(entity.getOutput())
            .error(entity.getError())
            .durationMs(entity.getDurationMs())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .build();
    }

    public NodeExecutionEntity toNodeExecutionEntity(NodeExecution execution) {
        NodeExecutionEntity entity = new NodeExecutionEntity();
        entity.setId(execution.getId());
        entity.setWorkflowExecutionId(execution.getWorkflowExecutionId());
        entity.setNodeId(execution.getNodeId());
        entity.setStatus(execution.getStatus().name());
        entity.setInput(execution.getInput());
        entity.setOutput(execution.getOutput());
        entity.setError(execution.getError().orElse(null));
        entity.setDurationMs(execution.getDurationMs());
        entity.setStartTime(execution.getStartTime());
        entity.setEndTime(execution.getEndTime());
        return entity;
    }
}
