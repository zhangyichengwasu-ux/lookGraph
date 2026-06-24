package com.lookgraph.domain.node;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Data
@Node("SemanticHistory")
public class SemanticHistoryNode {

    @Id
    private String historyId;

    private String entityId;
    private String entityType;

    private String content;
    private Integer version;
    private Boolean isActive;

    private String modifiedBy;
    private String modifyReason;
    private LocalDateTime createTime;
}
