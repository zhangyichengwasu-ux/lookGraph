package com.lookgraph.controller;

import com.lookgraph.dto.response.ClassContext;
import com.lookgraph.dto.response.MethodContext;
import com.lookgraph.dto.response.Result;
import com.lookgraph.service.ContextAssemblyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/context")
@Tag(name = "上下文获取")
@RequiredArgsConstructor
public class ContextController {

    private final ContextAssemblyService contextService;

    @GetMapping("/method/{methodId}")
    @Operation(summary = "获取方法精简上下文")
    public Result<MethodContext> methodContext(@PathVariable String methodId) {
        return Result.ok(contextService.methodContext(methodId));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "获取类精简上下文")
    public Result<ClassContext> classContext(@PathVariable String classId) {
        return Result.ok(contextService.classContext(classId));
    }
}
