# LookGraph 代码地图助手

## ⚠️ 重要：何时使用 LookGraph

**当用户要求理解、分析、查找代码时，优先使用 LookGraph，而不是直接 Grep/Read。**

典型触发词：
- "帮我理解这个类/方法"
- "xxx 功能在哪里？"
- "这个项目是做什么的？"
- "修改 xxx 会影响哪些地方？"
- "查找/搜索 xxx 相关代码"

**工作流程**：
1. 先用 `project_list.py` 检查项目是否已初始化
2. 如果未初始化，先运行 `project_init.py`
3. 使用 `semantic_search.py` 或 `class_context.py` 快速定位
4. 理解代码后，用 `semantic_annotate.py` 保存业务语义

---

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

### 场景 5: 提炼和存储业务语义 ⭐

**当你理解了某个类或方法的业务含义时，应该主动将其存储到 LookGraph。**

```bash
# 1. 分析代码，理解业务逻辑
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# 2. 创建类注释
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id <project_id> \
  --project-path /path/to/project \
  --package com.example \
  --class UserService \
  --type CLASS \
  --content "用户服务类，负责用户注册、登录、权限验证" \
  --source AI \
  --reason "代码分析自动生成"

# 3. 创建方法注释
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id <project_id> \
  --project-path /path/to/project \
  --package com.example \
  --class UserService \
  --method login \
  --type METHOD \
  --content "用户登录方法，验证用户名密码，生成 JWT token" \
  --source AI
```

**何时创建注释**:
- ✅ 当你深入分析某个类/方法并理解其业务逻辑时
- ✅ 当用户明确要求理解某段代码时
- ✅ 当你发现重要的业务规则或约束时
- ❌ 不要为琐碎的 getter/setter 创建注释
- ❌ 不要为显而易见的工具类创建注释

**修正注释**:
当发现 AI 生成的注释不准确时：
```bash
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id <project_id> \
  --project-path /path/to/project \
  --package com.example \
  --class UserService \
  --type CLASS \
  --content "修正后的准确描述" \
  --source HUMAN \
  --reason "修正 AI 的理解偏差"
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

#### semantic_annotate.py ⭐
创建或更新业务语义注释

```bash
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id <project_id> \
  --project-path /path/to/project \
  --package <package_name> \
  --class <class_name> \
  [--method <method_name>] \
  --type CLASS|METHOD|FIELD|ENUM|INTERFACE \
  --content "<业务描述>" \
  --source AI|HUMAN \
  [--reason "<创建原因>"]
```

**参数**:
- `project-id`: 项目 ID（必填）
- `project-path`: 项目路径，用于自动获取 git hash（推荐）
- `package`: 包名（必填）
- `class`: 类名（必填）
- `method`: 方法名（注释方法时必填）
- `type`: 注释类型（必填）
- `content`: 业务语义描述（必填）
- `source`: AI 或 HUMAN（必填）
- `reason`: 创建/修改原因（可选）

**自动功能**:
- 自动获取 git commit hash
- 自动触发向量索引更新（增量更新，只更新当前注释，秒级完成）
- 自动保存注释历史

**何时使用**:
- ✅ 分析代码后理解了业务逻辑
- ✅ 发现重要的业务规则
- ✅ 修正不准确的 AI 注释
- ❌ 不要为显而易见的代码创建注释

**示例**:
```bash
# 创建类注释
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id proj123 \
  --project-path /path/to/project \
  --package com.example.service \
  --class OrderService \
  --type CLASS \
  --content "订单服务，处理下单、支付、发货、退款等完整流程" \
  --source AI

# 创建方法注释
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id proj123 \
  --project-path /path/to/project \
  --package com.example.service \
  --class OrderService \
  --method refund \
  --type METHOD \
  --content "订单退款，校验条件，调用支付网关，更新状态，触发库存回滚" \
  --source AI

# 人工修正
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id proj123 \
  --project-path /path/to/project \
  --package com.example.service \
  --class OrderService \
  --type CLASS \
  --content "更准确的业务描述" \
  --source HUMAN \
  --reason "修正 AI 理解偏差"
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

## LookGraph 模式行为准则

### 进入 LookGraph 模式后 (`/look_graph`)

**核心原则**: 所有代码相关的搜索、定位、理解操作，优先使用 LookGraph API，而不是传统的 Grep/Read 工具。

### 工具选择优先级

#### ✅ 必须使用 LookGraph 的场景

1. **搜索代码**
   - ❌ 不要使用 `Grep` 搜索代码内容
   - ✅ 使用 `semantic_search.py` 通过业务描述或功能名称搜索
   ```bash
   python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "描述" 10
   ```

2. **理解类结构**
   - ❌ 不要使用 `Read` 直接读取源文件
   - ✅ 使用 `class_context.py` 获取类的结构化上下文（包含依赖、注释、方法签名）
   ```bash
   python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>
   ```

3. **理解方法实现**
   - ❌ 不要手动查找方法定义
   - ✅ 使用 `method_context.py` 获取方法上下文（包含调用关系、参数、返回值）
   ```bash
   python3 ~/.claude/hooks/lookgraph/method_context.py <method_id>
   ```

4. **查找依赖关系**
   - ❌ 不要手动追踪 import 语句
   - ✅ 使用 `class_relations.py` 获取继承、实现、依赖关系
   ```bash
   python3 ~/.claude/hooks/lookgraph/class_relations.py <class_id>
   ```

5. **评估修改影响**
   - ❌ 不要手动搜索引用
   - ✅ 使用 `impact_analysis.py` 获取影响范围
   ```bash
   python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD <method_id>
   ```

6. **追踪调用链**
   - ❌ 不要手动逐层查找调用
   - ✅ 使用 `method_callchain.py` 获取完整调用链
   ```bash
   python3 ~/.claude/hooks/lookgraph/method_callchain.py <method_id>
   ```

#### ⚠️ 可以使用传统工具的场景

只在以下情况下使用 `Read`、`Grep`、`Glob`：
- 读取配置文件（.yml, .properties, .json）
- 读取文档（README.md, *.md）
- 搜索日志消息或错误文本
- 项目未在 LookGraph 中初始化

### 标准工作流

#### 1. 会话开始时
```bash
# 检查项目是否已初始化
python3 ~/.claude/hooks/lookgraph/project_list.py

# 如果未初始化，先初始化
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project ProjectName

# 获取项目概览
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>
```

#### 2. 定位代码
```bash
# 用语义搜索找到相关代码
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "功能描述" 10
# 返回结果包含 entity_id (classId 或 methodId)
```

#### 3. 理解代码
```bash
# 如果是类，获取类上下文
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# 如果是方法，获取方法上下文
python3 ~/.claude/hooks/lookgraph/method_context.py <method_id>
```

#### 4. 保存理解
```bash
# 当理解了业务含义后，创建注释
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id <project_id> \
  --project-path /path/to/project \
  --package com.example \
  --class ClassName \
  --type CLASS \
  --content "业务含义描述" \
  --source AI
```

### 行为示例

**❌ 错误行为（不要这样做）**:
```
用户: "查找用户登录的代码"
Claude: [使用 Grep 搜索 "login"]
        [使用 Read 读取找到的文件]
```

**✅ 正确行为（应该这样做）**:
```
用户: "查找用户登录的代码"
Claude: [使用 semantic_search.py "用户登录" 搜索]
        [使用 class_context.py 获取类上下文]
        [解释代码结构和业务逻辑]
        [使用 semantic_annotate.py 保存理解]
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
