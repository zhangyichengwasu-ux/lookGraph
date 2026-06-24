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
- ✅ 创建 `~/.claude/CLAUDE.md` 文档（使用模板）
- ✅ 设置执行权限
- ✅ 生成 README 文档

## 卸载

```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/init
python3 uninstall.py
```

**卸载内容**:
- 删除 `~/.claude/hooks/lookgraph/` 目录
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

你可以直接问 Claude：
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
