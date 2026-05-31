package com.lookgraph.dto.response;

import com.lookgraph.domain.node.ClassNode;

import java.util.List;

public record ClassRelationView(
        ClassNode classNode,
        ClassNode parent,
        List<ClassNode> interfaces,
        List<ClassNode> dependencies,
        List<ClassNode> dependedBy
) {
    public static ClassRelationView empty(ClassNode node) {
        return new ClassRelationView(node, null, List.of(), List.of(), List.of());
    }
}
