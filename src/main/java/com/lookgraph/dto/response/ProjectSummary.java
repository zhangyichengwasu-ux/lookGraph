package com.lookgraph.dto.response;

import java.util.List;

public record ProjectSummary(
        String projectId,
        String name,
        String techStack,
        int classCount,
        int methodCount,
        int moduleCount,
        List<String> modules,
        String overview
) {}
