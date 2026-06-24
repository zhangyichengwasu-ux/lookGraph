package com.lookgraph.service;

import com.lookgraph.domain.node.ProjectNode;
import com.lookgraph.domain.repository.neo4j.ClassRepository;
import com.lookgraph.domain.repository.neo4j.MethodRepository;
import com.lookgraph.domain.repository.neo4j.ModuleRepository;
import com.lookgraph.domain.repository.neo4j.ProjectRepository;
import com.lookgraph.dto.response.ProjectSummary;
import com.lookgraph.vector.VectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ProjectRepository projectRepository;
    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;
    private final ModuleRepository moduleRepository;
    private final VectorIndexService vectorIndexService;

    public ProjectSummary get(String projectId) {
        ProjectNode project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目不存在: " + projectId));

        long classCount = classRepository.countByProjectId(projectId);
        long methodCount = methodRepository.countByProjectId(projectId);
        long moduleCount = moduleRepository.countByProjectId(projectId);
        List<String> moduleNames = moduleRepository.findModuleNamesByProjectId(projectId);

        String overview = buildOverview(project, classCount, moduleCount);

        return new ProjectSummary(
                project.getProjectId(),
                project.getName(),
                project.getTechStack(),
                (int) classCount,
                (int) methodCount,
                (int) moduleCount,
                moduleNames,
                overview);
    }

    public void generateAndIndex(String projectId) {
        ProjectSummary summary = get(projectId);
        String text = buildSummaryText(summary);
        vectorIndexService.indexProjectOverview(projectId, text);
        log.info("项目摘要已生成并索引: {}", projectId);
    }

    private String buildOverview(ProjectNode project, long classCount, long moduleCount) {
        return String.format("项目 %s，技术栈 %s，包含 %d 个类，%d 个模块。",
                project.getName(), project.getTechStack(), classCount, moduleCount);
    }

    private String buildSummaryText(ProjectSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("项目名称: ").append(summary.name()).append("\n");
        sb.append("技术栈: ").append(summary.techStack()).append("\n");
        sb.append("类数量: ").append(summary.classCount()).append("\n");
        sb.append("方法数量: ").append(summary.methodCount()).append("\n");
        sb.append("模块数量: ").append(summary.moduleCount()).append("\n");
        sb.append("模块列表: ").append(String.join(", ", summary.modules())).append("\n");
        sb.append("概述: ").append(summary.overview());
        return sb.toString();
    }
}
