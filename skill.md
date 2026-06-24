# LookGraph 代码地图助手 - Claude 使用指南

## 服务地址
`http://localhost:8090`

---

## 快速开始

### 1. 初始化项目代码地图

在分析任何代码库之前，先初始化项目：

```bash
curl -X POST http://localhost:8090/api/project/init \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-project",
    "path": "/absolute/path/to/project",
    "language": "JAVA"
  }'
```

**返回**：`projectId`，后续所有查询都需要它。

支持的 `language` 值：`JAVA`、`PYTHON`

---

## 核心接口

### 项目管理

| 操作 | 方法 | 路径 |
|------|------|------|
| 初始化项目 | POST | `/api/project/init` |
| 触发增量更新 | POST | `/api/project/update?projectId={id}` |
| 获取项目摘要 | GET | `/api/project/summary?projectId={id}` |
| 列出所有项目 | GET | `/api/project/list` |

**获取项目摘要**（每次对话开始时调用，建立全局认知）：
```bash
curl http://localhost:8090/api/project/summary?projectId={projectId}
```

---

### 结构查询（Neo4j 图谱）

#### 查询类的关联关系
```bash
curl http://localhost:8090/api/structure/class/{classId}/relation
```
返回：继承关系（parent）、实现接口（interfaces）、依赖类（dependencies）、被依赖类（dependedBy）

#### 查询方法调用链路
```bash
curl http://localhost:8090/api/structure/method/{methodId}/callchain
```
返回：目标方法、上游调用者（upstream）、下游被调用方（downstream）

#### 查询模块下所有类
```bash
curl http://localhost:8090/api/structure/module/{moduleId}/classes
```

#### 查询类下所有方法
```bash
curl http://localhost:8090/api/structure/class/{classId}/methods
```

#### 分析修改影响范围
```bash
curl http://localhost:8090/api/structure/impact/{entityType}/{entityId}
```
`entityType` 可选值：`CLASS`、`METHOD`、`MODULE`

---

### 语义检索（ChromaDB 向量）

根据业务描述找到对应代码：
```bash
curl -X POST http://localhost:8090/api/semantic/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "支付失败重试逻辑",
    "module": "支付模块",
    "topK": 5
  }'
```
`module` 和 `topK` 为可选参数，`topK` 默认 10。

---

### 业务语义注释管理

#### 创建/更新业务注释
```bash
curl -X POST http://localhost:8090/api/semantic \
  -H "Content-Type: application/json" \
  -d '{
    "packageName": "com.example",
    "className": "UserService",
    "methodName": "login",
    "type": "METHOD",
    "neo4jNodeId": "method-uuid-123",
    "content": "用户登录方法，验证用户名密码",
    "gitCommitHash": "abc123def456",
    "modifiedBy": "AI",
    "modifyReason": "根据方法签名和实现自动生成"
  }'
```

**字段说明**：
- `packageName`（必填）：包名
- `className`（必填）：类名
- `methodName`（可选）：方法名（注释类时不填）
- `fieldName`（可选）：字段名（注释字段时填写）
- `type`（必填）：注释类型，可选值：`CLASS`、`ENUM`、`INTERFACE`、`ABSTRACT_CLASS`、`METHOD`、`FIELD`
- `neo4jNodeId`（可选）：Neo4j 节点 ID，用于关联图谱节点
- `content`（可选）：业务注释内容
- `gitCommitHash`（必填）：当前代码的 Git commit hash
- `modifiedBy`（必填）：修改来源，可选值：`AI`、`HUMAN`
- `modifyReason`（可选）：修改原因说明

#### 查询类的注释历史
```bash
curl "http://localhost:8090/api/semantic/class?packageName=com.example&className=UserService"
```

**返回示例**：
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
  "history": [...]
}
```

#### 查询方法的注释历史
```bash
curl "http://localhost:8090/api/semantic/method?packageName=com.example&className=UserService&methodName=login"
```

#### 根据 Git Hash 查询所有注释
```bash
curl http://localhost:8090/api/semantic/git/{gitCommitHash}
```
用于查找某个 Git 版本的所有业务注释及变动原因。

#### 根据 Neo4j 节点 ID 查询注释历史
```bash
curl http://localhost:8090/api/semantic/node/{neo4jNodeId}
```
查询某个 Neo4j 节点的所有历史注释版本。

---

### 上下文获取（最小代码切片）

获取方法的精简上下文（源码 + 上下游调用 + 注释）：
```bash
curl http://localhost:8090/api/context/method/{methodId}
```

获取类的精简上下文：
```bash
curl http://localhost:8090/api/context/class/{classId}
```

---

## ID 格式说明

- **classId**：全限定类名，如 `com.example.service.OrderService`
- **methodId**：`{classId}#{方法名}({参数类型,...})`，如 `com.example.service.OrderService#createOrder(OrderRequest)`
- **moduleId**：模块目录名，如 `order-service`

---

## 典型工作流

### 场景 1：理解一个陌生代码库
```
1. POST /api/project/init          → 建立代码地图
2. GET  /api/project/summary       → 获取整体架构认知
3. POST /api/semantic/search       → 用业务描述找到入口类/方法
4. GET  /api/context/class/{id}    → 读取类的精简上下文
5. GET  /api/structure/class/{id}/relation → 了解依赖关系
```

### 场景 2：评估修改影响
```
1. GET /api/structure/impact/METHOD/{methodId}  → 找出所有受影响的调用方
2. GET /api/structure/method/{methodId}/callchain → 查看完整调用链
```

### 场景 3：定位业务逻辑
```
1. POST /api/semantic/search  query="退款流程"  → 找到相关类/方法
2. GET  /api/context/method/{methodId}          → 获取方法源码和上下文
```

### 场景 4：代码变更后同步
```
POST /api/project/update?projectId={id}   → 触发增量扫描，更新图谱
```

### 场景 5：管理业务语义注释
```
# AI 生成注释
1. 分析代码上下文，理解业务逻辑
2. POST /api/semantic  → 创建 AI 生成的注释（modifiedBy="AI"）

# 查看注释历史
3. GET /api/semantic/class?packageName=...&className=...  → 查看类的注释演进历史

# 人工纠正注释
4. POST /api/semantic  → 创建人工修正版本（modifiedBy="HUMAN"）

# 版本追溯
5. GET /api/semantic/git/{hash}  → 查看某个版本的所有注释
```

---

## 注意事项

1. **首次使用**：必须先调用 `/api/project/init`，扫描大型项目可能需要数十秒。
2. **ChromaDB 未启动时**：语义检索不可用，结构查询仍正常工作。
3. **Embedding API 未配置时**：向量索引跳过，仅图谱功能可用。
4. **增量更新**：代码变更后调用 `/api/project/update` 保持图谱最新。
5. **Swagger UI**：`http://localhost:8090/swagger-ui.html` 可交互式测试所有接口。
6. **业务注释管理**：
   - 注释历史无限保留，每次修改都会创建新版本
   - `gitCommitHash` 必填，用于关联代码版本
   - AI 和人工修正的注释通过 `modifiedBy` 字段区分
   - 可通过 Git hash 快速定位某个版本的所有业务理解
