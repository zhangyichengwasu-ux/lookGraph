package com.lookgraph.domain.repository.jpa;

import com.lookgraph.domain.entity.SemanticHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemanticHistoryRepository extends JpaRepository<SemanticHistory, Long> {

    List<SemanticHistory> findByGitCommitHashOrderByCreateTimeDesc(String gitCommitHash);

    List<SemanticHistory> findByPackageNameAndClassNameOrderByCreateTimeDesc(String packageName, String className);

    List<SemanticHistory> findByPackageNameAndClassNameAndMethodNameOrderByCreateTimeDesc(
            String packageName, String className, String methodName);

    List<SemanticHistory> findByPackageNameAndClassNameAndFieldNameOrderByCreateTimeDesc(
            String packageName, String className, String fieldName);

    List<SemanticHistory> findByNeo4jNodeIdOrderByCreateTimeDesc(String neo4jNodeId);
}
