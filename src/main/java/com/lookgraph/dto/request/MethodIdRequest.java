package com.lookgraph.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MethodIdRequest(
        @NotBlank String methodId
) {
}
