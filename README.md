# LookGraph — 代码地图助手

> 代码结构图谱 + 业务语义向量双引擎，让 AI 无需加载全量代码即可理解大型项目。

## 项目意义

大型项目动辄数十万行代码，直接喂给 AI 既超出上下文限制，又浪费 Token。LookGraph 将代码库预处理为两类索引：

- **结构图谱（Neo4j）**：类、方法、模块之间的继承、调用、依赖关系，还原代码骨架。
- **语义向量（ChromaDB）**：注释与业务描述的向量索引，支持自然语言检索代码含义。

AI 只需按需查询切片，而不是读取整个项目，上下文开销大幅降低，代码分析、重构、问题排查更精准。

---

## 前置条件

### 1. 运行时

| 依赖 | 版本要求 |
|------|----------|
| Java | 21+ |
| Maven | 3.9+ |
| Docker & Docker Compose | 任意近期版本 |
| Python | 3.10+（Python 项目解析时需要） |

### 2. 基础设施（Neo4j + ChromaDB）

一键启动：

```bash
docker compose up -d
```

默认配置：

| 服务 | 地址 | 账号/密码 |
|------|------|-----------|
| Neo4j Browser | http://localhost:7474 | neo4j / lookgraph123 |
| Neo4j Bolt | bolt://localhost:7687 | — |
| ChromaDB | http://localhost:8000 | — |

### 3. 嵌入式模型

默认使用 [Ollama](https://ollama.com) 本地运行 `nomic-embed-text`：

```bash
ollama pull nomic-embed-text
```

也可换用任意兼容 OpenAI embeddings 接口的服务，通过环境变量覆盖：

```bash
EMBEDDING_BASE_URL=https://api.openai.com/v1
EMBEDDING_API_KEY=sk-xxx
```

### 4. 快速安装脚本

```bash
# 克隆项目
git clone https://github.com/yourname/lookGraph.git && cd lookGraph

# 启动基础设施
docker compose up -d

# 拉取嵌入模型（Ollama）
ollama pull nomic-embed-text

# 构建并启动
mvn package -DskipTests
java -jar target/look-graph-1.0.0-SNAPSHOT.jar
```

服务启动后访问 Swagger UI：http://localhost:8090/swagger-ui.html

---

## 项目架构

```
┌──────────────────────────────────────────┐
│              应用交互层                   │
│      Claude / 第三方工具  HTTP API         │
└───────────────────┬──────────────────────┘
                    │
┌───────────────────▼──────────────────────┐
│              核心服务层                   │
│  ProjectService        — 项目扫描与初始化 │
│  StructureQueryService — 图结构查询       │
│  SemanticSearchService — 向量语义检索     │
│  ContextAssemblyService — 上下文组装      │
│  ImpactAnalysisService — 变更影响分析     │
│  SummaryService        — 项目摘要生成     │
└──────────┬────────────────┬──────────────┘
           │                │
┌──────────▼───────┐ ┌──────▼──────────────┐
│  Neo4j 图数据库  │ │  ChromaDB 向量数据库  │
│ 类/方法/调用关系  │ │  注释/业务语义向量    │
└──────────────────┘ └─────────────────────┘
           ▲                ▲
┌──────────┴────────────────┴──────────────┐
│              代码解析层                   │
│  JavaAstParser   — Java AST 解析          │
│  PythonAstParser — Python AST 解析        │
└──────────────────────────────────────────┘
```

### 核心 API

| 端点 | 功能 |
|------|------|
| `POST /api/project/init` | 扫描目录，构建图谱和向量索引 |
| `GET  /api/structure/class/{name}` | 查询类的关系视图 |
| `GET  /api/structure/callchain/{method}` | 查询方法调用链 |
| `POST /api/semantic/search` | 自然语言语义检索代码 |
| `GET  /api/context/{class}` | 获取类的完整上下文切片 |
| `GET  /api/structure/impact/{method}` | 分析变更影响范围 |

### 目录结构

```
src/main/java/com/lookgraph/
├── controller/     # REST 入口（Project / Structure / Semantic / Context）
├── service/        # 业务逻辑
├── parser/         # AST 解析（Java / Python）
├── vector/         # ChromaDB 向量索引服务
├── config/         # Neo4j、异步、扫描配置
├── dto/            # 请求 / 响应 DTO
└── common/         # 枚举、异常、工具类
```

---

## 配置说明

关键环境变量（均有默认值，开箱可用）：

```bash
NEO4J_PASSWORD=lookgraph123
CHROMA_URL=http://localhost:8000
EMBEDDING_BASE_URL=http://localhost:11434   # Ollama 默认地址
EMBEDDING_API_KEY=ollama
```

完整配置见 `src/main/resources/application.yml`。
