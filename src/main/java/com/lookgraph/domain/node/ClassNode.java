package com.lookgraph.domain.node;

import com.lookgraph.common.enums.ClassType;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Node("Class")
public class ClassNode {

    @Id
    private String classId;

    private String name;
    private String filePath;
    private String comment;
    private String moduleId;
    private String projectId;
    private ClassType type;

    @Relationship(type = "EXTENDS", direction = Relationship.Direction.OUTGOING)
    private ClassNode parent;

    @Relationship(type = "IMPLEMENTS", direction = Relationship.Direction.OUTGOING)
    private Set<ClassNode> interfaces = new HashSet<>();

    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private Set<ClassNode> dependsOn = new HashSet<>();

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.INCOMING)
    private List<MethodNode> methods = new ArrayList<>();
}
