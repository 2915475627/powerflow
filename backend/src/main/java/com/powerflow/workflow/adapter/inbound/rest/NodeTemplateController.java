package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.NodeTemplate;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.NodeTemplateRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/node-templates")
public class NodeTemplateController {

    private final NodeTemplateRepository repository;

    public NodeTemplateController(NodeTemplateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<NodeTemplate> listAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public NodeTemplate getById(@PathVariable String id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Template not found: " + id));
    }

    @GetMapping("/type/{nodeType}")
    public List<NodeTemplate> listByNodeType(@PathVariable NodeType nodeType) {
        return repository.findActiveByNodeType(nodeType);
    }

    @PostMapping
    public NodeTemplate create(@RequestBody NodeTemplate template) {
        return repository.save(template);
    }

    @PutMapping("/{id}")
    public NodeTemplate update(@PathVariable String id, @RequestBody NodeTemplate template) {
        return repository.save(template);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repository.delete(id);
    }
}
