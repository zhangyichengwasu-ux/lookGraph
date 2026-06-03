package com.lookgraph.service;

import com.lookgraph.common.enums.Language;
import com.lookgraph.common.enums.RelationType;
import com.lookgraph.common.exception.BizException;
import com.lookgraph.common.util.IdUtil;
import com.lookgraph.domain.node.ProjectNode;
import com.lookgraph.domain.repository.ClassRepository;
import com.lookgraph.domain.repository.MethodRepository;
import com.lookgraph.domain.repository.ModuleRepository;
import com.lookgraph.domain.repository.ProjectRepository;
import com.lookgraph.dto.request.ProjectInitRequest;
import com.lookgraph.dto.response.ProjectInitResponse;
import com.lookgraph.parser.ParseResult;
import com.lookgraph.parser.RelationEdge;
import com.lookgraph.scanner.FileWatcher;
import com.lookgraph.scanner.IncrementalScanner;
import com.lookgraph.scanner.ProjectScanner;
import com.lookgraph.vector.VectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectScanner projectScanner;
    private final ProjectRepository projectRepository;
    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;
    private final ModuleRepository moduleRepository;
    private final VectorIndexService vectorIndexService;
    private final SummaryService summaryService;
    private final FileWatcher fileWatcher;
    private final Neo4jClient neo4jClient;

    private static final int RELATION_BATCH_SIZE = 5000;
    private static final int NODE_BATCH_SIZE = 1000;

    public ProjectInitResponse init(ProjectInitRequest req) {
        Path root = Path.of(req.path());
        if (!Files.isDirectory(root)) {
            throw new BizException("路径不存在或不是目录: " + req.path());
        }

        projectRepository.findByPath(req.path()).ifPresent(p -> {
            throw new BizException("项目已存在: " + p.getName());
        });

        Language lang = req.language() != null ? req.language() : projectScanner.detectLanguage(root);
        ParseResult result = projectScanner.fullScan(root, lang);

        ProjectNode project = new ProjectNode();
        project.setProjectId(IdUtil.simpleUuid());
        project.setName(req.name());
        project.setPath(req.path());
        project.setTechStack(lang.name());
        project.setCreateTime(Instant.now());
        project.setUpdateTime(Instant.now());
        projectRepository.save(project);

        result.classes().forEach(c -> c.setProjectId(project.getProjectId()));
        result.methods().forEach(m -> m.setProjectId(project.getProjectId()));
        result.modules().forEach(m -> m.setProjectId(project.getProjectId()));

        // 按 ID 去重（内部类可能在多文件中出现同名）
        var uniqueClasses = result.classes().stream()
                .collect(Collectors.toMap(c -> c.getClassId(), c -> c, (a, b) -> a))
                .values();
        var uniqueMethods = result.methods().stream()
                .collect(Collectors.toMap(m -> m.getMethodId(), m -> m, (a, b) -> a))
                .values();

        saveBatch(classRepository, List.copyOf(uniqueClasses));
        saveBatch(methodRepository, List.copyOf(uniqueMethods));
        moduleRepository.saveAll(result.modules());

        writeRelations(result.relations(), project.getProjectId());

        vectorIndexService.initCollections();
        vectorIndexService.indexEntities(project.getProjectId(), result);
        summaryService.generateAndIndex(project.getProjectId());

        fileWatcher.watch(project.getProjectId(), root);

        return new ProjectInitResponse(
                project.getProjectId(),
                result.classes().size(),
                result.methods().size(),
                result.modules().size());
    }

    public void triggerIncrementalUpdate(String projectId) {
        ProjectNode project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在: " + projectId));
        Path root = Path.of(project.getPath());
        Language lang = Language.valueOf(project.getTechStack());
        ParseResult result = projectScanner.fullScan(root, lang);

        result.classes().forEach(c -> c.setProjectId(projectId));
        result.methods().forEach(m -> m.setProjectId(projectId));

        var uniqueClasses = result.classes().stream()
                .collect(Collectors.toMap(c -> c.getClassId(), c -> c, (a, b) -> a))
                .values();
        var uniqueMethods = result.methods().stream()
                .collect(Collectors.toMap(m -> m.getMethodId(), m -> m, (a, b) -> a))
                .values();

        saveBatch(classRepository, List.copyOf(uniqueClasses));
        saveBatch(methodRepository, List.copyOf(uniqueMethods));

        writeRelations(result.relations(), projectId);

        vectorIndexService.initCollections();
        vectorIndexService.upsert(projectId, result);

        project.setUpdateTime(Instant.now());
        projectRepository.save(project);
    }

    public List<ProjectNode> listProjects() {
        return projectRepository.findAll();
    }

    private void writeRelations(List<RelationEdge> relations, String projectId) {
        Map<RelationType, List<RelationEdge>> grouped = relations.stream()
                .collect(Collectors.groupingBy(RelationEdge::type));

        runBatch(grouped.get(RelationType.EXTENDS),
                "UNWIND $rows AS row " +
                "MATCH (a:Class {classId: row.from}), (b:Class {classId: row.to}) " +
                "MERGE (a)-[:EXTENDS]->(b)");
        runBatch(grouped.get(RelationType.IMPLEMENTS),
                "UNWIND $rows AS row " +
                "MATCH (a:Class {classId: row.from}), (b:Class {classId: row.to}) " +
                "MERGE (a)-[:IMPLEMENTS]->(b)");
        runBatch(grouped.get(RelationType.DEPENDS_ON),
                "UNWIND $rows AS row " +
                "MATCH (a:Class {classId: row.from}), (b:Class {classId: row.to}) " +
                "MERGE (a)-[:DEPENDS_ON]->(b)");

        // CALLS: 前缀匹配全表扫描太慢，暂跳过，后续用索引优化
        // List<RelationEdge> calls = grouped.get(RelationType.CALLS);
        // ...

        // BELONGS_TO: 从 method.classId 直接推，不依赖 RelationEdge
        neo4jClient.query(
                "MATCH (m:Method {projectId: $pid}) " +
                "MATCH (c:Class {classId: m.classId}) " +
                "MERGE (c)<-[:BELONGS_TO]-(m)")
                .bind(projectId).to("pid")
                .run();

        log.info("关系写入完成: EXTENDS={} IMPLEMENTS={} DEPENDS_ON={} CALLS={}",
                size(grouped.get(RelationType.EXTENDS)),
                size(grouped.get(RelationType.IMPLEMENTS)),
                size(grouped.get(RelationType.DEPENDS_ON)),
                size(grouped.get(RelationType.CALLS)));
    }

    private void runBatch(List<RelationEdge> edges, String cypher) {
        if (edges == null || edges.isEmpty()) return;
        List<Map<String, Object>> rows = edges.stream()
                .map(e -> Map.<String, Object>of("from", e.fromId(), "to", e.toId()))
                .toList();
        runRows(rows, cypher);
    }

    private void runRows(List<Map<String, Object>> rows, String cypher) {
        for (int i = 0; i < rows.size(); i += RELATION_BATCH_SIZE) {
            int end = Math.min(i + RELATION_BATCH_SIZE, rows.size());
            List<Map<String, Object>> batch = rows.subList(i, end);
            neo4jClient.query(cypher).bind(batch).to("rows").run();
        }
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private <T> void saveBatch(org.springframework.data.neo4j.repository.Neo4jRepository<T, String> repo, List<T> entities) {
        for (int i = 0; i < entities.size(); i += NODE_BATCH_SIZE) {
            int end = Math.min(i + NODE_BATCH_SIZE, entities.size());
            repo.saveAll(entities.subList(i, end));
            if (i % 5000 == 0 && i > 0) {
                log.info("已保存 {} / {} 个节点", i, entities.size());
            }
        }
        log.info("节点保存完成: {} 个", entities.size());
    }
}
