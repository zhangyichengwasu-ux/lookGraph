-- LookGraph 业务语义版本管理系统 - MySQL DDL
-- 数据库: lookgraph

CREATE DATABASE IF NOT EXISTS lookgraph
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lookgraph;

-- ============================================================================
-- 语义注释历史表
-- ============================================================================

CREATE TABLE IF NOT EXISTS semantic_history (
    -- 主键
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    -- 实体定位
    package_name VARCHAR(255) NOT NULL COMMENT '包名',
    class_name VARCHAR(255) NOT NULL COMMENT '类名',
    method_name VARCHAR(255) DEFAULT NULL COMMENT '方法名（注释方法时填写）',
    field_name VARCHAR(255) DEFAULT NULL COMMENT '字段名（注释字段时填写）',

    -- 注释类型
    type VARCHAR(50) NOT NULL COMMENT '注释类型: CLASS, ENUM, INTERFACE, ABSTRACT_CLASS, METHOD, FIELD',

    -- 关联图谱节点
    neo4j_node_id VARCHAR(255) DEFAULT NULL COMMENT 'Neo4j 节点 ID，用于关联图谱',

    -- 注释内容
    content TEXT COMMENT '业务注释内容',

    -- 版本追溯
    git_commit_hash VARCHAR(64) NOT NULL COMMENT 'Git 提交哈希或文件内容哈希，用于版本追溯',

    -- 修改元信息
    modified_by VARCHAR(20) NOT NULL COMMENT '修改来源: AI, HUMAN',
    modify_reason TEXT COMMENT '修改原因说明',

    -- 时间戳
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_git_hash (git_commit_hash) COMMENT '按 Git Hash 查询',
    INDEX idx_package_class (package_name, class_name) COMMENT '按包名+类名查询',
    INDEX idx_neo4j_node (neo4j_node_id) COMMENT '按 Neo4j 节点查询',
    INDEX idx_create_time (create_time) COMMENT '按时间排序'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务语义注释历史表';

-- ============================================================================
-- 索引说明
-- ============================================================================

-- idx_git_hash: 用于快速查找某个 Git 版本的所有注释
-- idx_package_class: 用于查询某个类的注释历史
-- idx_neo4j_node: 用于通过图谱节点 ID 查询注释
-- idx_create_time: 用于按时间排序历史版本

-- ============================================================================
-- 使用示例
-- ============================================================================

-- 1. 插入 AI 生成的类注释
-- INSERT INTO semantic_history (package_name, class_name, type, content, git_commit_hash, modified_by, modify_reason, create_time)
-- VALUES ('com.example', 'UserService', 'CLASS', '用户服务类，处理用户相关业务', 'abc123', 'AI', '根据代码分析自动生成', NOW());

-- 2. 插入人工修正的方法注释
-- INSERT INTO semantic_history (package_name, class_name, method_name, type, content, git_commit_hash, modified_by, modify_reason, create_time)
-- VALUES ('com.example', 'UserService', 'login', 'METHOD', '用户登录方法，验证用户名密码并返回 token', 'def456', 'HUMAN', '补充返回值说明', NOW());

-- 3. 查询类的所有历史注释（按时间倒序）
-- SELECT * FROM semantic_history
-- WHERE package_name = 'com.example' AND class_name = 'UserService'
-- ORDER BY create_time DESC;

-- 4. 查询某个 Git 版本的所有注释
-- SELECT * FROM semantic_history
-- WHERE git_commit_hash = 'abc123'
-- ORDER BY create_time DESC;

-- 5. 查询方法的注释历史
-- SELECT * FROM semantic_history
-- WHERE package_name = 'com.example' AND class_name = 'UserService' AND method_name = 'login'
-- ORDER BY create_time DESC;

-- 6. 统计各类型注释数量
-- SELECT type, COUNT(*) as count FROM semantic_history GROUP BY type;

-- 7. 统计 AI vs 人工修正比例
-- SELECT modified_by, COUNT(*) as count FROM semantic_history GROUP BY modified_by;
