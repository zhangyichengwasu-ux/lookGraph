# LookGraph Hooks 快速开始指南

## 一、安装

### 1. 安装依赖
```bash
pip3 install requests
```

### 2. 运行安装脚本
```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/hook
python3 install.py
```

安装完成后，所有钩子脚本将被复制到 `~/.claude/hooks/lookgraph/` 目录。

## 二、使用方式

### 方式 1: 直接使用 Python 脚本

```bash
# 列出所有项目
python3 ~/.claude/hooks/lookgraph/project_list.py

# 语义搜索
python3 ~/.claude/hooks/lookgraph/semantic_search.py project123 "用户登录" 5

# 查看方法调用链
python3 ~/.claude/hooks/lookgraph/method_callchain.py method456
```

### 方式 2: 使用便捷的 Shell 包装器

```bash
# 将 lookgraph.sh 添加到 PATH 或创建别名
alias lg='/Users/zhangyicheng/Documents/GitHub/lookGraph/hook/lookgraph.sh'

# 使用简短命令
lg lg-list                           # 列出项目
lg lg-search proj123 "登录" 10       # 语义搜索
lg lg-callchain method456            # 查看调用链
lg help                              # 查看所有命令
```

### 方式 3: 在 Claude Code 中使用

直接在 Claude Code 对话中请求执行：

```
请运行 python3 ~/.claude/hooks/lookgraph/project_list.py 查看所有项目
```

或者：

```
帮我用 lookgraph 的语义搜索功能查找 "用户认证" 相关的代码
```

## 三、配置

### 修改 API 地址

如果 LookGraph 服务不在默认的 `http://localhost:8080`，可以：

#### 方法 1: 设置环境变量
```bash
export LOOKGRAPH_BASE_URL=http://your-server:port
```

#### 方法 2: 修改配置文件
编辑 `~/.claude/hooks/lookgraph/lookgraph_client.py`：
```python
def __init__(self, base_url: Optional[str] = None):
    self.base_url = base_url or os.getenv("LOOKGRAPH_BASE_URL", "http://your-server:port")
```

### 在 settings.json 中注册钩子

编辑 `~/.claude/settings.json`：

```json
{
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8080"
  },
  "customCommands": {
    "lookgraph": {
      "description": "LookGraph 代码分析工具",
      "commands": [
        {
          "name": "lg-search",
          "command": "python3 ~/.claude/hooks/lookgraph/semantic_search.py",
          "description": "语义搜索代码"
        }
      ]
    }
  }
}
```

## 四、钩子脚本参数说明

所有脚本都支持命令行参数，settings.json 中可以这样配置：

### 1. 固定参数
```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/project_summary.py my-project-id"
  }
}
```

### 2. 环境变量参数
```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/project_summary.py $PROJECT_ID"
  }
}
```

使用前设置：
```bash
export PROJECT_ID=my-project-id
```

### 3. 命令替换
```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/semantic_search.py $(cat .project-id) 'query' 5"
  }
}
```

### 4. 传递多个参数
```json
{
  "hooks": {
    "custom-event": "python3 ~/.claude/hooks/lookgraph/semantic_method_history.py com.example.service UserService login"
  }
}
```

## 五、完整的 settings.json 示例

```json
{
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8080",
    "DEFAULT_PROJECT_ID": "my-project"
  },
  "hooks": {
    "pre-commit": "python3 ~/.claude/hooks/lookgraph/impact_analysis.py CLASS $CLASS_ID",
    "project-init": "python3 ~/.claude/hooks/lookgraph/project_init.py $PWD $(basename $PWD)"
  },
  "customCommands": {
    "lookgraph": {
      "description": "LookGraph 代码分析和语义搜索工具",
      "baseUrl": "http://localhost:8080",
      "commands": [
        {
          "name": "lookgraph:project-list",
          "description": "获取所有项目",
          "usage": "lookgraph:project-list",
          "command": "python3 ~/.claude/hooks/lookgraph/project_list.py"
        },
        {
          "name": "lookgraph:semantic-search",
          "description": "语义搜索代码",
          "usage": "lookgraph:semantic-search <project_id> <query> [top_k]",
          "command": "python3 ~/.claude/hooks/lookgraph/semantic_search.py"
        },
        {
          "name": "lookgraph:impact",
          "description": "影响分析",
          "usage": "lookgraph:impact <entity_type> <entity_id>",
          "command": "python3 ~/.claude/hooks/lookgraph/impact_analysis.py"
        }
      ]
    }
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

## 六、所有可用命令

| 命令 | 说明 | 参数 |
|------|------|------|
| `project_list.py` | 列出所有项目 | 无 |
| `project_init.py` | 初始化项目 | `<project_path> <project_name>` |
| `project_update.py` | 触发增量更新 | `<project_id>` |
| `project_summary.py` | 项目摘要 | `<project_id>` |
| `semantic_search.py` | 语义搜索 | `<project_id> <query> [top_k]` |
| `semantic_by_git.py` | Git Hash 查询 | `<git_commit_hash>` |
| `semantic_by_node.py` | Node ID 查询 | `<neo4j_node_id>` |
| `semantic_class_history.py` | 类注释历史 | `<package> <class>` |
| `semantic_method_history.py` | 方法注释历史 | `<package> <class> <method>` |
| `class_relations.py` | 类关系 | `<class_id>` |
| `class_methods.py` | 类的方法列表 | `<class_id>` |
| `class_context.py` | 类上下文 | `<class_id>` |
| `module_classes.py` | 模块的类列表 | `<module_id>` |
| `method_callchain.py` | 方法调用链 | `<method_id>` |
| `method_context.py` | 方法上下文 | `<method_id>` |
| `impact_analysis.py` | 影响分析 | `<entity_type> <entity_id>` |

## 七、卸载

```bash
python3 /Users/zhangyicheng/Documents/GitHub/lookGraph/hook/uninstall.py
```

## 八、故障排查

### 问题：连接被拒绝
**解决：** 确保 LookGraph 服务正在运行
```bash
curl http://localhost:8080/api/project/list
```

### 问题：模块未找到
**解决：** 安装 requests
```bash
pip3 install requests
```

### 问题：权限错误
**解决：** 添加执行权限
```bash
chmod +x ~/.claude/hooks/lookgraph/*.py
```

### 问题：Claude Code 中看不到命令
**解决：** 检查 settings.json 配置是否正确，重启 Claude Code

## 九、开发新钩子

1. 复制模板脚本
2. 使用 `lookgraph_client.py` 的 `get_client()`
3. 在 `install.py` 的 `HOOKS` 列表中添加元数据
4. 重新运行 `python3 install.py`

示例模板：
```python
#!/usr/bin/env python3
import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python script.py <arg>", file=sys.stderr)
        sys.exit(1)
    
    arg = sys.argv[1]
    client = get_client()
    response = client.get(f"/api/endpoint/{arg}")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
```
