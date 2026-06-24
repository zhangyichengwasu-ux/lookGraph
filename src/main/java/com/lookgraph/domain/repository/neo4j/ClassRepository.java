package com.lookgraph.domain.repository.neo4j;

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
    Long countByProjectId(@Param("projectId") String projectId);

    @Query("MATCH (c:Class {filePath: $filePath}) DETACH DELETE c")
    void deleteAllByFilePath(@Param("filePath") String filePath);

    @Query("MATCH (c:Class {classId: $classId})-[:EXTENDS]->(parent:Class) RETURN parent")
    ClassNode findParent(@Param("classId") String classId);

    @Query("MATCH (c:Class {classId: $classId})-[:IMPLEMENTS]->(iface:Class) RETURN iface")
    List<ClassNode> findInterfaces(@Param("classId") String classId);

    @Query("MATCH (c:Class {classId: $classId})-[:DEPENDS_ON]->(dep:Class) RETURN dep")
    List<ClassNode> findDependencies(@Param("classId") String classId);

    @Query("MATCH (c:Class {classId: $classId})<-[:DEPENDS_ON]-(depBy:Class) RETURN depBy")
    List<ClassNode> findDependedBy(@Param("classId") String classId);
}
