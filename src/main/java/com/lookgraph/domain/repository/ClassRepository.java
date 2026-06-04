package com.lookgraph.domain.repository;

import com.lookgraph.domain.node.ClassNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends Neo4jRepository<ClassNode, String> {

    List<ClassNode> findByModuleId(String moduleId);

    List<ClassNode> findByProjectId(String projectId);

    @Query("MATCH (c:Class {projectId: $projectId}) RETURN count(c)")
    long countByProjectId(@Param("projectId") String projectId);

    @Query("MATCH (c:Class {filePath: $filePath}) DETACH DELETE c")
    void deleteAllByFilePath(@Param("filePath") String filePath);

    @Query("""
            MATCH (c:Class {classId: $classId})
            OPTIONAL MATCH (c)-[:EXTENDS]->(parent:Class)
            OPTIONAL MATCH (c)-[:IMPLEMENTS]->(iface:Class)
            OPTIONAL MATCH (c)-[:DEPENDS_ON]->(dep:Class)
            OPTIONAL MATCH (c)<-[:DEPENDS_ON]-(depBy:Class)
            RETURN c, parent, collect(DISTINCT iface) AS interfaces,
                   collect(DISTINCT dep) AS dependencies,
                   collect(DISTINCT depBy) AS dependedBy
            """)
    ClassRelationProjection findClassRelations(@Param("classId") String classId);

    interface ClassRelationProjection {
        ClassNode getC();
        ClassNode getParent();
        List<ClassNode> getInterfaces();
        List<ClassNode> getDependencies();
        List<ClassNode> getDependedBy();
    }
}
