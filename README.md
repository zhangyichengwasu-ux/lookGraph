# LookGraph — 代码地图助手

> 代码结构图谱 + 业务语义向量双引擎，让 AI 无需加载全量代码即可理解大型项目。

## 项目简介

大型项目动辄数十万行代码，直接喂给 AI 既超出上下文限制，又浪费 Token。LookGraph 将代码库预处理为三类索引：

- **结构图谱（Neo4j）**: 类、方法、模块之间的继承、调用、依赖关系，还原代码骨架
- **语义向量（ChromaDB）**: 注释与业务描述的向量索引，支持自然语言检索代码含义
- **版本历史（MySQL）**: 业务注释的完整演进历史，支持 AI/人工持续优化

AI 只需按需查询切片，而不是读取整个项目，上下文开销大幅降低，代码分析、重构、问题排查更精准。

---

## 快速开始

### 1. 环境要求

| 依赖 | 版本要求 |
|------|----------|
| Java | 21+ |
| Maven | 3.9+ |
| Docker & Docker Compose | 任意近期版本 |
| Python | 3.10+（用于 Hook Scripts） |

### 2. 启动基础设施

```bash
# 启动 Neo4j + ChromaDB + MySQL
docker compose up -d

# 拉取嵌入模型（使用 Ollama）
ollama pull nomic-embed-text
```

默认服务地址：

| 服务 | 地址 | 账号/密码 |
|------|------|-----------|
| Neo4j Browser | http://localhost:7474 | neo4j / lookgraph123 |
| Neo4j Bolt | bolt://localhost:7687 | — |
| ChromaDB | http://localhost:8000 | — |
| MySQL | localhost:3306 | root / （空密码） |

### 3. 启动 LookGraph 服务

```bash
# 构建项目
mvn package -DskipTests

# 启动服务
java -jar target/look-graph-1.0.0-SNAPSHOT.jar
```

服务启动后访问：
- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **健康检查**: http://localhost:8090/actuator/health

### 4. 安装 Claude Code 钩子（可选）

```bash
cd hook
python3 install.py
```

安装后，Claude 可直接调用 LookGraph 的所有分析能力。

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

### 安装 Hook Scripts

```bash
cd hook
python3 install.py
```

安装脚本会自动：
- 复制所有钩子脚本到 `~/.claude/hooks/lookgraph/`
- 创建 `~/.claude/CLAUDE.md` 让 Claude 识别工具
- 配置权限和环境变量

### 使用方式

**方式 1：直接对话**

```
"用 lookgraph 初始化当前项目"
"搜索用户认证相关的代码"
"分析修改这个方法的影响"
```

**方式 2：命令行**

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

**场景 1：理解陌生代码库**

```bash
# 1. 初始化项目
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project MyProject

# 2. 获取项目摘要
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>

# 3. 语义搜索找到入口
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "用户登录" 5

# 4. 查看类上下文
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>
```

**场景 2：评估修改影响**

```bash
# 1. 影响分析
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD <method_id>

# 2. 查看调用链
python3 ~/.claude/hooks/lookgraph/method_callchain.py <method_id>
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
3. 在 `install.py` 的 `HOOKS` 列表中注册
4. 运行 `python3 install.py` 重新安装

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

**服务连接失败**

```bash
# 检查服务状态
curl http://localhost:8090/api/project/list

# 检查 Docker 容器
docker compose ps
```

**Hook Scripts 不可用**

```bash
# 测试连接
cd hook && python3 test_connection.py

# 检查权限
chmod +x ~/.claude/hooks/lookgraph/*.py

# 安装依赖
pip3 install requests
```

---

## 许可证

与 LookGraph 项目使用相同的许可证。

## 贡献

欢迎提交 Issue 和 Pull Request。
