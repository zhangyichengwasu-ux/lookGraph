package com.lookgraph.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SemanticSearchRequest(
        @NotBlank String query,
        String module,
        String businessTag,
        @Min(1) @Max(50) int topK
) {
    public SemanticSearchRequest {
        if (topK == 0) topK = 10;
    }
}
