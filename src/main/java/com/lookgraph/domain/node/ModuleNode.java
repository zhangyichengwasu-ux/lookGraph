package com.lookgraph.domain.node;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@Node("Module")
public class ModuleNode {

    @Id
    private String moduleId;

    private String name;
    private String businessTag;
    private String projectId;

    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private Set<ModuleNode> dependsOn = new HashSet<>();
}
