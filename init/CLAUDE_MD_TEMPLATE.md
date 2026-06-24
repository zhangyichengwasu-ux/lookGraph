# LookGraph 代码地图助手

## 概述

LookGraph 是一个代码分析和语义搜索系统，使用：
- **Neo4j** 存储代码结构图谱
- **ChromaDB** 进行向量检索
- **MySQL** 存储语义注释历史

**服务地址**: http://localhost:8090  
**Hook 脚本**: `~/.claude/hooks/lookgraph/`

---

## 快速开始

### 初始化新项目

当用户要求分析代码库时：

```bash
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project ProjectName
```

**返回**: `projectId` - 保存它，后续所有操作都需要

### 查看所有项目

```bash
python3 ~/.claude/hooks/lookgraph/project_list.py
```

---

## 典型工作流

### 场景 1: 理解陌生代码库

```bash
# 1. 初始化项目（建立代码地图）
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project MyProject

# 2. 获取项目摘要（整体架构认知）
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>

# 3. 语义搜索找到入口（用业务描述找代码）
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "用户登录流程" 5

# 4. 查看类上下文（读取精简上下文）
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# 5. 了解依赖关系
python3 ~/.claude/hooks/lookgraph/class_relations.py <class_id>
```

### 场景 2: 评估修改影响

```bash
# 1. 分析影响范围（找出所有受影响的代码）
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD <method_id>

# 2. 查看完整调用链
python3 ~/.claude/hooks/lookgraph/method_callchain.py <method_id>
```

### 场景 3: 定位业务逻辑

```bash
# 1. 语义搜索
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "退款流程" 10

# 2. 获取方法上下文
python3 ~/.claude/hooks/lookgraph/method_context.py <method_id>
```

### 场景 4: 代码变更后同步

```bash
# 触发增量扫描，更新图谱
python3 ~/.claude/hooks/lookgraph/project_update.py <project_id>
```

---

## 可用命令详解

### 项目管理

#### project_list.py
列出所有已初始化的项目

```bash
python3 ~/.claude/hooks/lookgraph/project_list.py
```

#### project_init.py
初始化新项目，扫描代码建立图谱

```bash
python3 ~/.claude/hooks/lookgraph/project_init.py <project_path> <project_name>
```

**参数**:
- `project_path`: 项目绝对路径
- `project_name`: 项目名称

**返回**: `projectId`, `classCount`, `methodCount`, `moduleCount`

**注意**: 扫描大型项目可能需要 30-60 秒

#### project_summary.py
获取项目摘要统计

```bash
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>
```

**返回**: 类数量、方法数量、模块列表、项目概览

**建议**: 每次对话开始时调用，建立全局认知

#### project_update.py
触发增量更新

```bash
python3 ~/.claude/hooks/lookgraph/project_update.py <project_id>
```

**使用时机**: 代码修改后、git pull 后

---

### 语义检索

#### semantic_search.py
根据业务描述找代码

```bash
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> <query> [top_k]
```

**参数**:
- `project_id`: 项目 ID
- `query`: 自然语言查询（如 "用户登录逻辑"）
- `top_k`: 返回结果数量（默认 10）

**示例**:
```bash
python3 ~/.claude/hooks/lookgraph/semantic_search.py proj123 "支付失败重试" 5
```

**返回**: 相关度最高的代码片段列表

#### semantic_by_git.py
根据 Git commit hash 查询注释

```bash
python3 ~/.claude/hooks/lookgraph/semantic_by_git.py <git_commit_hash>
```

**用途**: 版本追溯，查看某个版本的所有业务注释

#### semantic_by_node.py
根据 Neo4j 节点 ID 查询注释历史

```bash
python3 ~/.claude/hooks/lookgraph/semantic_by_node.py <neo4j_node_id>
```

#### semantic_class_history.py
查询类的注释演进历史

```bash
python3 ~/.claude/hooks/lookgraph/semantic_class_history.py <package_name> <class_name>
```

**示例**:
```bash
python3 ~/.claude/hooks/lookgraph/semantic_class_history.py com.example UserService
```

**返回**: 当前注释 + 历史所有版本

#### semantic_method_history.py
查询方法的注释演进历史

```bash
python3 ~/.claude/hooks/lookgraph/semantic_method_history.py <package_name> <class_name> <method_name>
```

---

### 结构查询

#### class_relations.py
查询类的关联关系

```bash
python3 ~/.claude/hooks/lookgraph/class_relations.py <class_id>
```

**返回**:
- 继承关系（parent）
- 实现接口（interfaces）
- 依赖的类（dependencies）
- 被依赖的类（dependedBy）

#### class_methods.py
查询类下所有方法

```bash
python3 ~/.claude/hooks/lookgraph/class_methods.py <class_id>
```

#### module_classes.py
查询模块下所有类

```bash
python3 ~/.claude/hooks/lookgraph/module_classes.py <module_id>
```

#### method_callchain.py
查询方法的完整调用链

```bash
python3 ~/.claude/hooks/lookgraph/method_callchain.py <method_id>
```

**返回**:
- 目标方法信息
- 上游调用者（upstream）
- 下游被调用方（downstream）

---

### 上下文获取

#### class_context.py
获取类的精简上下文

```bash
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>
```

**返回**: 类源码 + 字段 + 方法签名 + 依赖关系 + 业务注释

**用途**: 快速理解一个类的全貌

#### method_context.py
获取方法的精简上下文

```bash
python3 ~/.claude/hooks/lookgraph/method_context.py <method_id>
```

**返回**: 方法源码 + 参数 + 返回值 + 调用关系 + 业务注释

**用途**: 深入理解方法实现

---

### 影响分析

#### impact_analysis.py
分析代码修改的影响范围

```bash
python3 ~/.claude/hooks/lookgraph/impact_analysis.py <entity_type> <entity_id>
```

**参数**:
- `entity_type`: `CLASS`、`METHOD`、`MODULE`
- `entity_id`: 对应的 ID

**示例**:
```bash
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD com.example.UserService#login(String,String)
```

**返回**: 所有会受影响的类、方法、模块

**用途**: 修改代码前评估风险

---

## ID 格式说明

### classId（类 ID）
全限定类名：`com.example.service.OrderService`

**如何获取**:
- 通过 `semantic_search.py` 返回的 `metadata.entity_id`（当 entity_type 是 class）
- 通过 `module_classes.py` 返回的类列表
- 通过源代码的包名 + 类名拼接

### methodId（方法 ID）

**⚠️ 重要**: 方法 ID 格式复杂，**不要手工构造**，应该从 API 返回中获取！

**正确格式**: `{完整类名}#{方法名}({完整参数类型列表})`

**完整参数类型规则**:
- 基本类型: `int`, `long`, `boolean`, `double`, `float`, `byte`, `short`, `char`
- 引用类型: **必须使用完全限定类名**
  - ✅ 正确: `java.lang.String`, `java.util.List`, `com.example.dto.UserDTO`
  - ❌ 错误: `String`, `List`, `UserDTO`
- 数组: 类型后加 `[]`
  - 例如: `java.lang.String[]`, `int[]`
- 泛型: **忽略泛型参数**
  - `List<String>` → `java.util.List`
  - `Map<String, Object>` → `java.util.Map`
- 多个参数: 用逗号分隔，**无空格**
  - ✅ 正确: `(java.lang.String,int,java.util.List)`
  - ❌ 错误: `(String, int, List)` 或 `(java.lang.String, int, java.util.List)`

**示例**:
```
✅ com.example.UserService#login(java.lang.String,java.lang.String)
✅ com.example.OrderService#createOrder(com.example.dto.OrderRequest)
✅ com.example.PaymentService#process(java.lang.Long,java.math.BigDecimal,java.util.Map)
❌ com.example.UserService#login(String,String)  # 错误：缺少包名
❌ com.example.UserService#login(String, String)  # 错误：有空格
```

**如何获取正确的 methodId**:

**方法 1: 通过语义搜索获取** (最推荐):
```bash
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "方法功能描述" 10
```
返回结果中 `entity_type` 为 "method" 的项，其 `metadata.entity_id` 就是正确的 methodId

**方法 2: 通过类的方法列表获取**:
```bash
# 先获取类 ID
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "类名" 5

# 然后获取该类的所有方法
python3 ~/.claude/hooks/lookgraph/class_methods.py <class_id>
```
返回的每个方法都包含正确的 methodId

**方法 3: 通过类上下文获取**:
```bash
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>
```
返回结果包含所有方法及其完整签名

**⚠️ 如果必须手工构造**（最后手段）:
1. 先用 `class_context.py` 查看类的完整信息
2. 从方法签名中提取参数的**完全限定类型**
3. 按格式拼接：`类名#方法名(类型1,类型2,类型3)` - **注意无空格**

### moduleId（模块 ID）
模块目录名：`order-service`、`payment-service`

**如何获取**:
- 通过 `project_summary.py` 返回的 `modules` 列表
- 通常是项目的子目录名称

---

## 使用建议

### 当用户问 "这个项目是做什么的？"

```bash
# 1. 获取项目摘要
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>

# 2. 如果有具体模块，查询模块的类
python3 ~/.claude/hooks/lookgraph/module_classes.py <module_id>
```

### 当用户问 "xxx 功能在哪里？"

```bash
# 使用语义搜索
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "xxx功能" 5
```

### 当用户问 "修改这个方法会影响哪些地方？"

```bash
# 影响分析
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD <method_id>
```

### 当用户说 "帮我理解这个类"

```bash
# 1. 获取类上下文
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# 2. 查看类关系
python3 ~/.claude/hooks/lookgraph/class_relations.py <class_id>
```

---

## 注意事项

1. **首次使用**: 必须先运行 `project_init.py`
2. **projectId 管理**: 初始化后保存 projectId，或用 `project_list.py` 查询
3. **大型项目**: 初始化扫描可能需要较长时间
4. **增量更新**: 代码变更后记得运行 `project_update.py`
5. **服务状态**: 确保 LookGraph 服务运行在 http://localhost:8090
6. **⚠️ ID 获取的正确方式**: 
   - **不要手工构造 methodId**！参数类型必须是完全限定名（如 `java.lang.String` 而不是 `String`）
   - 始终通过以下方式获取正确的 ID：
     - `semantic_search.py` 返回的 `metadata.entity_id`
     - `class_methods.py` 返回的方法列表
     - `class_context.py` 返回的方法签名
   - 如果 API 返回 "方法不存在" 错误，通常是因为 methodId 格式错误

---

## 故障排查

### 服务未启动

```bash
# 检查服务状态
curl http://localhost:8090/api/project/list

# 启动服务
cd /Users/zhangyicheng/Documents/GitHub/lookGraph && bash run.sh &
```

### 没有返回结果

- 检查 project_id 是否正确
- 确认项目已初始化
- 尝试重新运行 project_update.py

---

## 配置

- **API 地址**: 默认 http://localhost:8090
- **环境变量**: 设置 `LOOKGRAPH_BASE_URL` 可覆盖默认地址
- **数据库**:
  - Neo4j: bolt://localhost:7687
  - ChromaDB: http://[::1]:8000
  - MySQL: localhost:3306/lookgraph

---

**记住**: 始终从 `project_summary.py` 开始，建立整体认知，再根据具体需求选择合适的命令！
