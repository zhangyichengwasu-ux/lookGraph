# 业务语义版本管理系统

## 架构说明

### 混合存储架构
- **Neo4j**: 存储代码结构图谱（Module、Class、Method 节点及其关系）
- **MySQL**: 存储业务注释的版本历史和元数据

## 数据模型

### MySQL 表结构：semantic_history

| 字段 | 类型 | 说明 | 必填 |
|-----|------|------|------|
| id | BIGINT | 主键（自增） | 是 |
| package_name | VARCHAR(255) | 包名 | 是 |
| class_name | VARCHAR(255) | 类名 | 是 |
| method_name | VARCHAR(255) | 方法名 | 否 |
| field_name | VARCHAR(255) | 字段名 | 否 |
| type | VARCHAR(50) | 注释类型 | 是 |
| neo4j_node_id | VARCHAR(255) | Neo4j 节点 ID | 否 |
| content | TEXT | 注释内容 | 否 |
| git_commit_hash | VARCHAR(40) | Git 提交哈希 | 是 |
| modified_by | VARCHAR(20) | 修改来源 | 是 |
| modify_reason | TEXT | 修改原因 | 否 |
| create_time | TIMESTAMP | 创建时间 | 是 |

### 注释类型（AnnotationType）
- `CLASS` - 类注释
- `ENUM` - 枚举注释
- `INTERFACE` - 接口注释
- `ABSTRACT_CLASS` - 抽象类注释
- `METHOD` - 方法注释
- `FIELD` - 字段注释

### 修改来源（ModifySource）
- `AI` - AI 自动生成/纠正
- `HUMAN` - 人工修正

## API 接口

### 1. 根据 Git Hash 查询所有业务注释
```
GET /api/semantic/git/{gitCommitHash}
```
用于快速查找某个 Git 版本的所有注释及变动原因。

**响应示例：**
```json
[
  {
    "historyId": "1",
    "entityId": "com.example.UserService",
    "entityType": "CLASS",
    "content": "用户服务类，处理用户相关业务逻辑",
    "modifiedBy": "AI",
    "modifyReason": "根据代码上下文自动生成",
    "createTime": "2026-06-23T10:00:00"
  }
]
```

### 2. 根据 Neo4j 节点 ID 查询业务注释历史
```
GET /api/semantic/node/{neo4jNodeId}
```
查询某个 Neo4j 节点的所有历史注释版本。

### 3. 查询类的业务注释历史
```
GET /api/semantic/class?packageName={packageName}&className={className}
```

**响应示例：**
```json
{
  "entityId": "com.example.UserService",
  "entityType": "CLASS",
  "current": {
    "historyId": "3",
    "content": "用户服务类，处理用户注册、登录、信息管理",
    "modifiedBy": "HUMAN",
    "modifyReason": "补充完整业务说明",
    "createTime": "2026-06-23T15:00:00"
  },
  "history": [
    {
      "historyId": "3",
      "content": "用户服务类，处理用户注册、登录、信息管理",
      "modifiedBy": "HUMAN",
      "createTime": "2026-06-23T15:00:00"
    },
    {
      "historyId": "2",
      "content": "用户服务类，处理用户业务逻辑",
      "modifiedBy": "AI",
      "createTime": "2026-06-23T12:00:00"
    },
    {
      "historyId": "1",
      "content": "用户服务类",
      "modifiedBy": "AI",
      "createTime": "2026-06-23T10:00:00"
    }
  ]
}
```

### 4. 查询方法的业务注释历史
```
GET /api/semantic/method?packageName={packageName}&className={className}&methodName={methodName}
```

### 5. 创建业务注释
```
POST /api/semantic
Content-Type: application/json

{
  "packageName": "com.example",
  "className": "UserService",
  "methodName": "login",
  "type": "METHOD",
  "neo4jNodeId": "method-uuid-123",
  "content": "用户登录方法，验证用户名密码",
  "gitCommitHash": "abc123def456",
  "modifiedBy": "AI",
  "modifyReason": "根据方法签名和实现自动生成"
}
```

## 配置说明

### MySQL 配置
在 `application.yml` 中配置或通过环境变量设置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lookgraph
    username: root
    password: your_password
```

**环境变量：**
- `MYSQL_HOST` - MySQL 主机地址（默认: localhost）
- `MYSQL_PORT` - MySQL 端口（默认: 3306）
- `MYSQL_DATABASE` - 数据库名（默认: lookgraph）
- `MYSQL_USERNAME` - 用户名（默认: root）
- `MYSQL_PASSWORD` - 密码（默认: 空）

### 数据库初始化
首次启动时，Hibernate 会自动创建表结构（`ddl-auto: update`）。

## 使用场景

### 场景 1：AI 持续优化业务注释
1. 系统扫描代码生成初始注释（AI）
2. 用户使用过程中，AI 根据上下文不断纠正注释
3. 每次纠正都保留历史版本，记录 Git commit hash

### 场景 2：人工审核和修正
1. 用户查看 AI 生成的注释
2. 发现不准确的地方进行人工修正
3. 系统记录修正原因和来源（HUMAN）

### 场景 3：代码版本追溯
1. 根据 Git commit hash 快速查找该版本的所有业务注释
2. 对比不同版本之间的注释变化
3. 了解业务理解的演进过程

## 核心价值

1. **持续演进**：业务语义随使用不断优化，越用越准确
2. **完整追溯**：保留所有历史版本，可溯源每次变更
3. **商业价值**：无限历史版本是商业版的核心卖点
4. **混合存储**：图数据库处理关系，关系数据库管理元数据
