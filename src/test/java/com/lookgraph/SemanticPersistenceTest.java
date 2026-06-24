package com.lookgraph;

import com.lookgraph.common.enums.AnnotationType;
import com.lookgraph.common.enums.ModifySource;
import com.lookgraph.domain.entity.SemanticHistory;
import com.lookgraph.domain.repository.jpa.SemanticHistoryRepository;
import com.lookgraph.dto.response.SemanticResponse;
import com.lookgraph.service.SemanticService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据持久化验证测试
 * 注意：此测试不会清理数据，用于验证数据是否真正持久化到数据库
 */
@SpringBootTest
class SemanticPersistenceTest {

    @Autowired
    private SemanticService semanticService;

    @Autowired
    private SemanticHistoryRepository repository;

    @Test
    void testDataPersistence() {
        // 记录插入前的数据量
        long countBefore = repository.count();
        System.out.println("[持久化测试] 插入前记录数: " + countBefore);

        // 插入测试数据
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName("com.persistence.test");
        entity.setClassName("PersistenceTestClass");
        entity.setType(AnnotationType.CLASS);
        entity.setContent("持久化测试注释 - 此数据将保留在数据库中");
        entity.setGitCommitHash("persist_test_" + System.currentTimeMillis());
        entity.setModifiedBy(ModifySource.AI);
        entity.setModifyReason("验证数据持久化功能");
        entity.setCreateTime(LocalDateTime.now());

        SemanticResponse response = semanticService.createSemantic(entity);
        assertNotNull(response);
        assertNotNull(response.getHistoryId());

        // 验证数据已插入
        long countAfter = repository.count();
        assertEquals(countBefore + 1, countAfter, "应该新增 1 条记录");

        System.out.println("[持久化测试] 插入后记录数: " + countAfter);
        System.out.println("[持久化测试] 新增记录 ID: " + response.getHistoryId());
        System.out.println("[持久化测试] ✓ 数据已成功持久化到数据库");
        System.out.println("[持久化测试] 提示: 此测试不清理数据，可在数据库中验证");
    }
}
