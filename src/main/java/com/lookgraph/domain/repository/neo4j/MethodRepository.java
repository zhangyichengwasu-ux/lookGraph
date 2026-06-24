package com.lookgraph.domain.repository.neo4j;

import com.lookgraph.domain.node.MethodNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MethodRepository extends Neo4jRepository<MethodNode, String> {

    List<MethodNode> findByClassId(String classId);

    @Query("MATCH (m:Method {projectId: $projectId}) RETURN count(m)")
    Long countByProjectId(@Param("projectId") String projectId);

    @Query("MATCH (m:Method) WHERE m.filePath = $filePath DETACH DELETE m")
    void deleteAllByFilePath(@Param("filePath") String filePath);

    @Query("""
            MATCH (m:Method {methodId: $methodId})-[:CALLS*1..3]->(callee:Method)
            RETURN DISTINCT callee
            """)
    List<MethodNode> findDownstreamCallees(@Param("methodId") String methodId);

    @Query("""
            MATCH (m:Method {methodId: $methodId})<-[:CALLS*1..3]-(caller:Method)
            RETURN DISTINCT caller
            """)
    List<MethodNode> findUpstreamCallers(@Param("methodId") String methodId);

    @Query("""
            MATCH path = (m:Method {methodId: $methodId})-[:CALLS*1..5]->(callee:Method)
            RETURN path
            """)
    List<MethodNode> findCallChain(@Param("methodId") String methodId);
}
