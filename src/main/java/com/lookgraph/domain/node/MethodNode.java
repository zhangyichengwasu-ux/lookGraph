package com.lookgraph.domain.node;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@Node("Method")
public class MethodNode {

    @Id
    private String methodId;

    private String name;
    private String params;
    private String returnType;
    private String comment;
    private String classId;
    private String projectId;
    private int startLine;
    private int endLine;

    @Relationship(type = "CALLS", direction = Relationship.Direction.OUTGOING)
    private Set<MethodNode> calls = new HashSet<>();
}
