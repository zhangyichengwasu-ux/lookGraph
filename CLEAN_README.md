# LookGraph 数据清理脚本

清理 LookGraph 系统中 Neo4j、ChromaDB、MySQL 的所有测试数据。

## 脚本文件

- **clean.sh** - Bash 版本（推荐，无需额外依赖）
- **clean.py** - Python 版本（需要安装 Python 包）

## 快速使用

### Bash 版本（推荐）

```bash
# 清理所有数据库（需要确认）
./clean.sh

# 清理所有数据库（跳过确认）
./clean.sh -y

# 仅清理 MySQL
./clean.sh --mysql -y

# 仅清理 Neo4j 和 ChromaDB
./clean.sh --neo4j --chroma -y

# 查看帮助
./clean.sh --help
```

### Python 版本

```bash
# 安装依赖
pip install neo4j mysql-connector-python

# 清理所有数据库
python clean.py -y

# 仅清理特定数据库
python clean.py --mysql -y
python clean.py --neo4j --chroma -y
```

## 环境变量配置

如果你的数据库配置与默认值不同，可以设置环境变量：

```bash
# Neo4j
export NEO4J_URI="bolt://localhost:7687"
export NEO4J_USER="neo4j"
export NEO4J_PASSWORD="your_password"

# ChromaDB
export CHROMA_URL="http://localhost:8000"

# MySQL
export MYSQL_HOST="localhost"
export MYSQL_PORT="3306"
export MYSQL_USER="root"
export MYSQL_PASSWORD=""
export MYSQL_DATABASE="lookgraph"

# 运行清理
./clean.sh -y
```

## 清理内容

### Neo4j
- 删除所有节点和关系
- 包括：Project, Module, Class, Method 等所有图谱数据

### ChromaDB
- 删除所有集合（collections）
- 包括：code_semantics, project_overview 等向量数据

### MySQL
- 清空 semantic_history 表
- 删除所有业务语义注释数据

## 使用场景

### 1. 开发环境重置

```bash
# 清理所有数据，准备重新测试
./clean.sh -y
```

### 2. 集成测试前准备

```bash
# 确保测试环境干净
./clean.sh -y
mvn test
```

### 3. 只清理特定数据库

```bash
# 只清理 MySQL，保留图谱和向量数据
./clean.sh --mysql -y

# 只清理 Neo4j 和 ChromaDB，保留语义注释
./clean.sh --neo4j --chroma -y
```

## 安全提示

⚠️ **警告**：此脚本会删除所有数据，操作不可逆！

- 默认会要求确认，使用 `-y` 跳过确认
- 建议在**开发/测试环境**使用
- **生产环境**请谨慎使用

## 故障排除

### cypher-shell 未安装

```bash
# macOS
brew install cypher-shell

# 或使用 Neo4j Desktop
```

### ChromaDB 连接失败

```bash
# 确认 ChromaDB 正在运行
docker ps | grep chroma

# 或检查端口
curl http://localhost:8000/api/v2/heartbeat
```

### MySQL 连接失败

```bash
# 检查 MySQL 服务
mysql -u root -p -e "SELECT 1"

# 确认数据库存在
mysql -u root -p -e "SHOW DATABASES LIKE 'lookgraph'"
```

## 清理结果示例

```
============================================================
LookGraph 数据清理脚本
============================================================

将清理以下数据库:
  • Neo4j (图谱数据)
  • ChromaDB (向量数据)
  • MySQL (语义注释)

开始清理数据...

[Neo4j] 开始清理...
  发现 150 个节点, 320 个关系
  ✓ 已删除所有节点和关系
[Neo4j] 清理完成 ✓

[ChromaDB] 开始清理...
  发现 2 个集合
  ✓ 已删除集合: code_semantics
  ✓ 已删除集合: project_overview
[ChromaDB] 清理完成 ✓

[MySQL] 开始清理...
  发现 50 条记录
  ✓ 已删除 50 条记录
[MySQL] 清理完成 ✓

============================================================
清理结果总结
============================================================
✓ 所有数据已清理完成 (3/3)
```

## 与测试集成

在测试脚本中使用：

```bash
#!/bin/bash

# 清理旧数据
./clean.sh -y

# 启动应用
./run.sh &
APP_PID=$!

# 等待启动
sleep 5

# 运行测试
mvn test

# 停止应用
kill $APP_PID
```
