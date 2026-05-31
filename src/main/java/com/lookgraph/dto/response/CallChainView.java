package com.lookgraph.dto.response;

import com.lookgraph.domain.node.MethodNode;

import java.util.List;

public record CallChainView(
        MethodNode target,
        List<MethodNode> upstream,
        List<MethodNode> downstream
) {}
