# LookGraph Hooks 项目总结

## ✅ 已完成的工作

### 1. 核心组件 (4 个文件)

#### lookgraph_client.py
- 统一的 HTTP 客户端库
- 支持 GET/POST 请求
- 自动错误处理
- 环境变量配置支持 (`LOOKGRAPH_BASE_URL`)
- JSON 格式化输出

#### install.py
- 自动安装脚本
- 创建 `~/.claude/hooks/lookgraph/` 目录
- 复制所有脚本并设置执行权限
- 更新 `~/.claude/settings.json`
- 生成安装文档

#### uninstall.py
- 卸载脚本
- 删除钩子目录
- 清理 settings.json 配置
- 交互式确认

#### test_connection.py
- 连接测试工具
- 验证 API 可用性
- 显示项目列表
- 故障诊断提示

### 2. Hook 脚本 (17 个)

#### 项目管理 (4 个)
- `project_list.py` - 列出所有项目
- `project_init.py` - 初始化项目
- `project_update.py` - 触发增量更新
- `project_summary.py` - 获取项目摘要

#### 语义检索 (5 个)
- `semantic_search.py` - 语义搜索代码
- `semantic_by_git.py` - 根据 Git Hash 查询
- `semantic_by_node.py` - 根据 Neo4j Node ID 查询
- `semantic_class_history.py` - 类注释历史
- `semantic_method_history.py` - 方法注释历史

#### 结构查询 (5 个)
- `class_relations.py` - 查询类关系
- `class_methods.py` - 查询类的方法
- `module_classes.py` - 查询模块的类
- `method_callchain.py` - 查询方法调用链
- `impact_analysis.py` - 影响分析

#### 上下文获取 (2 个)
- `class_context.py` - 获取类上下文
- `method_context.py` - 获取方法上下文

#### 辅助工具 (1 个)
- `lookgraph.sh` - Shell 包装器，提供简短命令别名

### 3. 文档 (3 个)

- `README.md` - 完整中文文档
- `QUICKSTART.md` - 快速开始指南
- `INDEX.md` - 文件索引和配置参考

## 📊 统计

- **总文件数**: 24 个
- **Python 脚本**: 20 个
- **Shell 脚本**: 1 个
- **文档文件**: 3 个
- **代码总量**: ~30KB

## 🎯 核心特性

### 1. 参数传递机制

所有脚本支持命令行参数，settings.json 中可以通过多种方式传递：

```json
// 固定参数
"command": "python3 script.py fixed_arg"

// 环境变量
"command": "python3 script.py $ENV_VAR"

// 命令替换
"command": "python3 script.py $(cat .config)"

// 多个参数
"command": "python3 script.py arg1 arg2 arg3"
```

### 2. 灵活的配置

```json
{
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8080"
  },
  "customCommands": {
    "lookgraph": {
      "commands": [...]
    }
  },
  "permissions": {
    "allowed": [
      {"tool": "Bash", "prompt": "python3 ~/.claude/hooks/lookgraph/*"}
    ]
  }
}
```

### 3. 三种使用方式

1. **直接调用**: `python3 ~/.claude/hooks/lookgraph/script.py args`
2. **Shell 包装器**: `./lookgraph.sh lg-command args`
3. **Claude Code 集成**: 直接在对话中请求执行

## 🚀 使用流程

### 安装
```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/hook
python3 test_connection.py    # 测试连接
python3 install.py             # 安装钩子
```

### 使用
```bash
# 测试
python3 ~/.claude/hooks/lookgraph/project_list.py

# 使用包装器
./lookgraph.sh lg-list

# 在 Claude Code 中
# "请运行 lookgraph 查询所有项目"
```

### 卸载
```bash
python3 uninstall.py
```

## 🔧 技术亮点

### 1. 统一的客户端库
所有脚本共享 `lookgraph_client.py`，确保：
- 代码复用
- 统一的错误处理
- 一致的配置管理

### 2. 完善的错误处理
```python
try:
    response = client.get(endpoint)
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    sys.exit(1)
```

### 3. 标准化的输出格式
```python
def format_output(response):
    if response.get("code") == 0:
        return json.dumps(response.get("data"), indent=2, ensure_ascii=False)
    else:
        return json.dumps(response, indent=2, ensure_ascii=False)
```

### 4. 可执行权限
所有脚本都设置了执行权限 (`chmod +x`)，可以直接运行。

## 📝 settings.json 配置要点

### 关键发现：钩子参数支持

Claude Code 的 hooks 系统支持以下参数传递方式：

1. **直接拼接参数到命令字符串**
   ```json
   "command": "python3 script.py param1 param2"
   ```

2. **使用 Shell 变量**
   ```json
   "command": "python3 script.py $VAR"
   ```

3. **使用命令替换**
   ```json
   "command": "python3 script.py $(command)"
   ```

4. **引用 env 配置**
   ```json
   "env": {"BASE_URL": "http://..."},
   "hooks": {"event": "python3 script.py $BASE_URL"}
   ```

### 推荐配置结构

```json
{
  "env": {
    "LOOKGRAPH_BASE_URL": "http://localhost:8080",
    "DEFAULT_PROJECT_ID": "my-project"
  },
  "customCommands": {
    "lookgraph": {
      "description": "LookGraph tools",
      "commands": [
        {
          "name": "lg-search",
          "command": "python3 ~/.claude/hooks/lookgraph/semantic_search.py",
          "description": "Semantic code search"
        }
      ]
    }
  },
  "permissions": {
    "allowed": [
      {"tool": "Bash", "prompt": "python3 ~/.claude/hooks/lookgraph/*"}
    ]
  }
}
```

## 🎓 设计思路

### 为什么选择 Python 脚本？

1. **跨平台**: Python 在 macOS/Linux/Windows 都可用
2. **易维护**: 代码清晰，易于扩展
3. **丰富的库**: requests 等 HTTP 库成熟稳定
4. **CLI 友好**: 易于接受参数，输出格式化

### 为什么需要包装器？

1. **简化命令**: `lg-search` 比完整路径更简洁
2. **统一入口**: 所有命令通过一个脚本
3. **帮助文档**: 内置 help 命令
4. **路径管理**: 自动处理脚本路径

### 为什么分离客户端库？

1. **代码复用**: 避免重复的 HTTP 逻辑
2. **统一配置**: 集中管理 base_url
3. **易于测试**: 独立的客户端易于单元测试
4. **维护性**: 修改 API 逻辑只需改一处

## 📂 文件位置

### 开发目录
```
/Users/zhangyicheng/Documents/GitHub/lookGraph/hook/
```

### 安装目录
```
~/.claude/hooks/lookgraph/
```

### 配置文件
```
~/.claude/settings.json
```

## 🔄 后续扩展

### 添加新钩子的步骤

1. 在 `hook/` 创建新脚本，参考模板
2. 在 `install.py` 的 `HOOKS` 列表添加元数据
3. 运行 `python3 install.py` 重新安装
4. 更新文档

### 模板
```python
#!/usr/bin/env python3
"""
Hook: Description
Usage: python script.py <args>
"""
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

## ✨ 总结

成功创建了一套完整的 LookGraph Claude Code Hooks 系统：

- ✅ 17 个功能钩子脚本，覆盖所有 API 端点
- ✅ 完整的安装/卸载/测试工具
- ✅ 详细的中文文档
- ✅ 支持参数传递的 settings.json 配置
- ✅ Shell 包装器提供便捷命令
- ✅ 统一的客户端库和错误处理
- ✅ 所有脚本可执行

**现在可以开始使用了！**

```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/hook
python3 test_connection.py  # 测试
python3 install.py          # 安装
```
