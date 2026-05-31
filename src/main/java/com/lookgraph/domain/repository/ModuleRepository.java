package com.lookgraph.domain.repository;

import com.lookgraph.domain.node.ModuleNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends Neo4jRepository<ModuleNode, String> {

    List<ModuleNode> findByProjectId(String projectId);

    @Query("MATCH (m:Module {projectId: $projectId})-[:DEPENDS_ON]->(dep:Module) RETURN m, dep")
    List<ModuleNode> findModulesWithDependencies(@Param("projectId") String projectId);

    void deleteAllByProjectId(String projectId);
}
