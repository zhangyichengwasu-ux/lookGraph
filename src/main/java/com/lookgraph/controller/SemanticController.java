package com.lookgraph.controller;

import com.lookgraph.domain.entity.SemanticHistory;
import com.lookgraph.dto.request.SemanticSearchRequest;
import com.lookgraph.dto.response.Result;
import com.lookgraph.dto.response.SemanticHit;
import com.lookgraph.dto.response.SemanticHistoryResponse;
import com.lookgraph.dto.response.SemanticResponse;
import com.lookgraph.service.SemanticSearchService;
import com.lookgraph.service.SemanticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/semantic")
@Tag(name = "语义检索")
@RequiredArgsConstructor
public class SemanticController {

    private final SemanticSearchService semanticSearchService;
    private final SemanticService semanticService;

    @PostMapping("/search")
    @Operation(summary = "语义检索代码")
    public Result<List<SemanticHit>> search(@RequestBody @Valid SemanticSearchRequest req) {
        return Result.ok(semanticSearchService.search(req));
    }

    @GetMapping("/git/{gitCommitHash}")
    @Operation(summary = "根据 Git Hash 查询所有业务注释")
    public Result<List<SemanticResponse>> getByGitHash(@PathVariable String gitCommitHash) {
        return Result.ok(semanticService.getByGitCommitHash(gitCommitHash));
    }

    @GetMapping("/node/{neo4jNodeId}")
    @Operation(summary = "根据 Neo4j 节点 ID 查询业务注释历史")
    public Result<List<SemanticResponse>> getByNodeId(@PathVariable String neo4jNodeId) {
        return Result.ok(semanticService.getByNeo4jNodeId(neo4jNodeId));
    }

    @GetMapping("/class")
    @Operation(summary = "查询类的业务注释历史")
    public Result<SemanticHistoryResponse> getClassHistory(
            @RequestParam String packageName,
            @RequestParam String className) {
        return Result.ok(semanticService.getHistoryByClass(packageName, className));
    }

    @GetMapping("/method")
    @Operation(summary = "查询方法的业务注释历史")
    public Result<SemanticHistoryResponse> getMethodHistory(
            @RequestParam String packageName,
            @RequestParam String className,
            @RequestParam String methodName) {
        return Result.ok(semanticService.getHistoryByMethod(packageName, className, methodName));
    }

    @PostMapping
    @Operation(summary = "创建业务注释")
    public Result<SemanticResponse> createSemantic(@RequestBody @Valid SemanticHistory entity) {
        return Result.ok(semanticService.createSemantic(entity));
    }

    @PostMapping("/{id}/index")
    @Operation(summary = "触发单条注释的向量索引")
    public Result<Void> indexSemantic(@PathVariable Long id, @RequestParam String projectId) {
        semanticService.indexSemanticById(id, projectId);
        return Result.ok();
    }
}
