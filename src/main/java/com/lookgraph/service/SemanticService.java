package com.lookgraph.service;

import com.lookgraph.common.exception.BizException;
import com.lookgraph.domain.entity.SemanticHistory;
import com.lookgraph.domain.repository.jpa.SemanticHistoryRepository;
import com.lookgraph.dto.request.SemanticUpdateRequest;
import com.lookgraph.dto.response.SemanticHistoryResponse;
import com.lookgraph.dto.response.SemanticResponse;
import com.lookgraph.vector.VectorIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticService {

    private final SemanticHistoryRepository semanticHistoryRepository;
    private final VectorIndexService vectorIndexService;

    public List<SemanticResponse> getByGitCommitHash(String gitCommitHash) {
        List<SemanticHistory> histories = semanticHistoryRepository
                .findByGitCommitHashOrderByCreateTimeDesc(gitCommitHash);
        return histories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SemanticResponse> getByNeo4jNodeId(String neo4jNodeId) {
        List<SemanticHistory> histories = semanticHistoryRepository
                .findByNeo4jNodeIdOrderByCreateTimeDesc(neo4jNodeId);
        return histories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SemanticHistoryResponse getHistoryByClass(String packageName, String className) {
        List<SemanticHistory> histories = semanticHistoryRepository
                .findByPackageNameAndClassNameOrderByCreateTimeDesc(packageName, className);

        SemanticHistoryResponse response = new SemanticHistoryResponse();
        response.setEntityId(packageName + "." + className);
        response.setEntityType("CLASS");

        List<SemanticResponse> historyList = histories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        response.setCurrent(historyList.isEmpty() ? null : historyList.get(0));
        response.setHistory(historyList);

        return response;
    }

    public SemanticHistoryResponse getHistoryByMethod(String packageName, String className, String methodName) {
        List<SemanticHistory> histories = semanticHistoryRepository
                .findByPackageNameAndClassNameAndMethodNameOrderByCreateTimeDesc(packageName, className, methodName);

        SemanticHistoryResponse response = new SemanticHistoryResponse();
        response.setEntityId(packageName + "." + className + "." + methodName);
        response.setEntityType("METHOD");

        List<SemanticResponse> historyList = histories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        response.setCurrent(historyList.isEmpty() ? null : historyList.get(0));
        response.setHistory(historyList);

        return response;
    }

    @Transactional("jpaTransactionManager")
    public SemanticResponse createSemantic(SemanticHistory entity) {
        entity.setCreateTime(LocalDateTime.now());
        SemanticHistory saved = semanticHistoryRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional("jpaTransactionManager")
    public void indexSemanticById(Long id, String projectId) {
        SemanticHistory semantic = semanticHistoryRepository.findById(id)
                .orElseThrow(() -> new BizException("语义注释不存在: " + id));

        String entityId = buildEntityId(semantic);
        vectorIndexService.indexSingleSemantic(
                entityId,
                semantic.getType().name(),
                semantic.getContent(),
                projectId
        );
    }

    private SemanticResponse toResponse(SemanticHistory entity) {
        SemanticResponse response = new SemanticResponse();
        response.setHistoryId(entity.getId().toString());
        response.setEntityId(buildEntityId(entity));
        response.setEntityType(entity.getType());
        response.setContent(entity.getContent());
        response.setVersion(null);
        response.setIsActive(true);
        response.setModifiedBy(entity.getModifiedBy());
        response.setModifyReason(entity.getModifyReason());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }

    private String buildEntityId(SemanticHistory entity) {
        StringBuilder sb = new StringBuilder(entity.getPackageName()).append(".").append(entity.getClassName());
        if (entity.getMethodName() != null) {
            sb.append(".").append(entity.getMethodName());
        }
        if (entity.getFieldName() != null) {
            sb.append(".").append(entity.getFieldName());
        }
        return sb.toString();
    }
}
