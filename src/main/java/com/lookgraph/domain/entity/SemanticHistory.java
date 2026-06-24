package com.lookgraph.domain.entity;

import com.lookgraph.common.enums.AnnotationType;
import com.lookgraph.common.enums.ModifySource;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "semantic_history", indexes = {
        @Index(name = "idx_git_hash", columnList = "gitCommitHash"),
        @Index(name = "idx_package_class", columnList = "packageName,className"),
        @Index(name = "idx_neo4j_node", columnList = "neo4jNodeId")
})
public class SemanticHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String packageName;

    @Column(nullable = false)
    private String className;

    private String methodName;

    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AnnotationType type;

    private String neo4jNodeId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 64)
    private String gitCommitHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModifySource modifiedBy;

    @Column(columnDefinition = "TEXT")
    private String modifyReason;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
