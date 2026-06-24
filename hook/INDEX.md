# LookGraph Hooks 文件清单

## 📁 文件结构

```
hook/
├── README.md                      # 完整文档（中文）
├── QUICKSTART.md                  # 快速开始指南
├── install.py                     # 安装脚本
├── uninstall.py                   # 卸载脚本
├── test_connection.py             # 连接测试脚本
├── lookgraph.sh                   # Shell 包装器（便捷命令）
├── lookgraph_client.py            # API 客户端库（被所有脚本使用）
│
├── 项目管理钩子:
│   ├── project_list.py           # 列出所有项目
│   ├── project_init.py           # 初始化项目
│   ├── project_update.py         # 触发增量更新
│   └── project_summary.py        # 获取项目摘要
│
├── 语义检索钩子:
│   ├── semantic_search.py        # 语义搜索代码
│   ├── semantic_by_git.py        # 根据 Git Hash 查询
│   ├── semantic_by_node.py       # 根据 Node ID 查询
│   ├── semantic_class_history.py # 类注释历史
│   └── semantic_method_history.py# 方法注释历史
│
├── 结构查询钩子:
│   ├── class_relations.py        # 查询类关系
│   ├── class_methods.py          # 查询类的方法
│   ├── module_classes.py         # 查询模块的类
│   ├── method_callchain.py       # 查询方法调用链
│   └── impact_analysis.py        # 影响分析
│
└── 上下文获取钩子:
    ├── class_context.py           # 获取类上下文
    └── method_context.py          # 获取方法上下文
```

## 🚀 快速开始

### 1. 测试连接
```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/hook
python3 test_connection.py
```

### 2. 安装钩子
```bash
python3 install.py
```

### 3. 使用钩子
```bash
# 方式 1: 直接调用
python3 ~/.claude/hooks/lookgraph/project_list.py

# 方式 2: 使用包装器
./lookgraph.sh lg-list

# 方式 3: 在 Claude Code 中
# 直接告诉 Claude 运行相应的命令
```

## 📊 统计信息

- **总脚本数**: 17 个钩子脚本
- **工具脚本**: 5 个（install, uninstall, test, wrapper, client）
- **文档文件**: 3 个（README, QUICKSTART, INDEX）

## 🔧 核心组件

### lookgraph_client.py
所有钩子脚本的共享 HTTP 客户端，提供：
- 统一的 API 调用接口
- 自动错误处理
- 环境变量配置支持
- JSON 格式化输出

### install.py
安装脚本，执行以下操作：
1. 创建 `~/.claude/hooks/lookgraph/` 目录
2. 复制所有钩子脚本
3. 设置执行权限
4. 更新 `~/.claude/settings.json`
5. 生成 README 文档

### lookgraph.sh
Shell 包装器，提供简短的命令别名：
- `lg-list` → `project_list.py`
- `lg-search` → `semantic_search.py`
- `lg-impact` → `impact_analysis.py`
- 等等...

## 📝 配置 settings.json

### 基本配置
```json
{
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8080"
  }
}
```

### 钩子参数传递方式

#### 1. 固定参数
```json
{
  "hooks": {
    "my-event": "python3 ~/.claude/hooks/lookgraph/project_summary.py my-project-id"
  }
}
```

#### 2. 环境变量
```json
{
  "hooks": {
    "my-event": "python3 ~/.claude/hooks/lookgraph/project_summary.py $PROJECT_ID"
  }
}
```

#### 3. 命令替换
```json
{
  "hooks": {
    "my-event": "python3 ~/.claude/hooks/lookgraph/semantic_search.py $(cat .project-id) 'query' 5"
  }
}
```

#### 4. 多个参数
```json
{
  "hooks": {
    "my-event": "python3 ~/.claude/hooks/lookgraph/semantic_method_history.py com.example UserService login"
  }
}
```

### 自动权限配置
```json
{
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

## 🎯 使用场景示例

### 场景 1: 代码搜索
```bash
# 查找所有与 "用户认证" 相关的代码
python3 ~/.claude/hooks/lookgraph/semantic_search.py project123 "用户认证" 10
```

### 场景 2: 影响分析
```bash
# 分析修改某个方法的影响范围
python3 ~/.claude/hooks/lookgraph/impact_analysis.py METHOD method456
```

### 场景 3: 调用链追踪
```bash
# 查看方法的完整调用链
python3 ~/.claude/hooks/lookgraph/method_callchain.py method456
```

### 场景 4: 上下文获取
```bash
# 获取类的精简上下文，用于 AI 分析
python3 ~/.claude/hooks/lookgraph/class_context.py class123
```

## 🔍 API 端点映射

| 钩子脚本 | API 端点 | HTTP 方法 |
|----------|----------|-----------|
| project_list.py | /api/project/list | GET |
| project_init.py | /api/project/init | POST |
| project_update.py | /api/project/update | POST |
| project_summary.py | /api/project/summary | GET |
| semantic_search.py | /api/semantic/search | POST |
| semantic_by_git.py | /api/semantic/git/{hash} | GET |
| semantic_by_node.py | /api/semantic/node/{id} | GET |
| semantic_class_history.py | /api/semantic/class | GET |
| semantic_method_history.py | /api/semantic/method | GET |
| class_relations.py | /api/structure/class/{id}/relation | GET |
| class_methods.py | /api/structure/class/{id}/methods | GET |
| module_classes.py | /api/structure/module/{id}/classes | GET |
| method_callchain.py | /api/structure/method/{id}/callchain | GET |
| impact_analysis.py | /api/structure/impact/{type}/{id} | GET |
| class_context.py | /api/context/class/{id} | GET |
| method_context.py | /api/context/method/{id} | GET |

## 📚 文档链接

- **完整文档**: [README.md](README.md)
- **快速开始**: [QUICKSTART.md](QUICKSTART.md)
- **LookGraph 系统**: [../lookGraph.md](../lookGraph.md)

## 🛠️ 维护

### 添加新钩子
1. 创建新的 Python 脚本，参考现有脚本模板
2. 在 `install.py` 的 `HOOKS` 列表中添加元数据
3. 更新本文档
4. 运行 `python3 install.py` 重新安装

### 更新现有钩子
1. 修改对应的 Python 脚本
2. 运行 `python3 install.py` 重新安装
3. 或直接修改 `~/.claude/hooks/lookgraph/` 中的文件

### 调试
```bash
# 启用详细输出
export DEBUG=1
python3 ~/.claude/hooks/lookgraph/project_list.py

# 使用其他 API 地址
export LOOKGRAPH_BASE_URL=http://dev-server:8080
python3 ~/.claude/hooks/lookgraph/project_list.py
```

## ❓ 常见问题

**Q: 钩子脚本在 Claude Code 中不可用？**  
A: 检查 `~/.claude/settings.json` 配置，确保路径正确，重启 Claude Code。

**Q: 连接被拒绝？**  
A: 确保 LookGraph 服务正在运行，可用 `python3 test_connection.py` 测试。

**Q: 如何传递带空格的参数？**  
A: 使用引号：`python3 script.py "arg with spaces"`

**Q: 如何在 settings.json 中使用动态参数？**  
A: 使用环境变量 `$VAR` 或命令替换 `$(command)`。

## 📞 支持

如有问题，请查看：
1. [README.md](README.md) - 完整文档
2. [QUICKSTART.md](QUICKSTART.md) - 快速开始指南
3. 运行 `python3 test_connection.py` - 诊断连接问题

---

**最后更新**: 2026-06-24
**版本**: 1.0.0
