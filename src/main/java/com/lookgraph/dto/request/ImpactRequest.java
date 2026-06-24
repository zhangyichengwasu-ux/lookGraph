package com.lookgraph.dto.request;

import com.lookgraph.common.enums.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImpactRequest(
        @NotNull EntityType entityType,
        @NotBlank String entityId
) {
}
