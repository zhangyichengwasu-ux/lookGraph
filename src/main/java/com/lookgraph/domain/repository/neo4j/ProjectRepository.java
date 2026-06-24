package com.lookgraph.domain.repository.neo4j;

import com.lookgraph.domain.node.ProjectNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends Neo4jRepository<ProjectNode, String> {

    Optional<ProjectNode> findByPath(String path);

    Optional<ProjectNode> findByName(String name);
}
