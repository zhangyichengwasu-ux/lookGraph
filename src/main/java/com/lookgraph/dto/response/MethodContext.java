package com.lookgraph.dto.response;

import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;

import java.util.List;

public record MethodContext(
        MethodNode target,
        String sourceSnippet,
        ClassNode ownerClass,
        List<MethodNode> upstream,
        List<MethodNode> downstream,
        String comment
) {}
