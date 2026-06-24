# LookGraph Claude Code Hooks 安装

## 概述

此目录包含 LookGraph 的 Claude Code hooks 安装脚本。

## 文件说明

- `install.py` - 安装脚本，将 hooks 安装到 `~/.claude/hooks/lookgraph/`
- `uninstall.py` - 卸载脚本，清理已安装的 hooks
- `CLAUDE_MD_TEMPLATE.md` - CLAUDE.md 文档模板（449 行）

## 快速安装

```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/init
python3 install.py
```

**安装内容**:
- ✅ 复制所有 hook 脚本到 `~/.claude/hooks/lookgraph/`
- ✅ 安装模式切换 skills 到 `<project>/.claude/skills/`（项目级别）
  - `look_graph/SKILL.md` - 进入 LookGraph 模式
  - `exit_look_graph/SKILL.md` - 退出 LookGraph 模式
- ✅ 创建 `~/.claude/CLAUDE.md` 文档（使用模板）
- ✅ 设置执行权限
- ✅ 生成 README 文档

**注意**: Skills 安装在项目级别（`<project>/.claude/skills/`），每个项目独立配置。

## 卸载

```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/init
python3 uninstall.py
```

**卸载内容**:
- 删除 `~/.claude/hooks/lookgraph/` 目录
- 删除项目级别的 skills：`<project>/.claude/skills/look_graph/` 和 `exit_look_graph/`
- 删除全局 skills（如果存在）：`~/.claude/skills/look_graph.skill.md` 和 `exit_look_graph.skill.md`
- 删除 `~/.claude/CLAUDE.md`（如果包含 LookGraph 内容）
- 清理 `settings.json` 中的旧配置

## 依赖

- Python 3.6+
- requests 库

```bash
pip install requests
```

## 使用

安装完成后，Claude 会自动识别 LookGraph 工具。

### 模式切换

#### 进入 LookGraph 模式
```
/look_graph
```

进入后，Claude 会优先使用 LookGraph API 来分析代码：
- ✅ 使用 `semantic_search.py` 代替 `Grep` 搜索代码
- ✅ 使用 `class_context.py` 代替 `Read` 理解类
- ✅ 使用 `method_context.py` 获取方法详情和依赖
- ✅ 使用 `impact_analysis.py` 评估修改影响
- ✅ 自动创建语义注释

**适用场景**：
- 分析陌生代码库
- 理解复杂业务逻辑
- 追踪依赖和调用链
- 评估变更影响
- 按业务意图搜索代码

#### 退出 LookGraph 模式
```
/exit_look_graph
```

恢复使用标准工具（`Grep`、`Glob`、`Read`）。

**适用场景**：
- 编辑配置文件
- 阅读文档
- 搜索非代码模式
- 标准文件操作

### 直接使用（无需模式切换）

你也可以直接问 Claude：
```
"使用 lookgraph 列出所有项目"
"用 lookgraph 初始化当前项目"
"使用 lookgraph 搜索代码"
```

Claude 会自动使用正确的命令。

## 服务要求

确保 LookGraph 服务运行在 http://localhost:8090

启动服务：
```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph
bash run.sh &
```

## 更新 CLAUDE.md 模板

如果需要修改 Claude 看到的文档，编辑 `CLAUDE_MD_TEMPLATE.md`，然后重新运行 `install.py`。

## 故障排查

### 安装失败

- 检查 Python 版本：`python3 --version`
- 检查 requests 库：`pip3 list | grep requests`
- 检查权限：确保有写入 `~/.claude/` 的权限

### Claude 无法识别工具

- 确认 `~/.claude/CLAUDE.md` 存在
- 检查文件内容：`cat ~/.claude/CLAUDE.md | head -20`
- 重启 Claude Code

### Hook 脚本报错

- 检查服务状态：`curl http://localhost:8090/api/project/list`
- 检查环境变量：确保没有设置错误的 `LOOKGRAPH_BASE_URL`

## 相关文档

- `../hook/` - Hook 脚本源文件
- `~/.claude/hooks/lookgraph/README.md` - 安装后的使用文档
- `~/.claude/CLAUDE.md` - Claude 自动加载的文档

## 项目结构

```
lookGraph/
├── init/                          # 安装脚本（本目录）
│   ├── install.py
│   ├── uninstall.py
│   └── CLAUDE_MD_TEMPLATE.md
└── hook/                          # Hook 脚本源文件
    ├── lookgraph_client.py
    ├── project_init.py
    ├── project_list.py
    └── ...
```

## 开发者说明

### 添加新的 Hook

1. 在 `../hook/` 目录创建新的 Python 脚本
2. 在 `install.py` 的 `HOOKS` 列表中添加配置
3. 更新 `CLAUDE_MD_TEMPLATE.md` 添加使用说明
4. 运行 `install.py` 测试

### 更新文档

编辑 `CLAUDE_MD_TEMPLATE.md`，然后运行：
```bash
python3 install.py
```

会自动更新 `~/.claude/CLAUDE.md`。

---

**维护者**: LookGraph Team  
**最后更新**: 2026-06-24
