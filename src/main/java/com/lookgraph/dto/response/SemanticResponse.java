package com.lookgraph.dto.response;

import com.lookgraph.common.enums.AnnotationType;
import com.lookgraph.common.enums.ModifySource;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SemanticResponse {

    private String historyId;
    private String entityId;
    private AnnotationType entityType;
    private String content;
    private Integer version;
    private Boolean isActive;
    private ModifySource modifiedBy;
    private String modifyReason;
    private LocalDateTime createTime;
}
