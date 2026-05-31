package com.lookgraph.controller;

import com.lookgraph.domain.node.ProjectNode;
import com.lookgraph.dto.request.ProjectInitRequest;
import com.lookgraph.dto.response.ProjectInitResponse;
import com.lookgraph.dto.response.ProjectSummary;
import com.lookgraph.dto.response.Result;
import com.lookgraph.service.ProjectService;
import com.lookgraph.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@Tag(name = "项目管理")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SummaryService summaryService;

    @PostMapping("/init")
    @Operation(summary = "初始化项目地图")
    public Result<ProjectInitResponse> init(@RequestBody @Valid ProjectInitRequest req) {
        return Result.ok(projectService.init(req));
    }

    @PostMapping("/update")
    @Operation(summary = "触发增量更新")
    public Result<Void> update(@RequestParam String projectId) {
        projectService.triggerIncrementalUpdate(projectId);
        return Result.ok();
    }

    @GetMapping("/summary")
    @Operation(summary = "获取项目摘要")
    public Result<ProjectSummary> summary(@RequestParam String projectId) {
        return Result.ok(summaryService.get(projectId));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有项目")
    public Result<List<ProjectNode>> list() {
        return Result.ok(projectService.listProjects());
    }
}
