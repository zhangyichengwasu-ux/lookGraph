package com.lookgraph.domain.node;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Node("Project")
public class ProjectNode {

    @Id
    private String projectId;

    private String name;
    private String path;
    private String techStack;
    private Instant createTime;
    private Instant updateTime;

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.INCOMING)
    private List<ModuleNode> modules = new ArrayList<>();
}
