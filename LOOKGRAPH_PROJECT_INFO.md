# LookGraph 项目信息

## 项目 ID
63e1fcc74c5845e390e1556269de570c

## 项目名称
lookGraph

## 项目路径
/Users/zhangyicheng/Documents/GitHub/lookGraph

## 初始化统计
- **类数量**: 51
- **方法数量**: 130
- **模块数量**: 1
- **初始化时间**: 2026-06-24 11:16

## 常用命令

### 查询项目摘要
```bash
python3 ~/.claude/hooks/lookgraph/project_summary.py 63e1fcc74c5845e390e1556269de570c
```

### 语义搜索
```bash
python3 ~/.claude/hooks/lookgraph/semantic_search.py 63e1fcc74c5845e390e1556269de570c "查询关键词" 5
```

### 增量更新
```bash
python3 ~/.claude/hooks/lookgraph/project_update.py 63e1fcc74c5845e390e1556269de570c
```

## 在 Claude Code 中使用

现在 Claude 可以直接识别和使用 LookGraph 工具了！

你可以这样问 Claude：
- "使用 lookgraph 搜索项目中的控制器代码"
- "分析 ProjectController 类的影响范围"
- "获取 init 方法的调用链"

Claude 会自动使用正确的命令。
