package com.lookgraph.service;

import com.lookgraph.common.enums.Language;
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
import com.lookgraph.scanner.FileWatcher;
import com.lookgraph.scanner.IncrementalScanner;
import com.lookgraph.scanner.ProjectScanner;
import com.lookgraph.vector.VectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

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

    @Transactional
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
                .collect(java.util.stream.Collectors.toMap(
                        c -> c.getClassId(), c -> c, (a, b) -> a))
                .values();
        var uniqueMethods = result.methods().stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> m.getMethodId(), m -> m, (a, b) -> a))
                .values();

        classRepository.saveAll(uniqueClasses);
        methodRepository.saveAll(uniqueMethods);
        moduleRepository.saveAll(result.modules());

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
                .collect(java.util.stream.Collectors.toMap(c -> c.getClassId(), c -> c, (a, b) -> a))
                .values();
        var uniqueMethods = result.methods().stream()
                .collect(java.util.stream.Collectors.toMap(m -> m.getMethodId(), m -> m, (a, b) -> a))
                .values();

        classRepository.saveAll(uniqueClasses);
        methodRepository.saveAll(uniqueMethods);
        vectorIndexService.upsert(projectId, result);

        project.setUpdateTime(Instant.now());
        projectRepository.save(project);
    }

    public List<ProjectNode> listProjects() {
        return projectRepository.findAll();
    }
}
