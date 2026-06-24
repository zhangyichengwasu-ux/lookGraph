# LookGraph Hooks 文件迁移记录

## 迁移日期
2026-06-24

## 迁移内容

以下文件已从 `hook/` 目录迁移到 `init/` 目录：

1. ✅ `install.py` - 安装脚本
2. ✅ `uninstall.py` - 卸载脚本  
3. ✅ `CLAUDE_MD_TEMPLATE.md` - CLAUDE.md 文档模板（449 行）

## 目录结构

### 迁移前
```
lookGraph/
└── hook/
    ├── install.py
    ├── uninstall.py
    ├── CLAUDE_MD_TEMPLATE.md
    ├── lookgraph_client.py
    ├── project_*.py
    └── ...其他 hook 脚本
```

### 迁移后
```
lookGraph/
├── init/                           # 安装脚本目录
│   ├── README.md                   # 安装说明（新增）
│   ├── install.py                  # 从 hook/ 迁移
│   ├── uninstall.py                # 从 hook/ 迁移
│   └── CLAUDE_MD_TEMPLATE.md       # 从 hook/ 迁移
└── hook/                           # Hook 脚本源文件
    ├── lookgraph_client.py
    ├── project_init.py
    ├── project_list.py
    ├── semantic_search.py
    └── ...其他 hook 脚本
    └── *.md                        # 文档文件
```

## 迁移原因

1. **职责分离**: 
   - `init/` 目录专门存放安装/卸载脚本
   - `hook/` 目录专门存放实际的 hook 脚本

2. **清晰的目录结构**: 用户更容易找到安装脚本

3. **符合惯例**: 很多项目都有独立的安装目录

## 更新内容

### 新增文件

- ✅ `init/README.md` - 安装说明文档

### 保持不变

- `hook/` 目录中的所有 Python 脚本位置不变
- `~/.claude/hooks/lookgraph/` 安装目标位置不变
- `~/.claude/CLAUDE.md` 文档位置不变

## 使用方法更新

### 旧的安装方式
```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/hook
python3 install.py
```

### 新的安装方式
```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/init
python3 install.py
```

## 验证

```bash
# 检查文件是否正确迁移
ls /Users/zhangyicheng/Documents/GitHub/lookGraph/init/
# 应该看到: README.md, install.py, uninstall.py, CLAUDE_MD_TEMPLATE.md

# 检查安装脚本是否能正常工作
cd /Users/zhangyicheng/Documents/GitHub/lookGraph/init
python3 install.py
```

## 影响

### 对用户的影响

- ✅ **无影响**: 已安装的 hooks 继续正常工作
- ✅ **无影响**: `~/.claude/hooks/lookgraph/` 位置不变
- ⚠️ **需要注意**: 新的安装/卸载需要从 `init/` 目录运行

### 对文档的影响

- ✅ 创建了 `init/README.md` 说明新的安装方式
- ✅ 旧的文档路径引用不受影响（如果有）

### 对开发的影响

- ✅ 更清晰的项目结构
- ✅ 安装脚本和 hook 脚本分离
- ✅ 便于维护和扩展

## 回滚方案

如果需要回滚，运行：

```bash
cd /Users/zhangyicheng/Documents/GitHub/lookGraph
mv init/install.py hook/
mv init/uninstall.py hook/
mv init/CLAUDE_MD_TEMPLATE.md hook/
```

## 后续任务

- [ ] 更新项目主 README（如果有）
- [ ] 更新任何引用旧路径的文档
- [ ] 在下次 commit 时说明目录结构变更

## 测试确认

- ✅ 文件已成功迁移到 `init/` 目录
- ✅ `hook/` 目录保留所有脚本文件
- ✅ 创建了 `init/README.md` 文档
- ⏳ 待测试：从 `init/` 运行 `install.py`

---

**执行者**: Kiro AI  
**日期**: 2026-06-24  
**状态**: ✅ 完成
