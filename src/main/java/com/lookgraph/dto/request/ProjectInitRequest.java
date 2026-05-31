package com.lookgraph.dto.request;

import com.lookgraph.common.enums.Language;
import jakarta.validation.constraints.NotBlank;

public record ProjectInitRequest(
        @NotBlank String name,
        @NotBlank String path,
        Language language
) {}
