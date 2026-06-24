package com.lookgraph.controller;

import com.lookgraph.common.enums.EntityType;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.dto.request.ImpactRequest;
import com.lookgraph.dto.request.MethodIdRequest;
import com.lookgraph.dto.response.*;
import com.lookgraph.service.ImpactAnalysisService;
import com.lookgraph.service.StructureQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/structure")
@Tag(name = "结构查询")
@RequiredArgsConstructor
public class StructureController {

    private final StructureQueryService structureService;
    private final ImpactAnalysisService impactService;

    @GetMapping("/class/{classId}/relation")
    @Operation(summary = "查询类的关联关系")
    public Result<ClassRelationView> classRelation(@PathVariable String classId) {
        return Result.ok(structureService.classRelations(classId));
    }

    @PostMapping("/method/callchain")
    @Operation(summary = "查询方法调用链路")
    public Result<CallChainView> callChain(@RequestBody MethodIdRequest request) {
        return Result.ok(structureService.callChain(request.methodId()));
    }

    @GetMapping("/method/{methodId:.+}/callchain")
    @Operation(summary = "查询方法调用链路 (已废弃，使用 POST 版本)")
    @Deprecated
    public Result<CallChainView> callChainDeprecated(@PathVariable String methodId) {
        return Result.ok(structureService.callChain(methodId));
    }

    @GetMapping("/module/{moduleId}/classes")
    @Operation(summary = "查询模块下所有类")
    public Result<List<ClassNode>> classesInModule(@PathVariable String moduleId) {
        return Result.ok(structureService.classesInModule(moduleId));
    }

    @GetMapping("/class/{classId}/methods")
    @Operation(summary = "查询类下所有方法")
    public Result<List<MethodNode>> methodsInClass(@PathVariable String classId) {
        return Result.ok(structureService.methodsInClass(classId));
    }

    @PostMapping("/impact")
    @Operation(summary = "查询代码修改影响范围")
    public Result<ImpactReport> impact(@RequestBody ImpactRequest request) {
        return Result.ok(impactService.analyze(request.entityType(), request.entityId()));
    }

    @GetMapping("/impact/{entityType}/{entityId:.+}")
    @Operation(summary = "查询代码修改影响范围 (已废弃，使用 POST 版本)")
    @Deprecated
    public Result<ImpactReport> impactDeprecated(@PathVariable EntityType entityType,
                                       @PathVariable String entityId) {
        return Result.ok(impactService.analyze(entityType, entityId));
    }
}
