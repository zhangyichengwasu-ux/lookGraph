# LookGraph Claude Code Hooks

这是一套用于 Claude Code 与 LookGraph API 集成的 Python 钩子脚本。

## 功能概述

LookGraph 是一个代码分析和语义搜索系统，这些钩子脚本让你可以在 Claude Code 中直接调用 LookGraph 的各种功能。

## 安装

### 前置要求

- Python 3.6+
- requests 库：`pip install requests`
- LookGraph 服务运行在 http://localhost:8080 (可配置)

### 安装步骤

1. 确保 LookGraph 服务正在运行
2. 运行安装脚本：

```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/hook
python3 install.py
```

安装脚本会：
- 创建 `~/.claude/hooks/lookgraph/` 目录
- 复制所有钩子脚本到该目录
- 设置执行权限
- 在 `~/.claude/settings.json` 中注册钩子
- 生成 README 文档

## 配置

### 修改 API 地址

如果你的 LookGraph 服务不在默认地址，可以通过环境变量配置：

```bash
export LOOKGRAPH_BASE_URL=http://your-server:port
```

或者修改 `~/.claude/hooks/lookgraph/lookgraph_client.py` 中的默认值。

## 可用命令

### 项目管理

#### lookgraph:project-list
列出所有项目

```bash
python3 ~/.claude/hooks/lookgraph/project_list.py
```

#### lookgraph:project-init
初始化新项目

```bash
python3 ~/.claude/hooks/lookgraph/project_init.py <project_path> <project_name>
```

示例：
```bash
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/repo MyProject
```

#### lookgraph:project-update
触发增量更新

```bash
python3 ~/.claude/hooks/lookgraph/project_update.py <project_id>
```

#### lookgraph:project-summary
获取项目摘要

```bash
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>
```

### 语义检索

#### lookgraph:semantic-search
语义搜索代码

```bash
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> <query> [top_k]
```

示例：
```bash
python3 ~/.claude/hooks/lookgraph/semantic_search.py proj123 "用户认证逻辑" 10
```

#### lookgraph:semantic-by-git
根据 Git commit hash 查询业务注释

```bash
python3 ~/.claude/hooks/lookgraph/semantic_by_git.py <git_commit_hash>
```

#### lookgraph:semantic-by-node
根据 Neo4j 节点 ID 查询业务注释

```bash
python3 ~/.claude/hooks/lookgraph/semantic_by_node.py <neo4j_node_id>
```

#### lookgraph:semantic-class-history
查询类的业务注释历史

```bash
python3 ~/.claude/hooks/lookgraph/semantic_class_history.py <package_name> <class_name>
```

示例：
```bash
python3 ~/.claude/hooks/lookgraph/semantic_class_history.py com.example.service UserService
```

#### lookgraph:semantic-method-history
查询方法的业务注释历史

```bash
python3 ~/.claude/hooks/lookgraph/semantic_method_history.py <package_name> <class_name> <method_name>
```

示例：
```bash
python3 ~/.claude/hooks/lookgraph/semantic_method_history.py com.example.service UserService login
```

### 结构查询

#### lookgraph:class-relations
查询类的关联关系

```bash
python3 ~/.claude/hooks/lookgraph/class_relations.py <class_id>
```

#### lookgraph:class-methods
查询类下所有方法

```bash
python3 ~/.claude/hooks/lookgraph/class_methods.py <class_id>
```

#### lookgraph:module-classes
查询模块下所有类

```bash
python3 ~/.claude/hooks/lookgraph/module_classes.py <module_id>
```

#### lookgraph:method-callchain
查询方法调用链路

```bash
python3 ~/.claude/hooks/lookgraph/method_callchain.py <method_id>
```

### 上下文获取

#### lookgraph:class-context
获取类的精简上下文

```bash
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>
```

#### lookgraph:method-context
获取方法的精简上下文

```bash
python3 ~/.claude/hooks/lookgraph/method_context.py <method_id>
```

### 影响分析

#### lookgraph:impact-analysis
查询代码修改的影响范围

```bash
python3 ~/.claude/hooks/lookgraph/impact_analysis.py <entity_type> <entity_id>
```

entity_type 可选值：CLASS, METHOD, MODULE

示例：
```bash
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD method123
```

## 在 Claude Code 中使用

### 方式 1: 直接调用命令

你可以直接在 Claude Code 中运行这些脚本：

```
请运行 python3 ~/.claude/hooks/lookgraph/project_list.py
```

### 方式 2: 通过 Bash 工具

Claude Code 可以使用 Bash 工具执行命令：

```
使用 lookgraph 查询项目列表
```

### 方式 3: 集成到工作流

你可以在 `~/.claude/settings.json` 中配置钩子，让某些操作自动触发：

```json
{
  "hooks": {
    "pre-commit": "python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD $METHOD_ID"
  }
}
```

## settings.json 配置说明

### 钩子参数支持

在 `settings.json` 中，钩子命令可以包含参数。有几种方式：

#### 1. 硬编码参数

```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/project_summary.py my-project-id"
  }
}
```

#### 2. 环境变量

```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/project_summary.py $PROJECT_ID"
  }
}
```

需要先设置环境变量：
```bash
export PROJECT_ID=my-project-id
```

#### 3. 命令替换

```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/semantic_search.py $(cat .project-id) 'search query' 5"
  }
}
```

#### 4. 自定义命令别名

```json
{
  "customCommands": {
    "lookgraph": {
      "description": "LookGraph analysis tools",
      "commands": [
        {
          "name": "lg-search",
          "command": "python3 ~/.claude/hooks/lookgraph/semantic_search.py"
        }
      ]
    }
  }
}
```

### 完整配置示例

```json
{
  "hooks": {
    "pre-commit": "python3 ~/.claude/hooks/lookgraph/impact_analysis.py CLASS $CLASS_ID",
    "post-read": "python3 ~/.claude/hooks/lookgraph/method_context.py $METHOD_ID"
  },
  "customCommands": {
    "lookgraph": {
      "description": "LookGraph code analysis and semantic search tools",
      "baseUrl": "http://localhost:8080",
      "commands": [
        {
          "name": "lookgraph:project-list",
          "description": "Get all projects in LookGraph",
          "usage": "lookgraph:project-list",
          "command": "python3 ~/.claude/hooks/lookgraph/project_list.py"
        },
        {
          "name": "lookgraph:semantic-search",
          "description": "Search code semantically",
          "usage": "lookgraph:semantic-search <project_id> <query> [top_k]",
          "command": "python3 ~/.claude/hooks/lookgraph/semantic_search.py"
        }
      ]
    }
  },
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8080",
    "DEFAULT_PROJECT_ID": "my-project"
  }
}
```

## 卸载

如果需要卸载，运行：

```bash
rm -rf ~/.claude/hooks/lookgraph
```

然后手动从 `~/.claude/settings.json` 中移除 `customCommands.lookgraph` 部分。

## 故障排查

### 问题 1: 连接失败

确保 LookGraph 服务正在运行：
```bash
curl http://localhost:8080/api/project/list
```

### 问题 2: 权限错误

确保脚本有执行权限：
```bash
chmod +x ~/.claude/hooks/lookgraph/*.py
```

### 问题 3: Python 模块未找到

安装 requests 库：
```bash
pip install requests
```

或使用 Python 3 的 pip：
```bash
pip3 install requests
```

## 开发

### 添加新的钩子

1. 在 `hook/` 目录创建新的 Python 脚本
2. 使用 `lookgraph_client.py` 中的 `get_client()` 获取客户端
3. 在 `install.py` 的 `HOOKS` 列表中添加新钩子的元数据
4. 重新运行 `python3 install.py`

### 脚本模板

```python
#!/usr/bin/env python3
"""
Hook: Description
Usage: python script_name.py <args>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python script_name.py <arg>", file=sys.stderr)
        sys.exit(1)

    arg = sys.argv[1]
    client = get_client()
    response = client.get(f"/api/endpoint/{arg}")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
```

## 许可证

与 LookGraph 项目使用相同的许可证。
