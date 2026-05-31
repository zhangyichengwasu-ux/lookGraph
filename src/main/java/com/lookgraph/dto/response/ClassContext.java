package com.lookgraph.dto.response;

import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;

import java.util.List;

public record ClassContext(
        ClassNode classNode,
        String sourceSnippet,
        List<MethodNode> methods,
        ClassNode parent,
        List<ClassNode> interfaces,
        String comment
) {}
