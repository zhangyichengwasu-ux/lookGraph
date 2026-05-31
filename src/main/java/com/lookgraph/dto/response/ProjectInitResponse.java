package com.lookgraph.dto.response;

public record ProjectInitResponse(
        String projectId,
        int classCount,
        int methodCount,
        int moduleCount
) {}
