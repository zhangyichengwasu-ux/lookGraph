package com.lookgraph.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SemanticHistoryResponse {

    private String entityId;
    private String entityType;
    private SemanticResponse current;
    private List<SemanticResponse> history;
}
