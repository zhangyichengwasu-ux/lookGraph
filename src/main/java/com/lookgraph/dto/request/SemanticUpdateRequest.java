package com.lookgraph.dto.request;

import com.lookgraph.common.enums.ModifySource;
import lombok.Data;

@Data
public class SemanticUpdateRequest {

    private String content;
    private ModifySource modifiedBy;
    private String modifyReason;
}
