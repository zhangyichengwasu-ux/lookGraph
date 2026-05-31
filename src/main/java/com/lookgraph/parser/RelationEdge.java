package com.lookgraph.parser;

import com.lookgraph.common.enums.RelationType;

public record RelationEdge(
        String fromId,
        String toId,
        RelationType type
) {}
