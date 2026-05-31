package com.lookgraph.controller;

import com.lookgraph.dto.request.SemanticSearchRequest;
import com.lookgraph.dto.response.Result;
import com.lookgraph.dto.response.SemanticHit;
import com.lookgraph.service.SemanticSearchService;
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

    @PostMapping("/search")
    @Operation(summary = "语义检索代码")
    public Result<List<SemanticHit>> search(@RequestBody @Valid SemanticSearchRequest req) {
        return Result.ok(semanticSearchService.search(req));
    }
}
