# LookGraph Hooks 测试计划

## 测试目标
验证所有 17 个 hook 脚本的正确性，确保数据库读写操作正确。

## 测试环境
- LookGraph API: http://localhost:8080
- MySQL: 存储语义注释历史
- Neo4j: 存储代码结构图谱
- ChromaDB: 存储向量数据用于语义搜索

## 测试分类

### 一、写入操作（需验证数据库变化）

#### 1. project_init.py
**API**: POST /api/project/init
**写入目标**: Neo4j (ProjectNode, ModuleNode, ClassNode, MethodNode)
**测试步骤**:
1. 执行初始化命令
2. 查询 Neo4j 验证节点创建
3. 检查项目、模块、类、方法节点数量
**验证点**:
- Neo4j 中是否创建了项目节点
- 是否正确扫描了所有模块、类、方法
- 关系是否正确建立

#### 2. project_update.py
**API**: POST /api/project/update
**写入目标**: Neo4j (更新节点), ChromaDB (更新向量), MySQL (可能的语义历史)
**测试步骤**:
1. 修改源代码文件
2. 执行增量更新
3. 查询 Neo4j 验证节点更新
4. 检查 ChromaDB 向量是否更新
**验证点**:
- 新增/修改的代码是否被扫描
- 删除的代码节点是否被标记
- 向量数据是否同步更新

#### 3. semantic annotation (通过 SemanticController 的 POST /api/semantic)
**说明**: 虽然没有单独的 hook 脚本，但需要验证语义注释创建功能
**写入目标**: MySQL (semantic_history 表), Neo4j (可能的 SemanticHistoryNode)
**测试步骤**:
1. 创建一条业务注释
2. 查询 MySQL 验证记录
3. 查询 API 验证返回
**验证点**:
- MySQL 中是否插入了新记录
- 注释内容是否正确
- 关联的节点 ID 是否正确

---

### 二、读取操作（需验证返回结果与数据库一致）

#### 1. project_list.py
**API**: GET /api/project/list
**读取源**: Neo4j (ProjectNode)
**测试步骤**:
1. 执行脚本
2. 查询 Neo4j 的所有项目节点
3. 对比结果
**验证点**:
- 返回的项目数量与 Neo4j 一致
- 项目 ID、名称、路径正确

#### 2. project_summary.py
**API**: GET /api/project/summary?projectId=
**读取源**: Neo4j (统计查询)
**测试步骤**:
1. 选择一个项目 ID
2. 执行脚本
3. 手动在 Neo4j 中统计
**验证点**:
- 模块数量正确
- 类数量正确
- 方法数量正确
- 统计数据准确

#### 3. semantic_search.py
**API**: POST /api/semantic/search
**读取源**: ChromaDB (向量检索)
**测试步骤**:
1. 执行语义搜索
2. 查询 ChromaDB 验证向量数据
3. 验证返回的相似度排序
**验证点**:
- 返回结果包含相关代码
- 相似度分数合理
- top_k 参数生效

#### 4. semantic_by_git.py
**API**: GET /api/semantic/git/{hash}
**读取源**: MySQL (semantic_history 表)
**测试步骤**:
1. 查询 MySQL 找到一个 git_commit_hash
2. 执行脚本
3. 对比结果
**验证点**:
- 返回的注释与数据库匹配
- git_commit_hash 正确

#### 5. semantic_by_node.py
**API**: GET /api/semantic/node/{id}
**读取源**: MySQL (semantic_history 表)
**测试步骤**:
1. 查询 MySQL 找到一个 neo4j_node_id
2. 执行脚本
3. 对比结果
**验证点**:
- 返回的注释与数据库匹配
- neo4j_node_id 正确

#### 6. semantic_class_history.py
**API**: GET /api/semantic/class?packageName=&className=
**读取源**: MySQL (semantic_history 表 JOIN Neo4j 查询)
**测试步骤**:
1. 从 Neo4j 选择一个类
2. 执行脚本
3. 查询 MySQL 验证历史记录
**验证点**:
- 返回所有历史版本
- 按时间排序
- 内容正确

#### 7. semantic_method_history.py
**API**: GET /api/semantic/method?packageName=&className=&methodName=
**读取源**: MySQL (semantic_history 表 JOIN Neo4j 查询)
**测试步骤**:
1. 从 Neo4j 选择一个方法
2. 执行脚本
3. 查询 MySQL 验证历史记录
**验证点**:
- 返回所有历史版本
- 按时间排序
- 内容正确

#### 8. class_relations.py
**API**: GET /api/structure/class/{id}/relation
**读取源**: Neo4j (关系查询)
**测试步骤**:
1. 选择一个类 ID
2. 执行脚本
3. 在 Neo4j 中查询该类的关系
**验证点**:
- 依赖关系正确 (DEPENDS_ON)
- 继承关系正确 (EXTENDS, IMPLEMENTS)
- 关系方向正确

#### 9. class_methods.py
**API**: GET /api/structure/class/{id}/methods
**读取源**: Neo4j (HAS_METHOD 关系)
**测试步骤**:
1. 选择一个类 ID
2. 执行脚本
3. 在 Neo4j 中查询该类的方法
**验证点**:
- 方法数量一致
- 方法名称、签名正确
- 所有方法都被返回

#### 10. module_classes.py
**API**: GET /api/structure/module/{id}/classes
**读取源**: Neo4j (CONTAINS 关系)
**测试步骤**:
1. 选择一个模块 ID
2. 执行脚本
3. 在 Neo4j 中查询该模块的类
**验证点**:
- 类数量一致
- 类名称正确
- 包名正确

#### 11. method_callchain.py
**API**: GET /api/structure/method/{id}/callchain
**读取源**: Neo4j (CALLS 关系链)
**测试步骤**:
1. 选择一个方法 ID
2. 执行脚本
3. 在 Neo4j 中查询调用链
**验证点**:
- 调用链完整
- 上游调用者正确
- 下游被调用者正确
- 深度合理

#### 12. impact_analysis.py
**API**: GET /api/structure/impact/{type}/{id}
**读取源**: Neo4j (依赖关系分析)
**测试步骤**:
1. 选择一个实体 (CLASS/METHOD/MODULE)
2. 执行脚本
3. 在 Neo4j 中追踪依赖关系
**验证点**:
- 直接影响的实体正确
- 间接影响的实体正确
- 影响范围合理

#### 13. class_context.py
**API**: GET /api/context/class/{id}
**读取源**: Neo4j (类及其相关信息)
**测试步骤**:
1. 选择一个类 ID
2. 执行脚本
3. 验证返回的上下文信息
**验证点**:
- 类基本信息正确
- 字段列表正确
- 方法签名正确
- 依赖关系正确

#### 14. method_context.py
**API**: GET /api/context/method/{id}
**读取源**: Neo4j (方法及其相关信息)
**测试步骤**:
1. 选择一个方法 ID
2. 执行脚本
3. 验证返回的上下文信息
**验证点**:
- 方法签名正确
- 参数列表正确
- 返回类型正确
- 调用的方法列表正确

---

## 测试执行顺序

### 阶段 1: 环境准备
1. 启动 LookGraph 服务
2. 验证数据库连接 (MySQL, Neo4j, ChromaDB)
3. 测试基本 API 可用性

### 阶段 2: 写入操作测试
1. 测试 project_init.py - 初始化测试项目
2. 验证 Neo4j 数据
3. 测试 project_update.py - 触发增量更新
4. 验证更新后的数据

### 阶段 3: 读取操作测试
按照上述列表逐个测试，每个测试：
1. 执行 hook 脚本
2. 查询对应数据库
3. 对比验证结果
4. 记录问题

### 阶段 4: 问题修复
1. 汇总所有发现的问题
2. 逐个修复
3. 回归测试

---

## 数据库查询命令参考

### Neo4j 查询
```cypher
// 查询所有项目
MATCH (p:Project) RETURN p

// 查询项目统计
MATCH (p:Project {id: 'xxx'})
OPTIONAL MATCH (p)-[:CONTAINS]->(m:Module)
OPTIONAL MATCH (m)-[:CONTAINS]->(c:Class)
OPTIONAL MATCH (c)-[:HAS_METHOD]->(method:Method)
RETURN p.name, count(DISTINCT m) as modules, count(DISTINCT c) as classes, count(DISTINCT method) as methods

// 查询类关系
MATCH (c:Class {id: 'xxx'})-[r]-(other)
RETURN type(r), other

// 查询方法调用链
MATCH path = (m:Method {id: 'xxx'})-[:CALLS*]->(called)
RETURN path
```

### MySQL 查询
```sql
-- 查询语义注释
SELECT * FROM semantic_history WHERE git_commit_hash = 'xxx';

SELECT * FROM semantic_history WHERE neo4j_node_id = 'xxx';

-- 查询类的注释历史
SELECT sh.* FROM semantic_history sh
WHERE sh.package_name = 'xxx' AND sh.class_name = 'yyy'
ORDER BY sh.created_at DESC;

-- 查询方法的注释历史
SELECT sh.* FROM semantic_history sh
WHERE sh.package_name = 'xxx' AND sh.class_name = 'yyy' AND sh.method_name = 'zzz'
ORDER BY sh.created_at DESC;
```

### ChromaDB 查询
```python
# 查询向量数据
collection.get()
collection.query(query_texts=["search term"], n_results=5)
```

---

## 测试结果记录

| 脚本名称 | 测试状态 | 数据库验证 | 问题描述 | 修复状态 |
|---------|---------|-----------|---------|---------|
| test_connection.py | ⏳ | - | - | - |
| project_init.py | ⏳ | Neo4j | - | - |
| project_update.py | ⏳ | Neo4j/ChromaDB | - | - |
| project_list.py | ⏳ | Neo4j | - | - |
| project_summary.py | ⏳ | Neo4j | - | - |
| semantic_search.py | ⏳ | ChromaDB | - | - |
| semantic_by_git.py | ⏳ | MySQL | - | - |
| semantic_by_node.py | ⏳ | MySQL | - | - |
| semantic_class_history.py | ⏳ | MySQL/Neo4j | - | - |
| semantic_method_history.py | ⏳ | MySQL/Neo4j | - | - |
| class_relations.py | ⏳ | Neo4j | - | - |
| class_methods.py | ⏳ | Neo4j | - | - |
| module_classes.py | ⏳ | Neo4j | - | - |
| method_callchain.py | ⏳ | Neo4j | - | - |
| impact_analysis.py | ⏳ | Neo4j | - | - |
| class_context.py | ⏳ | Neo4j | - | - |
| method_context.py | ⏳ | Neo4j | - | - |

---

## 预期问题清单

1. **连接问题**: 服务未启动、端口被占用
2. **参数错误**: 脚本参数传递错误
3. **数据不一致**: API 返回与数据库不匹配
4. **空数据**: 数据库中没有测试数据
5. **编码问题**: 中文字符处理
6. **权限问题**: 文件或数据库访问权限
7. **异常处理**: 错误情况下的脚本行为

---

**测试开始时间**: 待定  
**预计完成时间**: 待定  
**测试执行人**: Kiro AI
