package com.lookgraph;

import com.lookgraph.common.enums.AnnotationType;
import com.lookgraph.common.enums.ModifySource;
import com.lookgraph.domain.entity.SemanticHistory;
import com.lookgraph.domain.repository.jpa.SemanticHistoryRepository;
import com.lookgraph.service.SemanticService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * 完整数据持久化演示测试
 * 此测试会插入多种类型的注释数据，用于演示完整的数据流
 */
@SpringBootTest
class CompletePersistenceDemo {

    @Autowired
    private SemanticService semanticService;

    @Autowired
    private SemanticHistoryRepository repository;

    @Test
    void insertCompleteTestData() {
        long startCount = repository.count();
        System.out.println("\n========================================");
        System.out.println("开始插入完整测试数据");
        System.out.println("当前数据库记录数: " + startCount);
        System.out.println("========================================\n");

        // 1. 创建类注释（AI 生成）
        insertClassAnnotation();

        // 2. 创建方法注释（AI 生成）
        insertMethodAnnotation();

        // 3. 创建方法注释（人工修正）
        insertMethodAnnotationHuman();

        // 4. 创建接口注释
        insertInterfaceAnnotation();

        // 5. 创建字段注释
        insertFieldAnnotation();

        long endCount = repository.count();
        System.out.println("\n========================================");
        System.out.println("数据插入完成");
        System.out.println("插入前记录数: " + startCount);
        System.out.println("插入后记录数: " + endCount);
        System.out.println("新增记录数: " + (endCount - startCount));
        System.out.println("========================================\n");
        System.out.println("提示: 请在 MySQL 中执行以下命令查看数据：");
        System.out.println("mysql -u root --default-character-set=utf8mb4 -e \"USE lookgraph; SELECT id, package_name, class_name, method_name, type, content, modified_by FROM semantic_history ORDER BY id DESC LIMIT 10;\"");
    }

    private void insertClassAnnotation() {
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName("com.example.order");
        entity.setClassName("OrderService");
        entity.setType(AnnotationType.CLASS);
        entity.setContent("订单服务类，负责处理订单的创建、查询、取消等核心业务逻辑");
        entity.setGitCommitHash("abc123def456");
        entity.setModifiedBy(ModifySource.AI);
        entity.setModifyReason("根据类名和方法分析自动生成");
        entity.setCreateTime(LocalDateTime.now());

        var response = semanticService.createSemantic(entity);
        System.out.println("[1/5] 类注释已插入 - ID: " + response.getHistoryId() + " | " + entity.getClassName());
    }

    private void insertMethodAnnotation() {
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName("com.example.order");
        entity.setClassName("OrderService");
        entity.setMethodName("createOrder");
        entity.setType(AnnotationType.METHOD);
        entity.setNeo4jNodeId("neo4j-node-uuid-001");
        entity.setContent("创建订单方法，接收订单请求参数，校验库存后创建订单并返回订单号");
        entity.setGitCommitHash("abc123def456");
        entity.setModifiedBy(ModifySource.AI);
        entity.setModifyReason("基于方法签名和调用关系自动生成");
        entity.setCreateTime(LocalDateTime.now());

        var response = semanticService.createSemantic(entity);
        System.out.println("[2/5] 方法注释已插入 (AI) - ID: " + response.getHistoryId() + " | " + entity.getMethodName());
    }

    private void insertMethodAnnotationHuman() {
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName("com.example.order");
        entity.setClassName("OrderService");
        entity.setMethodName("createOrder");
        entity.setType(AnnotationType.METHOD);
        entity.setNeo4jNodeId("neo4j-node-uuid-001");
        entity.setContent("创建订单方法 - 校验用户信息、商品库存、优惠券有效性后，锁定库存并生成订单，返回订单号和支付链接");
        entity.setGitCommitHash("def789ghi012");
        entity.setModifiedBy(ModifySource.HUMAN);
        entity.setModifyReason("人工补充完整业务流程和返回值说明");
        entity.setCreateTime(LocalDateTime.now());

        var response = semanticService.createSemantic(entity);
        System.out.println("[3/5] 方法注释已插入 (人工修正) - ID: " + response.getHistoryId() + " | " + entity.getMethodName());
    }

    private void insertInterfaceAnnotation() {
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName("com.example.payment");
        entity.setClassName("PaymentGateway");
        entity.setType(AnnotationType.INTERFACE);
        entity.setContent("支付网关接口，定义了支付、退款、查询等统一接口规范，支持多种支付渠道");
        entity.setGitCommitHash("ghi345jkl678");
        entity.setModifiedBy(ModifySource.AI);
        entity.setModifyReason("接口定义分析");
        entity.setCreateTime(LocalDateTime.now());

        var response = semanticService.createSemantic(entity);
        System.out.println("[4/5] 接口注释已插入 - ID: " + response.getHistoryId() + " | " + entity.getClassName());
    }

    private void insertFieldAnnotation() {
        SemanticHistory entity = new SemanticHistory();
        entity.setPackageName("com.example.order");
        entity.setClassName("OrderService");
        entity.setFieldName("maxRetryTimes");
        entity.setType(AnnotationType.FIELD);
        entity.setContent("订单创建失败时的最大重试次数，默认为 3 次");
        entity.setGitCommitHash("jkl901mno234");
        entity.setModifiedBy(ModifySource.HUMAN);
        entity.setModifyReason("补充配置说明");
        entity.setCreateTime(LocalDateTime.now());

        var response = semanticService.createSemantic(entity);
        System.out.println("[5/5] 字段注释已插入 - ID: " + response.getHistoryId() + " | " + entity.getFieldName());
    }
}
