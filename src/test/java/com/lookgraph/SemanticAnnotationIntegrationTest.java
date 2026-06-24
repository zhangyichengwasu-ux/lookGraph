package com.lookgraph;

import com.lookgraph.common.enums.AnnotationType;
import com.lookgraph.common.enums.ModifySource;
import com.lookgraph.domain.entity.SemanticHistory;
import com.lookgraph.domain.repository.jpa.SemanticHistoryRepository;
import com.lookgraph.dto.response.SemanticHistoryResponse;
import com.lookgraph.dto.response.SemanticResponse;
import com.lookgraph.service.SemanticService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SemanticAnnotationIntegrationTest {

    @Autowired
    private SemanticService semanticService;

    @Autowired
    private SemanticHistoryRepository semanticHistoryRepository;

    private static final String TEST_PACKAGE = "com.example.test";
    private static final String TEST_CLASS = "TestUserService";
    private static final String TEST_METHOD = "login";
    private static final String TEST_GIT_HASH_V1 = "abc123def456";
    private static final String TEST_GIT_HASH_V2 = "def789ghi012";

    @BeforeEach
    void setup() {
        // 清理测试数据
        semanticHistoryRepository.deleteAll();
    }

    @Test
    void testCreateClassAnnotation_AI() {
        // 创建 AI 生成的类注释
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName(TEST_PACKAGE);
        entity.setClassName(TEST_CLASS);
        entity.setType(AnnotationType.CLASS);
        entity.setContent("用户服务类，处理用户注册、登录等业务");
        entity.setGitCommitHash(TEST_GIT_HASH_V1);
        entity.setModifiedBy(ModifySource.AI);
        entity.setModifyReason("根据类名和方法签名自动生成");
        entity.setCreateTime(LocalDateTime.now());

        SemanticResponse response = semanticService.createSemantic(entity);

        assertNotNull(response);
        assertNotNull(response.getHistoryId());
        assertEquals(TEST_PACKAGE + "." + TEST_CLASS, response.getEntityId());
        assertEquals(AnnotationType.CLASS, response.getEntityType());
        assertEquals("用户服务类，处理用户注册、登录等业务", response.getContent());
        assertEquals(ModifySource.AI, response.getModifiedBy());

        System.out.println("[语义注释] AI 生成类注释创建成功: " + response.getHistoryId());
    }

    @Test
    void testCreateMethodAnnotation_Human() {
        // 创建人工编写的方法注释
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName(TEST_PACKAGE);
        entity.setClassName(TEST_CLASS);
        entity.setMethodName(TEST_METHOD);
        entity.setType(AnnotationType.METHOD);
        entity.setNeo4jNodeId("method-node-uuid-123");
        entity.setContent("用户登录方法，验证用户名密码，返回 JWT token");
        entity.setGitCommitHash(TEST_GIT_HASH_V1);
        entity.setModifiedBy(ModifySource.HUMAN);
        entity.setModifyReason("补充返回值说明");
        entity.setCreateTime(LocalDateTime.now());

        SemanticResponse response = semanticService.createSemantic(entity);

        assertNotNull(response);
        assertTrue(response.getEntityId().contains(TEST_METHOD), "entityId 应包含方法名");
        assertEquals(AnnotationType.METHOD, response.getEntityType());
        assertEquals(ModifySource.HUMAN, response.getModifiedBy());

        System.out.println("[语义注释] 人工方法注释创建成功: " + response.getHistoryId());
    }

    @Test
    void testAnnotationVersionHistory() {
        // 创建第一个版本（AI 生成）
        SemanticHistory v1 = new SemanticHistory();
        v1.setPackageName(TEST_PACKAGE);
        v1.setClassName(TEST_CLASS);
        v1.setMethodName(TEST_METHOD);
        v1.setType(AnnotationType.METHOD);
        v1.setContent("用户登录方法");
        v1.setGitCommitHash(TEST_GIT_HASH_V1);
        v1.setModifiedBy(ModifySource.AI);
        v1.setModifyReason("自动生成");
        v1.setCreateTime(LocalDateTime.now());

        SemanticResponse response1 = semanticService.createSemantic(v1);
        assertNotNull(response1.getHistoryId());

        // 等待一秒确保时间戳不同
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 创建第二个版本（人工修正）
        SemanticHistory v2 = new SemanticHistory();
        v2.setPackageName(TEST_PACKAGE);
        v2.setClassName(TEST_CLASS);
        v2.setMethodName(TEST_METHOD);
        v2.setType(AnnotationType.METHOD);
        v2.setContent("用户登录方法，验证用户名密码，成功后返回 JWT token");
        v2.setGitCommitHash(TEST_GIT_HASH_V2);
        v2.setModifiedBy(ModifySource.HUMAN);
        v2.setModifyReason("补充完整业务逻辑说明");
        v2.setCreateTime(LocalDateTime.now());

        SemanticResponse response2 = semanticService.createSemantic(v2);
        assertNotNull(response2.getHistoryId());
        assertNotEquals(response1.getHistoryId(), response2.getHistoryId());

        // 查询方法的注释历史
        SemanticHistoryResponse history = semanticService.getHistoryByMethod(TEST_PACKAGE, TEST_CLASS, TEST_METHOD);

        assertNotNull(history);
        assertEquals(String.format("%s.%s.%s", TEST_PACKAGE, TEST_CLASS, TEST_METHOD), history.getEntityId());
        assertEquals("METHOD", history.getEntityType());

        // 验证当前版本是最新的（人工修正版本）
        assertNotNull(history.getCurrent());
        assertEquals(response2.getHistoryId(), history.getCurrent().getHistoryId());
        assertEquals("用户登录方法，验证用户名密码，成功后返回 JWT token", history.getCurrent().getContent());
        assertEquals(ModifySource.HUMAN, history.getCurrent().getModifiedBy());

        // 验证历史版本列表包含两个版本
        assertNotNull(history.getHistory());
        assertEquals(2, history.getHistory().size());

        // 验证历史版本按时间倒序排列（最新的在前）
        SemanticResponse latest = history.getHistory().get(0);
        SemanticResponse oldest = history.getHistory().get(1);
        assertEquals(ModifySource.HUMAN, latest.getModifiedBy());
        assertEquals(ModifySource.AI, oldest.getModifiedBy());

        System.out.println("[语义注释] 版本历史测试成功:");
        System.out.println("  - V1 (AI): " + oldest.getContent());
        System.out.println("  - V2 (HUMAN): " + latest.getContent());
    }

    @Test
    void testQueryByGitCommitHash() {
        // 创建同一个 Git 版本的多个注释
        SemanticHistory classAnnotation = new SemanticHistory();
        classAnnotation.setPackageName(TEST_PACKAGE);
        classAnnotation.setClassName(TEST_CLASS);
        classAnnotation.setType(AnnotationType.CLASS);
        classAnnotation.setContent("用户服务类");
        classAnnotation.setGitCommitHash(TEST_GIT_HASH_V1);
        classAnnotation.setModifiedBy(ModifySource.AI);
        classAnnotation.setCreateTime(LocalDateTime.now());

        SemanticHistory methodAnnotation = new SemanticHistory();
        methodAnnotation.setPackageName(TEST_PACKAGE);
        methodAnnotation.setClassName(TEST_CLASS);
        methodAnnotation.setMethodName(TEST_METHOD);
        methodAnnotation.setType(AnnotationType.METHOD);
        methodAnnotation.setContent("登录方法");
        methodAnnotation.setGitCommitHash(TEST_GIT_HASH_V1);
        methodAnnotation.setModifiedBy(ModifySource.AI);
        methodAnnotation.setCreateTime(LocalDateTime.now());

        semanticService.createSemantic(classAnnotation);
        semanticService.createSemantic(methodAnnotation);

        // 根据 Git Hash 查询
        List<SemanticResponse> results = semanticService.getByGitCommitHash(TEST_GIT_HASH_V1);

        assertNotNull(results);
        assertTrue(results.size() >= 2, "应至少包含 2 个注释");

        // 验证查询结果包含类和方法注释
        boolean hasClassAnnotation = results.stream()
                .anyMatch(r -> r.getEntityType() == AnnotationType.CLASS && r.getEntityId().endsWith(TEST_CLASS));
        boolean hasMethodAnnotation = results.stream()
                .anyMatch(r -> r.getEntityType() == AnnotationType.METHOD && r.getEntityId().contains(TEST_METHOD));

        assertTrue(hasClassAnnotation, "应包含类注释");
        assertTrue(hasMethodAnnotation, "应包含方法注释");

        System.out.println("[语义注释] Git Hash 查询成功，找到 " + results.size() + " 条注释");
    }

    @Test
    void testQueryByNeo4jNodeId() {
        String nodeId = "test-node-uuid-789";

        // 创建关联 Neo4j 节点的注释
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName(TEST_PACKAGE);
        entity.setClassName(TEST_CLASS);
        entity.setMethodName(TEST_METHOD);
        entity.setType(AnnotationType.METHOD);
        entity.setNeo4jNodeId(nodeId);
        entity.setContent("关联图谱节点的方法注释");
        entity.setGitCommitHash(TEST_GIT_HASH_V1);
        entity.setModifiedBy(ModifySource.AI);
        entity.setCreateTime(LocalDateTime.now());

        semanticService.createSemantic(entity);

        // 根据 Neo4j 节点 ID 查询
        List<SemanticResponse> results = semanticService.getByNeo4jNodeId(nodeId);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "应找到关联的注释");
        // Note: SemanticResponse 没有直接的 neo4jNodeId 字段，通过数据库查询验证即可

        System.out.println("[语义注释] Neo4j 节点 ID 查询成功，找到 " + results.size() + " 条注释");
    }

    @Test
    void testQueryClassHistory() {
        // 创建类注释
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName(TEST_PACKAGE);
        entity.setClassName(TEST_CLASS);
        entity.setType(AnnotationType.CLASS);
        entity.setContent("用户服务类，处理用户相关业务逻辑");
        entity.setGitCommitHash(TEST_GIT_HASH_V1);
        entity.setModifiedBy(ModifySource.AI);
        entity.setCreateTime(LocalDateTime.now());

        semanticService.createSemantic(entity);

        // 查询类的注释历史
        SemanticHistoryResponse history = semanticService.getHistoryByClass(TEST_PACKAGE, TEST_CLASS);

        assertNotNull(history);
        assertEquals(String.format("%s.%s", TEST_PACKAGE, TEST_CLASS), history.getEntityId());
        assertEquals("CLASS", history.getEntityType());
        assertNotNull(history.getCurrent());
        assertEquals("用户服务类，处理用户相关业务逻辑", history.getCurrent().getContent());

        System.out.println("[语义注释] 类注释历史查询成功");
    }

    @Test
    void testEmptyHistory() {
        // 查询不存在的类的注释历史
        SemanticHistoryResponse history = semanticService.getHistoryByClass("com.nonexistent", "NonExistentClass");

        assertNotNull(history);
        assertEquals("com.nonexistent.NonExistentClass", history.getEntityId());
        assertNull(history.getCurrent(), "不存在的类应返回空的当前版本");
        assertNotNull(history.getHistory());
        assertTrue(history.getHistory().isEmpty(), "历史列表应为空");

        System.out.println("[语义注释] 空历史查询测试通过");
    }

    @Test
    void testMultipleAnnotationTypes() {
        // 测试不同类型的注释
        String[] types = {
                AnnotationType.CLASS.name(),
                AnnotationType.INTERFACE.name(),
                AnnotationType.ENUM.name(),
                AnnotationType.METHOD.name(),
                AnnotationType.FIELD.name()
        };

        for (String typeStr : types) {
            SemanticHistory entity = new SemanticHistory();
            entity.setPackageName(TEST_PACKAGE);
            entity.setClassName("Test" + typeStr + "Class");
            entity.setType(AnnotationType.valueOf(typeStr));
            entity.setContent("测试 " + typeStr + " 类型注释");
            entity.setGitCommitHash(TEST_GIT_HASH_V1);
            entity.setModifiedBy(ModifySource.AI);
            entity.setCreateTime(LocalDateTime.now());

            if (typeStr.equals(AnnotationType.METHOD.name())) {
                entity.setMethodName("testMethod");
            } else if (typeStr.equals(AnnotationType.FIELD.name())) {
                entity.setFieldName("testField");
            }

            SemanticResponse response = semanticService.createSemantic(entity);
            assertNotNull(response);
            assertEquals(AnnotationType.valueOf(typeStr), response.getEntityType());
        }

        System.out.println("[语义注释] 多种注释类型测试通过，共测试 " + types.length + " 种类型");
    }
}
