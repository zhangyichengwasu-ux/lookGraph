package com.lookgraph.dto.response;

import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.node.ModuleNode;

import java.util.List;

public record ImpactReport(
        String entityId,
        String entityType,
        List<MethodNode> affectedMethods,
        List<ClassNode> affectedClasses,
        List<ModuleNode> affectedModules
) {}
