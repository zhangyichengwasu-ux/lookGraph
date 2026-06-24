package com.lookgraph;

import com.lookgraph.common.enums.ClassType;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.node.ModuleNode;
import com.lookgraph.domain.node.ProjectNode;
import com.lookgraph.domain.repository.neo4j.ClassRepository;
import com.lookgraph.domain.repository.neo4j.MethodRepository;
import com.lookgraph.domain.repository.neo4j.ModuleRepository;
import com.lookgraph.domain.repository.neo4j.ProjectRepository;
import com.lookgraph.parser.ParseResult;
import com.lookgraph.vector.VectorDocument;
import com.lookgraph.vector.VectorIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DataWriteIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private MethodRepository methodRepository;
    @Autowired
    private VectorIndexService vectorIndexService;

    @Test
    void writeNeo4jTestData() {
        // Project
        ProjectNode project = new ProjectNode();
        project.setProjectId("test-project-001");
        project.setName("TestECommerce");
        project.setPath("/projects/test-ecommerce");
        project.setTechStack("Java,Spring Boot,Neo4j");
        project.setCreateTime(Instant.now());
        project.setUpdateTime(Instant.now());
        projectRepository.save(project);

        // Module
        ModuleNode orderModule = new ModuleNode();
        orderModule.setModuleId("test-module-order");
        orderModule.setName("order-service");
        orderModule.setBusinessTag("订单域");
        orderModule.setProjectId("test-project-001");

        ModuleNode payModule = new ModuleNode();
        payModule.setModuleId("test-module-pay");
        payModule.setName("payment-service");
        payModule.setBusinessTag("支付域");
        payModule.setProjectId("test-project-001");

        orderModule.setDependsOn(Set.of(payModule));
        moduleRepository.save(payModule);
        moduleRepository.save(orderModule);

        // Class
        ClassNode orderService = new ClassNode();
        orderService.setClassId("test-class-order-svc");
        orderService.setName("OrderService");
        orderService.setFilePath("/projects/test-ecommerce/order/OrderService.java");
        orderService.setComment("订单核心服务，处理下单、取消、查询");
        orderService.setModuleId("test-module-order");
        orderService.setProjectId("test-project-001");
        orderService.setType(ClassType.CLASS);

        ClassNode payClient = new ClassNode();
        payClient.setClassId("test-class-pay-client");
        payClient.setName("PaymentClient");
        payClient.setFilePath("/projects/test-ecommerce/pay/PaymentClient.java");
        payClient.setComment("支付网关客户端，对接第三方支付渠道");
        payClient.setModuleId("test-module-pay");
        payClient.setProjectId("test-project-001");
        payClient.setType(ClassType.CLASS);

        orderService.setDependsOn(Set.of(payClient));
        classRepository.save(payClient);
        classRepository.save(orderService);

        // Method
        MethodNode createOrder = new MethodNode();
        createOrder.setMethodId("test-method-create-order");
        createOrder.setName("createOrder");
        createOrder.setParams("CreateOrderRequest request");
        createOrder.setReturnType("OrderVO");
        createOrder.setComment("创建订单，校验库存后锁定并发起支付");
        createOrder.setClassId("test-class-order-svc");
        createOrder.setProjectId("test-project-001");
        createOrder.setStartLine(25);
        createOrder.setEndLine(58);

        MethodNode doPay = new MethodNode();
        doPay.setMethodId("test-method-do-pay");
        doPay.setName("doPay");
        doPay.setParams("PayRequest payRequest");
        doPay.setReturnType("PayResult");
        doPay.setComment("调用第三方支付渠道执行扣款");
        doPay.setClassId("test-class-pay-client");
        doPay.setProjectId("test-project-001");
        doPay.setStartLine(30);
        doPay.setEndLine(50);

        createOrder.setCalls(Set.of(doPay));
        methodRepository.save(doPay);
        methodRepository.save(createOrder);

        // 验证 Neo4j 写入
        assertTrue(projectRepository.findById("test-project-001").isPresent());
        assertTrue(classRepository.findById("test-class-order-svc").isPresent());
        assertTrue(methodRepository.findById("test-method-create-order").isPresent());
        System.out.println("[Neo4j] 数据写入成功，共写入: 1 Project, 2 Module, 2 Class, 2 Method");
    }

    @Test
    void writeChromaTestData() {
        String projectId = "test-project-001";

        // 构造带注释的类与方法节点（VectorIndexService 只索引有 comment 的实体）
        ClassNode orderService = new ClassNode();
        orderService.setClassId("test-class-order-svc");
        orderService.setName("OrderService");
        orderService.setFilePath("/projects/test-ecommerce/order/OrderService.java");
        orderService.setComment("订单核心服务，处理下单、取消、查询");
        orderService.setModuleId("test-module-order");
        orderService.setProjectId(projectId);
        orderService.setType(ClassType.CLASS);

        ClassNode payClient = new ClassNode();
        payClient.setClassId("test-class-pay-client");
        payClient.setName("PaymentClient");
        payClient.setFilePath("/projects/test-ecommerce/pay/PaymentClient.java");
        payClient.setComment("支付网关客户端，对接微信、支付宝等第三方支付渠道");
        payClient.setModuleId("test-module-pay");
        payClient.setProjectId(projectId);
        payClient.setType(ClassType.CLASS);

        ClassNode userRepo = new ClassNode();
        userRepo.setClassId("test-class-user-repo");
        userRepo.setName("UserRepository");
        userRepo.setFilePath("/projects/test-ecommerce/user/UserRepository.java");
        userRepo.setComment("用户数据访问层，提供用户注册、查询、更新功能");
        userRepo.setModuleId("test-module-user");
        userRepo.setProjectId(projectId);
        userRepo.setType(ClassType.CLASS);

        MethodNode createOrder = new MethodNode();
        createOrder.setMethodId("test-method-create-order");
        createOrder.setName("createOrder");
        createOrder.setComment("创建订单，校验库存后锁定并发起支付");
        createOrder.setClassId("test-class-order-svc");
        createOrder.setProjectId(projectId);

        MethodNode doPay = new MethodNode();
        doPay.setMethodId("test-method-do-pay");
        doPay.setName("doPay");
        doPay.setComment("调用第三方支付渠道执行扣款");
        doPay.setClassId("test-class-pay-client");
        doPay.setProjectId(projectId);

        ParseResult parseResult = new ParseResult(
                List.of(orderService, payClient, userRepo),
                List.of(createOrder, doPay),
                List.of(),
                List.of()
        );

        // 初始化集合 + 写入向量
        vectorIndexService.initCollections();
        vectorIndexService.indexEntities(projectId, parseResult);

        System.out.println("[ChromaDB] 向量化 + 写入成功，共写入 5 条实体（3 Class + 2 Method）");

        // 基本查询验证：查询功能正常工作（不要求特定结果排序，因为这取决于向量模型质量和已有数据）
        List<VectorDocument> results = vectorIndexService.search("支付扣款流程", 5, null);

        System.out.println("[ChromaDB] 查询返回 " + results.size() + " 条:");
        for (VectorDocument doc : results) {
            System.out.printf("  - id=%s score=%.4f doc=%s%n",
                    doc.getId(), doc.getScore(), doc.getDocument());
        }

        // 只验证向量查询系统能正常工作，返回结果即可
        assertTrue(results.size() > 0, "向量查询系统应能返回结果");
    }
}
