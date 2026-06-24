# LookGraph — 代码地图助手

> 代码结构图谱 + 业务语义向量双引擎，让 AI 无需加载全量代码即可理解大型项目。

## 项目简介

大型项目动辄数十万行代码，直接喂给 AI 既超出上下文限制，又浪费 Token。LookGraph 将代码库预处理为三类索引：

- **结构图谱（Neo4j）**: 类、方法、模块之间的继承、调用、依赖关系，还原代码骨架
- **语义向量（ChromaDB）**: 注释与业务描述的向量索引，支持自然语言检索代码含义
- **版本历史（MySQL）**: 业务注释的完整演进历史，支持 AI/人工持续优化

AI 只需按需查询切片，而不是读取整个项目，上下文开销大幅降低，代码分析、重构、问题排查更精准。

---

## 效果验证：对比实验

我们在真实商城后端代码库（zcyl-backend）上，以「绘制下单→派单→备货→发货全链路业务流程图」为任务，进行了三轮严格对比实验：

| 轮次 | 模式 | 说明 |
|------|------|------|
| 第1轮 | **不使用 LookGraph** | 仅用传统 Grep/Read，逐步拼凑代码信息 |
| 第2轮 | **LG 初次使用** | 首次使用 LookGraph 辅助完成 |
| 第3轮 | **LG 熟练使用** | 熟悉工具链后再次执行（排除初学效应） |

### 实验结果

| 指标 | 无 LG | LG 熟练 | 变化 |
|------|:-----:|:------:|:----:|
| **交互轮次** | 99 | 53 | **↓46%** |
| **工具调用** | 61 | 31 | **↓49%** |
| **传统 Grep/Read** | 29 | 0 | **完全替代** |
| **总计 Token** | 455K | 521K | ↑15% |
| **单轮效率** | 1.0× | 1.9× | **↑87%** |

### 交付物质量

| 维度 | 无 LG | LG 熟练 |
|------|:-----:|:------:|
| 完整性 | ★★★★★ | ★★★★ |
| 字段精度 | ★★★★★ | ★★★★ |
| 可读性 | ★★★ | ★★★★★ |
| 跨模块视角 | ★★★ | ★★★★★ |

### 核心结论

**LookGraph 的价值不在于省钱，而在于省时间和提质量。**

- **Token 并未大幅降低**（+15%），因为 LG 返回的信息密度远高于传统 Grep 的匹配片段——一次 `class_context` 调用就能返回类源码、字段、方法签名、依赖关系和业务注释
- **交互轮次减半**（99→53），一次 LG API 调用能替代多次 Grep/Read 的逐步拼凑，对话效率翻倍
- **交付物质量提升**：跨模块视角（支付→结算闭环）和可读性显著优于无 LG 模式，传统方式虽在字段精度上略胜一筹，但信息密度已超出最佳承载范围
- **熟练度效应明显**：经过 1-2 次任务熟悉后，LG 的效能才能完全释放

> 详细对比报告见：[测试对比/长链路业务流程解读实验/业务流程解读测试对比报告.md](测试对比/长链路业务流程解读实验/业务流程解读测试对比报告.md)

---

## 快速开始

### 一键启动

```bash
# 1. 拉取嵌入模型
ollama pull nomic-embed-text

# 2. 启动服务
bash run.sh

# 3. 安装 Claude Code 集成
cd init && python3 install.py
```

### 验证安装

```bash
# 检查服务状态
curl http://localhost:8090/actuator/health

# 检查 Neo4j
curl -u neo4j:lookgraph123 http://localhost:7474

# 检查 Claude Code 安装
ls ~/.claude/hooks/lookgraph/
ls .claude/skills/look_graph/SKILL.md
```

### 环境要求

| 依赖 | 版本要求 |
|------|----------|
| Java | 21+ |
| Maven | 3.9+ |
| Python | 3.10+（用于 Hook Scripts） |

### 启动服务

```bash
# 构建项目
mvn package -DskipTests

# 启动服务
java -jar target/look-graph-1.0.0-SNAPSHOT.jar
```

服务启动后访问：
- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **健康检查**: http://localhost:8090/actuator/health

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| LookGraph API | 8090 | 主服务 |
| Neo4j Browser | 7474 | 图数据库界面 |
| Neo4j Bolt | 7687 | 图数据库连接 |
| ChromaDB | 8000 | 向量数据库 |
| MySQL | 3306 | 关系数据库 |
| Ollama | 11434 | 嵌入模型服务 |

### 安装 Claude Code 集成

```bash
cd init
python3 install.py
```

安装后，Claude 可直接调用 LookGraph 的所有分析能力。

**安装内容**：
- ✅ Hook 脚本 → `~/.claude/hooks/lookgraph/`
- ✅ 模式切换 Skills → `<project>/.claude/skills/`（项目级别）
  - `look_graph/SKILL.md` - 进入 LookGraph 模式
  - `exit_look_graph/SKILL.md` - 退出 LookGraph 模式
- ✅ 文档 → `~/.claude/CLAUDE.md`

**注意**: Skills 安装在项目目录下，每个项目可以独立配置。

### 停止和清理

```bash
# 停止 LookGraph 服务
# Ctrl+C 或
pkill -f look-graph

# 卸载 Claude Code 集成
cd init && python3 uninstall.py
```

---

## 核心特性

### 1. 代码结构图谱

- 自动解析 Java/Python 代码，构建类、方法、模块的关系图
- 支持继承、实现、调用、依赖关系查询
- 影响分析：评估代码修改的影响范围
- 调用链追踪：查看方法的完整调用路径

### 2. 语义检索

- 自然语言搜索代码：用业务描述找到对应实现
- 向量化业务注释，支持相似度检索
- 与代码结构图谱结合，提供精准上下文

### 3. 版本管理

- 记录所有业务注释的演进历史
- 支持 AI 自动生成和人工修正
- 关联 Git commit hash，追溯每次变更
- 完整的历史版本追溯能力

---

## 架构设计

```
┌─────────────────────────────────────┐
│         应用交互层                   │
│   Claude / 第三方工具  HTTP API      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         核心服务层                   │
│  • 项目扫描与初始化                  │
│  • 图结构查询                        │
│  • 向量语义检索                      │
│  • 上下文组装                        │
│  • 影响分析                          │
└──┬────────┬──────────┬──────────────┘
   │        │          │
┌──▼──┐  ┌─▼───┐  ┌───▼────┐
│Neo4j│  │Chroma│  │ MySQL  │
│图谱 │  │向量 │  │版本历史│
└─────┘  └──────┘  └────────┘
   ▲        ▲          ▲
   └────────┴──────────┘
    代码解析层（Java/Python）
```

### 目录结构

```
src/main/java/com/lookgraph/
├── controller/     # REST API 入口
├── service/        # 业务逻辑
├── parser/         # AST 解析器
├── vector/         # ChromaDB 向量服务
├── repository/     # 数据访问层
├── config/         # 配置类
└── dto/            # 数据传输对象
```

---

## Claude Code 集成

### 安装说明

```bash
cd init
python3 install.py
```

安装脚本会自动：
- 复制所有钩子脚本到 `~/.claude/hooks/lookgraph/`
- 安装模式切换 skills 到 `<project>/.claude/skills/`（项目级别）
- 创建 `~/.claude/CLAUDE.md` 让 Claude 识别工具
- 配置权限和环境变量

**卸载**：
```bash
cd init
python3 uninstall.py
```

### 两种工作模式

#### 🔍 LookGraph 模式 (`/look_graph`)

进入此模式后，Claude 会优先使用 LookGraph API 进行代码分析：

| 传统工具 | LookGraph 替代 | 优势 |
|---------|---------------|------|
| `Grep` | `semantic_search.py` | 按业务意图搜索，不是关键词 |
| `Read` | `class_context.py` | 包含依赖关系和业务注释 |
| 手动追踪 | `method_callchain.py` | 自动展示调用链 |
| 手动查找 | `impact_analysis.py` | 精确影响范围分析 |

**适用场景**：
- 分析陌生代码库
- 理解复杂业务逻辑
- 追踪依赖和调用链
- 评估变更影响
- 按业务意图搜索代码

**示例**：
```
/look_graph

"帮我理解这个项目"
"用户登录的逻辑在哪里？"
"修改这个方法会影响哪些地方？"
```

#### ⚪ 标准模式 (`/exit_look_graph`)

恢复使用标准 Claude Code 工具（`Grep`、`Glob`、`Read`）。

**适用场景**：
- 编辑配置文件（JSON、YAML、properties）
- 阅读文档（README、markdown）
- 搜索非代码模式（日志、错误消息）
- 非 Java/Kotlin 项目
- 项目未在 LookGraph 中初始化

### 典型使用对话

**LookGraph 模式对话示例**：

```
用户: /look_graph
Claude: LookGraph Mode is now ACTIVE 🔍

用户: 帮我理解这个项目
Claude: [自动运行 project_list.py 检查初始化]
       [如需要则运行 project_init.py]
       [运行 project_summary.py 获取概览]
       这是一个...项目，包含以下模块...

用户: 用户登录的逻辑在哪里？
Claude: [运行 semantic_search.py "用户登录" 10]
       [运行 class_context.py 获取详情]
       找到了 UserService 类...
```

**标准模式对话示例**：

```
用户: /exit_look_graph
Claude: LookGraph Mode is now INACTIVE ⚪

用户: 更新 application.properties
Claude: [使用 Read 工具]
       [使用 Edit 工具修改]
```

### 何时使用哪个模式

**✅ 使用 LookGraph 模式**：
- 🔍 **代码搜索**："查找订单处理逻辑"
- 📖 **理解代码**："这个类是做什么的？"
- 🔗 **追踪依赖**："调用了哪些方法？"
- ⚠️ **影响分析**："修改这个会影响哪里？"
- 🎯 **定位功能**："退款功能在哪个文件？"

**✅ 使用标准模式**：
- ⚙️ **配置文件**：JSON, YAML, properties
- 📝 **文档编辑**：README, markdown
- 🔍 **日志搜索**：搜索错误消息、日志模式
- 📦 **非 Java 项目**：Python, JavaScript（LookGraph 暂不支持）
- 🚫 **项目未初始化**：LookGraph 中没有这个项目

### 使用方式

#### 方式 1：模式切换（推荐）

```bash
# 进入 LookGraph 模式，Claude 自动使用图谱分析
/look_graph

# 提问
"初始化当前项目"
"搜索用户认证相关的代码"
"分析修改这个方法的影响"

# 退出模式
/exit_look_graph
```

#### 方式 2：直接对话

```
"用 lookgraph 初始化当前项目"
"用 lookgraph 搜索用户认证相关的代码"
"用 lookgraph 分析修改这个方法的影响"
```

#### 方式 3：命令行

```bash
python3 ~/.claude/hooks/lookgraph/project_list.py
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "query" 10
```

### 可用钩子脚本

#### 项目管理

| 脚本 | 功能 | 参数 |
|-----|------|------|
| `project_list.py` | 列出所有项目 | 无 |
| `project_init.py` | 初始化项目 | `<path> <name>` |
| `project_update.py` | 增量更新 | `<project_id>` |
| `project_summary.py` | 项目摘要 | `<project_id>` |

#### 语义检索

| 脚本 | 功能 | 参数 |
|-----|------|------|
| `semantic_search.py` | 语义搜索代码 | `<project_id> <query> [top_k]` |
| `semantic_annotate.py` | 创建/更新注释 | `--project-id ID --package PKG --class CLASS --type TYPE --content TEXT --source AI\|HUMAN` |
| `semantic_by_git.py` | Git Hash 查询 | `<commit_hash>` |
| `semantic_class_history.py` | 类注释历史 | `<package> <class>` |
| `semantic_method_history.py` | 方法注释历史 | `<package> <class> <method>` |

#### 结构查询

| 脚本 | 功能 | 参数 |
|-----|------|------|
| `class_relations.py` | 类关系 | `<class_id>` |
| `class_methods.py` | 类的方法列表 | `<class_id>` |
| `module_classes.py` | 模块的类列表 | `<module_id>` |
| `method_callchain.py` | 方法调用链 | `<method_id>` |
| `impact_analysis.py` | 影响分析 | `<entity_type> <entity_id>` |

#### 上下文获取

| 脚本 | 功能 | 参数 |
|-----|------|------|
| `class_context.py` | 类上下文 | `<class_id>` |
| `method_context.py` | 方法上下文 | `<method_id>` |

### 典型工作流

**场景 1：理解陌生代码库（LookGraph 模式）**

```bash
# 进入 LookGraph 模式
/look_graph

# 直接提问，Claude 自动执行以下步骤：
"帮我理解这个项目"

# Claude 会自动：
# 1. 检查项目是否初始化 (project_list.py)
# 2. 如需要则初始化 (project_init.py /path/to/project ProjectName)
# 3. 获取项目摘要 (project_summary.py <project_id>)
# 4. 解释项目架构

# 继续提问
"用户登录逻辑在哪里？"
# Claude 会使用 semantic_search.py 找到相关代码，
# 然后用 class_context.py 展示完整上下文
```

**场景 2：评估修改影响（LookGraph 模式）**

```bash
/look_graph

"我要修改 UserService.login 方法，会影响哪些地方？"

# Claude 会自动：
# 1. 语义搜索定位方法 (semantic_search.py)
# 2. 影响分析 (impact_analysis.py METHOD <method_id>)
# 3. 查看调用链 (method_callchain.py <method_id>)
# 4. 列出所有受影响的代码
```

**场景 3：理解业务逻辑（LookGraph 模式）**

```bash
/look_graph

"订单支付失败后的重试机制是怎么实现的？"

# Claude 会自动：
# 1. semantic_search.py 搜索 "支付失败重试"
# 2. class_context.py 获取相关类
# 3. method_context.py 获取方法实现
# 4. 解释逻辑
# 5. 运行 semantic_annotate.py 保存理解
```

**场景 4：配置文件编辑（标准模式）**

```bash
/exit_look_graph

"修改 application.yml 中的数据库连接"

# Claude 会使用标准工具：
# - Read 读取文件
# - Edit 修改配置
```

**场景 5：手动调用（无需模式切换）**

```bash
# 1. 初始化项目
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project MyProject

# 2. 获取项目摘要
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>

# 3. 语义搜索找到入口
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "用户登录" 5

# 4. 查看类上下文
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# 5. 创建业务注释
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
  --project-id <project_id> \
  --project-path /path/to/project \
  --package com.example \
  --class UserService \
  --type CLASS \
  --content "用户服务，处理注册、登录、权限验证" \
  --source AI
```

### LookGraph 模式的自动行为

当你在 LookGraph 模式下请求代码分析时，Claude 会自动：

1. **检查初始化**
   ```bash
   python3 ~/.claude/hooks/lookgraph/project_list.py
   ```

2. **如果未初始化，自动初始化**
   ```bash
   python3 ~/.claude/hooks/lookgraph/project_init.py $(pwd) ProjectName
   ```

3. **获取项目概览**
   ```bash
   python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>
   ```

4. **保存新理解**
   当 Claude 理解了某个类或方法的业务含义，会自动运行：
   ```bash
   python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \
     --project-id <id> \
     --package <pkg> \
     --class <class> \
     --type CLASS \
     --content "业务描述" \
     --source AI
   ```

### 常见问题

**Q: 如何知道当前在哪个模式？**

A: 
- 进入 LookGraph 模式时会显示：**LookGraph Mode is now ACTIVE 🔍**
- 退出时会显示：**LookGraph Mode is now INACTIVE ⚪**

**Q: 可以不用模式切换，直接使用吗？**

A: 可以！你随时可以明确要求：
```
"用 lookgraph 搜索用户登录"
"用标准工具读取 README"
```
模式只是改变默认行为。

**Q: LookGraph 模式会影响所有操作吗？**

A: 不会。只影响代码分析相关操作：
- 搜索代码
- 理解类/方法
- 追踪依赖
- 影响分析

配置文件、文档等仍然用标准工具。

**Q: 忘记退出 LookGraph 模式会怎样？**

A: 没关系。Claude 会智能判断：
- 如果你要编辑配置文件，会自动用标准工具
- 如果需要，会提醒你当前模式不适合

**Q: 如何重新安装？**

```bash
# 卸载
cd init && python3 uninstall.py

# 重新安装
cd init && python3 install.py
```

---

## 配置指南

### 环境变量

```bash
# Neo4j
NEO4J_PASSWORD=lookgraph123

# ChromaDB
CHROMA_URL=http://localhost:8000

# 嵌入模型（Ollama）
EMBEDDING_BASE_URL=http://localhost:11434
EMBEDDING_API_KEY=ollama

# MySQL
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=lookgraph
MYSQL_USERNAME=root
MYSQL_PASSWORD=

# LookGraph 服务
LOOKGRAPH_BASE_URL=http://localhost:8090
```

### application.yml

完整配置参考 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:lookgraph}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:}
  
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: ${NEO4J_PASSWORD:lookgraph123}

vector:
  chroma:
    base-url: ${CHROMA_URL:http://localhost:8000}
  
  embedding:
    base-url: ${EMBEDDING_BASE_URL:http://localhost:11434}
    api-key: ${EMBEDDING_API_KEY:ollama}
```

### Claude Code settings.json

```json
{
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8090"
  },
  "permissions": {
    "allowed": [
      {
        "tool": "Bash",
        "prompt": "python3 ~/.claude/hooks/lookgraph/*"
      }
    ]
  }
}
```

---

## API 参考

### 项目管理

```
POST   /api/project/init        # 初始化项目
GET    /api/project/list        # 列出所有项目
POST   /api/project/update      # 触发增量更新
GET    /api/project/summary     # 获取项目摘要
```

### 结构查询

```
GET    /api/structure/class/{id}/relation      # 类关系
GET    /api/structure/class/{id}/methods       # 类的方法
GET    /api/structure/module/{id}/classes      # 模块的类
GET    /api/structure/method/{id}/callchain    # 方法调用链
GET    /api/structure/impact/{type}/{id}       # 影响分析
```

### 语义检索

```
POST   /api/semantic/search                    # 语义搜索
GET    /api/semantic/git/{hash}                # Git Hash 查询
GET    /api/semantic/node/{id}                 # Neo4j 节点查询
GET    /api/semantic/class                     # 类注释历史
GET    /api/semantic/method                    # 方法注释历史
POST   /api/semantic                           # 创建注释
```

### 上下文获取

```
GET    /api/context/class/{id}                 # 类上下文
GET    /api/context/method/{id}                # 方法上下文
```

详细 API 文档请访问 Swagger UI：http://localhost:8090/swagger-ui.html

### 常用命令快速参考

```bash
# 列出所有项目
python3 ~/.claude/hooks/lookgraph/project_list.py

# 初始化项目
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project ProjectName

# 语义搜索
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "查询内容" 10

# 获取类上下文
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# 影响分析
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD <method_id>

# 获取项目摘要
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>

# 查看方法调用链
python3 ~/.claude/hooks/lookgraph/method_callchain.py <method_id>
```

---

## 业务语义版本管理

### 数据模型

MySQL `semantic_history` 表存储所有注释变更历史：

| 字段 | 说明 |
|-----|------|
| package_name, class_name, method_name | 实体标识 |
| type | 注释类型（CLASS/METHOD/FIELD/...） |
| content | 注释内容 |
| git_commit_hash | 关联的 Git commit |
| modified_by | 来源（AI/HUMAN） |
| modify_reason | 修改原因 |
| create_time | 创建时间 |

### 使用场景

1. **AI 持续优化**：系统自动生成初始注释，使用中不断纠正，每次变更都记录
2. **人工审核**：用户发现不准确的注释进行修正，系统记录修正原因
3. **版本追溯**：根据 Git commit hash 查找该版本的所有注释，对比演进过程

### 核心价值

- **持续演进**：业务语义随使用不断优化，越用越准确
- **完整追溯**：保留所有历史版本，可溯源每次变更
- **混合存储**：图数据库处理关系，关系数据库管理元数据

---

## 开发指南

### 添加新的 Hook Script

1. 在 `hook/` 目录创建 Python 脚本
2. 使用 `lookgraph_client.py` 调用 API
3. 在 `init/install.py` 的 `HOOKS` 列表中注册
4. 运行 `cd init && python3 install.py` 重新安装

**脚本模板**：

```python
#!/usr/bin/env python3
import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python script.py <arg>", file=sys.stderr)
        sys.exit(1)
    
    client = get_client()
    response = client.get(f"/api/endpoint/{sys.argv[1]}")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
```

### ID 格式说明

- **classId**: 全限定类名，如 `com.example.UserService`
- **methodId**: `{classId}#{方法名}({参数类型})`，如 `com.example.UserService#login(String,String)`
- **moduleId**: 模块目录名，如 `order-service`

### 故障排查

**服务无法启动**

```bash
# 检查端口占用
lsof -i :8090

# 查看服务日志
tail -f logs/application.log
```

**服务连接失败**

```bash
# 检查服务状态
curl http://localhost:8090/api/project/list

# 检查服务健康状态
curl http://localhost:8090/actuator/health
```

**Claude Code 无法连接**

```bash
# 检查服务
curl http://localhost:8090/api/project/list

# 检查环境变量
echo $LOOKGRAPH_BASE_URL

# 重新安装
cd init && python3 install.py
```

**Hook Scripts 不可用**

```bash
# 重新安装
cd init && python3 install.py

# 测试连接
cd hook && python3 test_connection.py

# 检查权限
chmod +x ~/.claude/hooks/lookgraph/*.py

# 安装依赖
pip3 install requests
```

**Skills 未识别**

```bash
# 检查 skills 文件（项目级别）
ls -la .claude/skills/look_graph/SKILL.md
ls -la .claude/skills/exit_look_graph/SKILL.md

# 重新安装
cd init && python3 install.py

# 重启 Claude Code
```

**注意**: Skills 必须安装在项目的 `.claude/skills/` 目录下，而不是全局的 `~/.claude/skills/`。

---

## 技术细节

### Skills 实现原理

Skills 文件位于项目目录：
- `<project>/.claude/skills/look_graph/SKILL.md`
- `<project>/.claude/skills/exit_look_graph/SKILL.md`

Skills 通过包含详细指令的 markdown 文件来改变 Claude 的行为优先级。当用户输入 `/look_graph` 时，Claude Code 加载对应的 skill 文件，并遵循其中的指令。

**目录结构**：每个 skill 是一个目录，包含 `SKILL.md` 文件：
```
.claude/skills/
├── look_graph/
│   └── SKILL.md
└── exit_look_graph/
    └── SKILL.md
```

### Hook 脚本架构

位于 `~/.claude/hooks/lookgraph/`，通过 Python 调用 LookGraph REST API。

**核心组件**：
- `lookgraph_client.py` - API 客户端封装
- `project_*.py` - 项目管理脚本
- `semantic_*.py` - 语义检索和注释脚本
- `class_*.py` / `method_*.py` - 结构查询脚本
- `impact_analysis.py` - 影响分析脚本

### 数据流

```
用户输入
  ↓
Claude (LookGraph 模式)
  ↓
选择工具：semantic_search.py
  ↓
调用 LookGraph API (http://localhost:8090)
  ↓
查询 Neo4j + ChromaDB
  ↓
返回结果 + 业务注释
  ↓
Claude 理解并回答
  ↓
(可选) 保存新注释到 MySQL
```

### 模式状态管理

- **无状态设计**: 模式不需要持久化存储
- **文档驱动**: 通过加载不同的 skill 文档来改变行为
- **显式切换**: 用户明确输入命令来切换模式

## 最佳实践

1. **新项目分析**：一开始就进入 LookGraph 模式，建立全局认知
2. **批量代码理解**：保持 LookGraph 模式，让 Claude 积累语义注释
3. **配置修改**：临时退出模式，改完再进入
4. **代码审查**：使用 LookGraph 模式，利用影响分析和调用链追踪
5. **文档编写**：退出模式，使用标准工具
6. **持续演进**：随着使用，LookGraph 的语义注释会越来越准确

---

## 许可证

与 LookGraph 项目使用相同的许可证。

## 贡献

欢迎提交 Issue 和 Pull Request。
